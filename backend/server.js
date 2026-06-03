const http = require("http");
const fs = require("fs");
const path = require("path");

loadEnvFile(path.join(__dirname, ".env"));

const PORT = Number(process.env.PORT || 8787);
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-2.5-flash";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;
const YOLO_DETECT_URL = process.env.YOLO_DETECT_URL || "";
const YOLO_TIMEOUT_MS = Number(process.env.YOLO_TIMEOUT_MS || 20000);

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "GET" && req.url === "/health") {
      const yoloHealth = await checkYoloHealth();
      sendJson(res, 200, {
        ok: true,
        model: GEMINI_MODEL,
        geminiConfigured: Boolean(GEMINI_API_KEY),
        yoloConfigured: Boolean(YOLO_DETECT_URL),
        yoloReady: yoloHealth.ready,
        yoloError: yoloHealth.error,
        yoloDetectUrl: YOLO_DETECT_URL ? maskServiceUrl(YOLO_DETECT_URL) : ""
      });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/recipe-suggestions") {
      const body = await readJson(req);
      const text = await callGemini(buildRecipePrompt(body));
      sendJson(res, 200, { text, provider: "gemini", model: GEMINI_MODEL });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/ingredient-detection") {
      const body = await readJson(req);
      if (!YOLO_DETECT_URL) {
        sendJson(res, 503, {
          error: "YOLO detector chưa được cấu hình. Hãy đặt YOLO_DETECT_URL=https://.../detect trong backend/.env."
        });
        return;
      }
      const payload = await callYoloDetector(body);
      sendJson(res, 200, {
        ingredients: normalizeIngredientRows(payload.ingredients),
        provider: "yolo",
        model: payload.model || "yolo26s-ingredients-v1"
      });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/voice") {
      const body = await readJson(req);
      const text = await callGemini(buildVoicePrompt(body));
      sendJson(res, 200, { text, provider: "gemini", model: GEMINI_MODEL });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/voice-command-match") {
      const body = await readJson(req);
      const text = await callGemini(buildVoiceCommandMatchPrompt(body));
      sendJson(res, 200, { text: normalizeMatchedCommand(text, body.allowedCommands), provider: "gemini", model: GEMINI_MODEL });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/voice-audio-command-match") {
      const body = await readJson(req);
      const payload = await callGeminiJson(buildVoiceAudioCommandMatchParts(body));
      sendJson(res, 200, {
        text: normalizeMatchedCommand(payload.command, body.allowedCommands),
        transcript: String(payload.transcript || "").trim(),
        provider: "gemini",
        model: GEMINI_MODEL
      });
      return;
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (error) {
    sendJson(res, 500, { error: error.message || "Backend AI error" });
  }
});

server.on("error", error => {
  if (error.code === "EADDRINUSE") {
    console.error(`Khong the chay AI backend: port ${PORT} dang duoc su dung.`);
    console.error("Hay dung terminal backend dang mo, dong tien trinh cu, hoac doi PORT trong backend/.env.");
    process.exit(1);
  }
  throw error;
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`AI backend listening on http://localhost:${PORT}`);
});

function loadEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }
  const rows = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  for (const row of rows) {
    const trimmed = row.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }
    const equalsIndex = trimmed.indexOf("=");
    if (equalsIndex <= 0) {
      continue;
    }
    const key = trimmed.slice(0, equalsIndex).trim();
    const value = trimmed.slice(equalsIndex + 1).trim();
    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let data = "";
    req.on("data", chunk => {
      data += chunk;
      if (data.length > 8 * 1024 * 1024) {
        req.destroy();
        reject(new Error("Request body is too large"));
      }
    });
    req.on("end", () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch (error) {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

async function callGemini(prompt) {
  const payload = await callGeminiPayload({
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.55,
      maxOutputTokens: 512,
      thinkingConfig: {
        thinkingBudget: 0
      }
    }
  });
  const text = payload.candidates?.[0]?.content?.parts
    ?.map(part => part.text || "")
    .join("")
    .trim();
  if (!text) {
    throw new Error("Gemini returned empty text");
  }
  return text;
}

async function callGeminiJson(parts) {
  const payload = await callGeminiPayload({
    contents: [{ role: "user", parts }],
    generationConfig: {
      temperature: 0.1,
      maxOutputTokens: 256,
      responseMimeType: "application/json",
      thinkingConfig: {
        thinkingBudget: 0
      }
    }
  });
  const text = payload.candidates?.[0]?.content?.parts
    ?.map(part => part.text || "")
    .join("")
    .trim();
  if (!text) {
    throw new Error("Gemini returned empty JSON text");
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(`Gemini JSON parse failed: ${text}`);
  }
}

async function callGeminiPayload(payload) {
  if (!GEMINI_API_KEY) {
    throw new Error("GEMINI_API_KEY is not configured on backend");
  }
  const response = await fetch(GEMINI_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": GEMINI_API_KEY
    },
    body: JSON.stringify(payload)
  });

  const payloadText = await response.text();
  if (!response.ok) {
    throw new Error(`Gemini ${response.status}: ${payloadText}`);
  }
  return JSON.parse(payloadText);
}

async function callYoloDetector(body) {
  const imageBase64 = String(body.imageBase64 || "");
  const mimeType = String(body.mimeType || "image/jpeg");
  if (!imageBase64) {
    throw new Error("imageBase64 is required");
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), YOLO_TIMEOUT_MS);
  try {
    const response = await fetch(YOLO_DETECT_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      signal: controller.signal,
      body: JSON.stringify({ imageBase64, mimeType })
    });
    const payloadText = await response.text();
    if (!response.ok) {
      throw new Error(`YOLO detector ${response.status}: ${payloadText}`);
    }
    try {
      return JSON.parse(payloadText);
    } catch (error) {
      throw new Error(`YOLO detector JSON parse failed: ${payloadText}`);
    }
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error(`YOLO detector timeout sau ${YOLO_TIMEOUT_MS}ms`);
    }
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

async function checkYoloHealth() {
  if (!YOLO_DETECT_URL) {
    return { ready: false, error: "YOLO_DETECT_URL is not configured" };
  }
  let healthUrl;
  try {
    const detectUrl = new URL(YOLO_DETECT_URL);
    detectUrl.pathname = detectUrl.pathname.replace(/\/detect\/?$/, "/health");
    healthUrl = detectUrl.toString();
  } catch (error) {
    return { ready: false, error: "YOLO_DETECT_URL is invalid" };
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 2000);
  try {
    const response = await fetch(healthUrl, { signal: controller.signal });
    if (!response.ok) {
      return { ready: false, error: `YOLO health ${response.status}` };
    }
    return { ready: true, error: "" };
  } catch (error) {
    return {
      ready: false,
      error: error.name === "AbortError" ? "YOLO health timeout" : error.message
    };
  } finally {
    clearTimeout(timeout);
  }
}

function buildRecipePrompt(body) {
  const ingredients = Array.isArray(body.selectedIngredients) ? body.selectedIngredients : [];
  const pantry = Array.isArray(body.pantryItems) ? body.pantryItems : [];
  const matches = Array.isArray(body.matches) ? body.matches : [];
  const topMatches = matches.slice(0, 3).map(match => {
    const missing = Array.isArray(match.missingIngredients) && match.missingIngredients.length
      ? match.missingIngredients.join(", ")
      : "không thiếu";
    return `${match.recipeName || "Món chưa rõ"} ${match.scorePercent || 0}%, thiếu ${missing}`;
  }).join("; ");

  return [
    "Bạn là AI Chef của app Sổ Tay Nấu Ăn AI.",
    "Trả lời tiếng Việt, 2-3 câu ngắn, thực tế cho gia đình.",
    "Không tự bịa số lượng. Chỉ dựa trên pantryItems và matches app gửi.",
    `Nguyên liệu người dùng xác nhận: ${ingredients.length ? ingredients.join(", ") : "chưa rõ"}.`,
    `Tồn bếp local: ${pantry.length ? JSON.stringify(pantry) : "chưa có dữ liệu tồn bếp"}.`,
    `Các món app đã so khớp local: ${topMatches || "chưa có món phù hợp"}.`,
    "Hãy nêu món nên nấu trước, lý do ngắn, phần cần mua nếu matches có nêu, và một mẹo nhỏ."
  ].join(" ");
}

function buildIngredientDetectionParts(body) {
  const imageBase64 = String(body.imageBase64 || "");
  const mimeType = String(body.mimeType || "image/jpeg");
  if (!imageBase64) {
    throw new Error("imageBase64 is required");
  }
  return [
    {
      text: [
        "Bạn là bộ nhận diện nguyên liệu nấu ăn từ ảnh camera.",
        "Trả JSON hợp lệ dạng {\"ingredients\":[\"...\"]}.",
        "Chỉ liệt kê tên nguyên liệu nhìn thấy rõ, tiếng Việt, không ghi số lượng.",
        "Ưu tiên tên ngắn dùng được trong app: trứng, cà chua, hành lá, tỏi, thịt gà, thịt heo, thịt bò, tôm, cá, rau muống, bún, cơm, nước mắm.",
        "Không thêm giải thích, không markdown, không bịa nguyên liệu không thấy."
      ].join(" ")
    },
    {
      inlineData: {
        mimeType,
        data: imageBase64
      }
    }
  ];
}

function normalizeIngredientRows(rows) {
  const source = Array.isArray(rows) ? rows : [];
  const seen = new Set();
  const ingredients = [];
  for (const row of source) {
    const normalized = normalizeIngredientRow(row);
    if (!normalized.name) {
      continue;
    }
    const key = normalized.name.toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      ingredients.push(normalized);
    }
  }
  return ingredients.slice(0, 8);
}

function normalizeIngredientRow(row) {
  if (typeof row === "string") {
    const name = cleanText(row);
    return {
      name,
      quantity: "",
      count: 0,
      confidence: 0,
      boxes: []
    };
  }
  const source = row && typeof row === "object" ? row : {};
  const name = cleanText(source.name || source.label || source.className || "");
  const count = Math.max(0, Number.isFinite(Number(source.count)) ? Math.round(Number(source.count)) : 0);
  const boxes = Array.isArray(source.boxes)
    ? source.boxes.map(normalizeBox).filter(Boolean)
    : [];
  const confidence = clamp01(Number.isFinite(Number(source.confidence))
    ? Number(source.confidence)
    : averageConfidence(boxes));
  const quantity = cleanText(source.quantity || quantityFromCount(name, count));
  return {
    name,
    quantity,
    count,
    confidence,
    boxes
  };
}

function normalizeBox(box) {
  if (!box || typeof box !== "object") {
    return null;
  }
  return {
    x1: finiteNumber(box.x1),
    y1: finiteNumber(box.y1),
    x2: finiteNumber(box.x2),
    y2: finiteNumber(box.y2),
    confidence: clamp01(finiteNumber(box.confidence))
  };
}

function quantityFromCount(name, count) {
  if (!name || count <= 0) {
    return "";
  }
  const unit = unitForIngredient(name);
  return unit ? `${count} ${unit}` : "";
}

function unitForIngredient(name) {
  const normalized = name.toLowerCase();
  if (/(trứng|cà chua|chanh|ớt|dưa leo)/.test(normalized)) {
    return "quả";
  }
  if (/(tỏi|hành tím|hành tây|cà rốt|khoai tây|gừng|sả)/.test(normalized)) {
    return "củ";
  }
  if (/(hành lá|rau muống|cải xanh|bắp cải)/.test(normalized)) {
    return "bó";
  }
  if (/(thịt|cá|tôm|mực|đậu hũ|bún|mì|gạo|cơm|nấm|bí đỏ)/.test(normalized)) {
    return count === 1 ? "phần" : "phần";
  }
  return "";
}

function cleanText(value) {
  return String(value || "").trim().replace(/\s+/g, " ");
}

function finiteNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

function clamp01(value) {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Math.max(0, Math.min(1, value));
}

function averageConfidence(boxes) {
  if (!boxes.length) {
    return 0;
  }
  const total = boxes.reduce((sum, box) => sum + finiteNumber(box.confidence), 0);
  return total / boxes.length;
}

function maskServiceUrl(value) {
  try {
    const url = new URL(value);
    return `${url.protocol}//${url.host}${url.pathname}`;
  } catch (error) {
    return "configured";
  }
}

function buildVoicePrompt(body) {
  return [
    "Bạn là trợ lý giọng nói trong lúc người dùng đang nấu.",
    "Trả lời tiếng Việt, một câu ngắn, an toàn, không tự chuyển bước nấu.",
    `Ngữ cảnh: ${body.contextLabel || "chưa có phiên nấu"}.`,
    `Lệnh người dùng: ${body.command || "không rõ"}.`,
    `Kết quả local trong app: ${body.localResponse || "không có"}.`,
    "Hãy diễn đạt lại thành câu trợ lý tự nhiên và hữu ích."
  ].join(" ");
}

function buildVoiceCommandMatchPrompt(body) {
  const allowedCommands = Array.isArray(body.allowedCommands) ? body.allowedCommands : [];
  return [
    "Bạn là bộ phân loại lệnh giọng nói cho app nấu ăn.",
    "Chỉ được trả về đúng một lệnh trong danh sách cho phép, không giải thích, không thêm dấu câu.",
    "Nếu câu nói không rõ, hãy chọn lệnh gần nghĩa nhất.",
    `Ngữ cảnh: ${body.contextLabel || "chưa có phiên nấu"}.`,
    `Câu người dùng vừa nói: ${body.spokenText || "không rõ"}.`,
    `Danh sách lệnh cho phép: ${allowedCommands.join(" | ")}.`
  ].join(" ");
}

function buildVoiceAudioCommandMatchParts(body) {
  const allowedCommands = Array.isArray(body.allowedCommands) ? body.allowedCommands : [];
  const audioBase64 = String(body.audioBase64 || "");
  const mimeType = String(body.mimeType || "audio/mp4");
  if (!audioBase64) {
    throw new Error("audioBase64 is required");
  }
  return [
    {
      text: [
        "Bạn là bộ nhận diện lệnh giọng nói tiếng Việt cho app nấu ăn.",
        "Hãy nghe file audio, chép lại câu nói tiếng Việt, rồi chọn đúng một lệnh gần nghĩa nhất trong danh sách.",
        "Chỉ trả JSON hợp lệ dạng {\"transcript\":\"...\",\"command\":\"...\"}.",
        "command bắt buộc phải là một mục nguyên văn trong danh sách lệnh cho phép.",
        "Nếu audio không rõ, transcript để mô tả ngắn và command chọn lệnh gần nhất.",
        `Ngữ cảnh: ${body.contextLabel || "chưa có phiên nấu"}.`,
        `Danh sách lệnh cho phép: ${allowedCommands.join(" | ")}.`
      ].join(" ")
    },
    {
      inlineData: {
        mimeType,
        data: audioBase64
      }
    }
  ];
}

function normalizeMatchedCommand(text, allowedCommands) {
  const commands = Array.isArray(allowedCommands) ? allowedCommands.filter(Boolean) : [];
  const cleaned = String(text || "").trim().replace(/^["'`]+|["'`]+$/g, "");
  const exact = commands.find(command => command.toLowerCase() === cleaned.toLowerCase());
  if (exact) {
    return exact;
  }
  const contained = commands.find(command =>
    cleaned.toLowerCase().includes(command.toLowerCase()) ||
    command.toLowerCase().includes(cleaned.toLowerCase())
  );
  return contained || commands[0] || cleaned;
}

function matchAllowedCommandLocally(text, allowedCommands) {
  const commands = Array.isArray(allowedCommands) ? allowedCommands.filter(Boolean) : [];
  const normalized = String(text || "").toLowerCase();
  if ((normalized.includes("thêm") || normalized.includes("cộng")) && normalized.includes("phút")) {
    return commands.find(command => command.toLowerCase().includes("thêm")) || commands[0] || "";
  }
  if (normalized.includes("bao lâu") || normalized.includes("timer") || normalized.includes("hẹn giờ")) {
    return commands.find(command => command.toLowerCase().includes("timer")) || commands[0] || "";
  }
  if (normalized.includes("đọc") || normalized.includes("nhắc") || normalized.includes("lặp")) {
    return commands.find(command => command.toLowerCase().includes("đọc")) || commands[0] || "";
  }
  if (normalized.includes("bước")) {
    return commands.find(command => command.toLowerCase().includes("bước")) || commands[0] || "";
  }
  return commands[0] || "";
}

function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*"
  });
  res.end(JSON.stringify(data));
}
