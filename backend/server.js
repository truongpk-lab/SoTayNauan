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
const IMAGE_SEARCH_TIMEOUT_MS = Number(process.env.IMAGE_SEARCH_TIMEOUT_MS || 7000);
const GOOGLE_CSE_API_KEY = process.env.GOOGLE_CSE_API_KEY || "";
const GOOGLE_CSE_ID = process.env.GOOGLE_CSE_ID || "";
const PIXABAY_API_KEY = process.env.PIXABAY_API_KEY || "";
const PEXELS_API_KEY = process.env.PEXELS_API_KEY || "";
const UNSPLASH_ACCESS_KEY = process.env.UNSPLASH_ACCESS_KEY || "";
const RECIPE_IMAGE_MIN_SCORE = Number(process.env.RECIPE_IMAGE_MIN_SCORE || 58);
const RECIPE_IMAGE_SOURCE_SITES = (process.env.RECIPE_IMAGE_SOURCE_SITES || [
  "cooky.vn",
  "dienmayxanh.com",
  "bachhoaxanh.com",
  "ngonaz.com",
  "thatlangon.com",
  "afamily.vn"
].join(",")).split(",").map(value => value.trim()).filter(Boolean);

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
        yoloDetectUrl: YOLO_DETECT_URL ? maskServiceUrl(YOLO_DETECT_URL) : "",
        recipeImageSearch: {
          googleCseConfigured: Boolean(GOOGLE_CSE_API_KEY && GOOGLE_CSE_ID),
          pixabayConfigured: Boolean(PIXABAY_API_KEY),
          pexelsConfigured: Boolean(PEXELS_API_KEY),
          unsplashConfigured: Boolean(UNSPLASH_ACCESS_KEY),
          sourceSites: RECIPE_IMAGE_SOURCE_SITES
        }
      });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/recipe-suggestions") {
      const body = await readJson(req);
      const text = await callGemini(buildRecipePrompt(body));
      sendJson(res, 200, { text, provider: "gemini", model: GEMINI_MODEL });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/related-recipes") {
      const body = await readJson(req);
      const payload = await callGeminiJsonPrompt(buildRelatedRecipesPrompt(body), 512, 0.35, false);
      sendJson(res, 200, {
        suggestions: normalizeRecipeSuggestions(payload.suggestions),
        provider: "gemini",
        model: GEMINI_MODEL
      });
      return;
    }

    if (req.method === "POST" && req.url === "/api/ai/import-recipe") {
      const body = await readJson(req);
      const payload = await callGeminiJsonPrompt(buildImportRecipePrompt(body), 1536, 0.2, true);
      const recipe = normalizeGeneratedRecipe(payload.recipe || payload, body.recipeName);
      if (!recipe.imageUrl) {
        recipe.imageUrl = await findRecipeImageUrl(recipe, payload.recipe || payload);
      }
      sendJson(res, 200, {
        recipe,
        provider: "gemini",
        model: GEMINI_MODEL
      });
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
    sendJson(res, error.statusCode || 500, {
      error: error.code || "BACKEND_ERROR",
      message: error.userMessage || error.message || "Backend AI error",
      retryAfterSeconds: error.retryAfterSeconds || 0
    });
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
    throw createBackendError(503, "GEMINI_NOT_CONFIGURED",
      "Backend chưa cấu hình GEMINI_API_KEY.");
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
    throw createGeminiError(response.status, payloadText);
  }
  return JSON.parse(payloadText);
}

function createGeminiError(statusCode, payloadText) {
  let payload = null;
  try {
    payload = JSON.parse(payloadText);
  } catch (error) {
    payload = null;
  }
  const message = payload?.error?.message || payloadText || "Gemini API error";
  const status = payload?.error?.status || "";
  const retryAfterSeconds = extractRetryAfterSeconds(payload);
  if (statusCode === 429 || status === "RESOURCE_EXHAUSTED") {
    return createBackendError(429, "GEMINI_QUOTA_EXCEEDED",
      "Gemini đã hết hạn mức tạm thời. Hãy đợi"
      + (retryAfterSeconds > 0 ? " khoảng " + retryAfterSeconds + " giây" : "")
      + " rồi thử lại, hoặc đổi API key/gói quota trong backend.",
      retryAfterSeconds);
  }
  return createBackendError(statusCode, status || "GEMINI_ERROR",
    "Gemini đang lỗi: " + cleanText(message).slice(0, 220),
    retryAfterSeconds);
}

function extractRetryAfterSeconds(payload) {
  const details = Array.isArray(payload?.error?.details) ? payload.error.details : [];
  for (const detail of details) {
    const retryDelay = String(detail.retryDelay || "");
    const match = retryDelay.match(/(\d+(?:\.\d+)?)s/);
    if (match) {
      return Math.max(1, Math.ceil(Number(match[1])));
    }
  }
  return 0;
}

function createBackendError(statusCode, code, userMessage, retryAfterSeconds) {
  const error = new Error(userMessage);
  error.statusCode = statusCode;
  error.code = code;
  error.userMessage = userMessage;
  error.retryAfterSeconds = retryAfterSeconds || 0;
  return error;
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

async function callGeminiJsonPrompt(prompt, maxOutputTokens, temperature, useSearch) {
  const request = {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature,
      maxOutputTokens,
      responseMimeType: "application/json",
      thinkingConfig: {
        thinkingBudget: 0
      }
    }
  };
  if (useSearch) {
    request.tools = [{ googleSearch: {} }];
  }
  try {
    const payload = await callGeminiPayload(request);
    return parseGeminiJsonPayload(payload);
  } catch (error) {
    if (!useSearch) {
      throw error;
    }
    try {
      const searchTextRequest = {
        ...request,
        generationConfig: {
          ...request.generationConfig
        }
      };
      delete searchTextRequest.generationConfig.responseMimeType;
      const payload = await callGeminiPayload(searchTextRequest);
      return parseGeminiJsonPayload(payload);
    } catch (searchError) {
      // Continue to a no-search JSON request so the app can still finish the flow.
    }
    const fallbackRequest = { ...request };
    delete fallbackRequest.tools;
    const payload = await callGeminiPayload(fallbackRequest);
    return parseGeminiJsonPayload(payload);
  }
}

function parseGeminiJsonPayload(payload) {
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
    const extracted = extractJsonObject(text);
    if (extracted) {
      return JSON.parse(extracted);
    }
    throw new Error(`Gemini JSON parse failed: ${text}`);
  }
}

function extractJsonObject(text) {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) {
    return "";
  }
  return text.slice(start, end + 1);
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

function buildRelatedRecipesPrompt(body) {
  const dishName = String(body.dishName || "").trim();
  const existingRecipeNames = Array.isArray(body.existingRecipeNames)
    ? body.existingRecipeNames.filter(Boolean).slice(0, 80)
    : [];
  if (!dishName) {
    throw new Error("dishName is required");
  }
  return [
    "Bạn là AI Chef của app Sổ Tay Nấu Ăn AI.",
    "Người dùng nhập tên món hoặc ý tưởng món ăn. Hãy tìm 5 món ăn liên quan nhất để người dùng chọn.",
    "Ưu tiên món Việt Nam hoặc món phổ biến với gia đình Việt. Không trả món trùng chính xác với danh sách đã có nếu có lựa chọn gần hơn.",
    "Trả JSON hợp lệ, không markdown, dạng {\"suggestions\":[{\"name\":\"...\",\"reason\":\"...\"}]} với đúng 5 phần tử.",
    "Mỗi name là tên món ngắn, rõ ràng. reason tối đa 12 từ.",
    `Tên người dùng nhập: ${dishName}.`,
    `Tên món đã có trong app: ${existingRecipeNames.join(", ") || "chưa gửi"}.`
  ].join(" ");
}

function buildImportRecipePrompt(body) {
  const recipeName = String(body.recipeName || "").trim();
  if (!recipeName) {
    throw new Error("recipeName is required");
  }
  return [
    "Bạn là AI Chef của app Sổ Tay Nấu Ăn AI.",
    "Hãy dùng Google Search grounding nếu khả dụng để tham khảo các trang hướng dẫn nấu ăn đáng tin cậy, rồi tổng hợp một công thức thực hành được.",
    "Không sao chép nguyên văn nội dung từ một trang. Chỉ tổng hợp công thức ngắn gọn, an toàn, dễ nấu tại nhà.",
    "Trả JSON hợp lệ, không markdown, dạng {\"recipe\":{...}}.",
    "recipe bắt buộc có đủ các trường: name, description, totalMinutes, difficulty, category, serving, calories, cost, ingredients, steps, imageSearchQuery, sourceRecipeUrls.",
    "difficulty chỉ dùng một trong: Dễ, Trung bình, Khó.",
    "category nên là một nhóm món thông dụng như Canh, Món kho, Món cơm, Món nước, Bún/Phở, Lẩu, Món chiên, Món xào, Món nướng, Món hấp, Món luộc, Gỏi & Salad, Món bánh, Tráng miệng, Nước uống, Món nhanh.",
    "ingredients là mảng 8-16 dòng theo mẫu 'Tên nguyên liệu: số lượng đơn vị', ví dụ 'Thịt gà: 500 g'. Với gia vị dùng tiền tố 'Gia vị - ', ví dụ 'Gia vị - Nước mắm: 2 muỗng canh'.",
    "steps là mảng 5-10 bước nấu rõ ràng, mỗi bước một câu.",
    "calories dạng '520 kcal/phần' nếu ước lượng được; cost dạng '90.000đ' theo giá phổ thông Việt Nam; nếu không chắc vẫn đưa ước lượng hợp lý.",
    "imageSearchQuery là cụm từ ngắn để tìm ảnh thật của món trên web, gồm tên tiếng Việt và nếu biết thì tên tiếng Anh, ví dụ 'khổ qua nhồi thịt Vietnamese stuffed bitter melon soup'.",
    "sourceRecipeUrls là mảng URL trang công thức bạn đã tham khảo nếu có; ưu tiên trang có ảnh món thật và structured data Recipe.",
    `Món cần tìm công thức: ${recipeName}.`
  ].join(" ");
}

function normalizeRecipeSuggestions(rows) {
  const source = Array.isArray(rows) ? rows : [];
  const seen = new Set();
  const suggestions = [];
  for (const row of source) {
    const name = cleanText(row && row.name);
    if (!name) {
      continue;
    }
    const key = name.toLowerCase();
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    suggestions.push({
      name,
      reason: cleanText(row && row.reason)
    });
    if (suggestions.length >= 5) {
      break;
    }
  }
  return suggestions;
}

function normalizeGeneratedRecipe(rawRecipe, fallbackName) {
  const recipe = rawRecipe && typeof rawRecipe === "object" ? rawRecipe : {};
  const ingredients = cleanStringArray(recipe.ingredients).slice(0, 18);
  const steps = cleanStringArray(recipe.steps).slice(0, 12);
  if (!ingredients.length || !steps.length) {
    throw new Error("Gemini returned an incomplete recipe");
  }
  return {
    name: cleanText(recipe.name) || cleanText(fallbackName),
    description: cleanText(recipe.description) || "Công thức được AI tổng hợp từ các nguồn hướng dẫn nấu ăn.",
    totalMinutes: clampInt(recipe.totalMinutes, 5, 240, 30),
    difficulty: normalizeDifficulty(recipe.difficulty),
    category: normalizeRecipeCategory(recipe, fallbackName),
    serving: cleanText(recipe.serving) || "2 người",
    calories: cleanText(recipe.calories) || "Ước lượng",
    cost: cleanText(recipe.cost) || "Ước lượng",
    imageUrl: isLikelyImageUrl(recipe.imageUrl) ? cleanText(recipe.imageUrl) : "",
    imageSearchQuery: cleanText(recipe.imageSearchQuery),
    sourceRecipeUrls: normalizeUrlList(recipe.sourceRecipeUrls || recipe.sourceUrls || recipe.sources),
    imageSourceType: "",
    imageSourceUrl: "",
    imageAttribution: "",
    imageConfidence: 0,
    ingredients,
    steps
  };
}

async function findRecipeImageUrl(recipe, rawRecipe) {
  const selected = await findRecipeImageCandidate(recipe, rawRecipe);
  if (selected) {
    recipe.imageSourceType = selected.sourceType;
    recipe.imageSourceUrl = selected.sourceUrl;
    recipe.imageAttribution = selected.attribution || "";
    recipe.imageConfidence = selected.score;
    return selected.url;
  }
  return "";
}

async function findRecipeImageCandidate(recipe, rawRecipe) {
  const queries = buildImageQueries(recipe, rawRecipe);
  const candidates = [];

  const sourceUrls = collectSourceRecipeUrls(recipe, rawRecipe);
  for (const sourceUrl of sourceUrls) {
    candidates.push(...await findRecipePageImageCandidates(sourceUrl, recipe));
  }

  const googleRecipePages = await findRecipePagesViaGoogle(queries);
  for (const sourceUrl of googleRecipePages) {
    if (!sourceUrls.includes(sourceUrl)) {
      candidates.push(...await findRecipePageImageCandidates(sourceUrl, recipe));
    }
  }

  candidates.push(...await findGoogleImageCandidates(queries, recipe));
  candidates.push(...await findPixabayImageCandidates(queries, recipe));
  candidates.push(...await findPexelsImageCandidates(queries, recipe));
  candidates.push(...await findUnsplashImageCandidates(queries, recipe));
  candidates.push(...await findWikimediaImageCandidates(queries, recipe));

  const best = chooseBestImageCandidate(candidates, recipe);
  return best && best.score >= RECIPE_IMAGE_MIN_SCORE ? best : null;
}

function normalizeUrlList(values) {
  const source = Array.isArray(values) ? values : values ? [values] : [];
  const urls = [];
  const seen = new Set();
  for (const value of source) {
    const url = cleanText(value);
    if (!isHttpUrl(url) || seen.has(url)) {
      continue;
    }
    seen.add(url);
    urls.push(url);
  }
  return urls.slice(0, 8);
}

function collectSourceRecipeUrls(recipe, rawRecipe) {
  const urls = [
    ...normalizeUrlList(recipe.sourceRecipeUrls),
    ...normalizeUrlList(rawRecipe && rawRecipe.sourceRecipeUrls),
    ...normalizeUrlList(rawRecipe && rawRecipe.sourceUrls),
    ...normalizeUrlList(rawRecipe && rawRecipe.sources),
    ...normalizeUrlList(rawRecipe && rawRecipe.sourceRecipeUrl)
  ];
  const seen = new Set();
  return urls.filter(url => {
    if (seen.has(url)) {
      return false;
    }
    seen.add(url);
    return true;
  }).slice(0, 10);
}

async function findRecipePageImageCandidates(sourceUrl, recipe) {
  try {
    const html = await fetchTextWithTimeout(sourceUrl, IMAGE_SEARCH_TIMEOUT_MS);
    return extractImageCandidatesFromRecipePage(html, sourceUrl, recipe);
  } catch (error) {
    return [];
  }
}

function extractImageCandidatesFromRecipePage(html, sourceUrl, recipe) {
  const candidates = [];
  for (const jsonText of extractJsonLdBlocks(html)) {
    const payload = safeJsonParse(jsonText);
    for (const node of flattenJsonLd(payload)) {
      if (!isRecipeJsonLdNode(node)) {
        continue;
      }
      for (const imageUrl of extractImageUrlsFromValue(node.image, sourceUrl)) {
        candidates.push(imageCandidate(imageUrl, "recipe-jsonld", sourceUrl,
          cleanText(node.name || recipe.name), 92));
      }
    }
  }
  for (const imageUrl of extractMetaImageUrls(html, sourceUrl)) {
    candidates.push(imageCandidate(imageUrl, "recipe-meta", sourceUrl, "", 74));
  }
  return candidates;
}

function extractJsonLdBlocks(html) {
  const blocks = [];
  const regex = /<script[^>]+type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = regex.exec(html || "")) !== null) {
    const text = decodeHtmlEntities(match[1]).trim();
    if (text) {
      blocks.push(text);
    }
  }
  return blocks;
}

function safeJsonParse(text) {
  try {
    return JSON.parse(text);
  } catch (error) {
    return null;
  }
}

function flattenJsonLd(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return value.flatMap(flattenJsonLd);
  }
  if (typeof value !== "object") {
    return [];
  }
  const nodes = [value];
  if (Array.isArray(value["@graph"])) {
    nodes.push(...value["@graph"].flatMap(flattenJsonLd));
  }
  return nodes;
}

function isRecipeJsonLdNode(node) {
  const type = node && node["@type"];
  const types = Array.isArray(type) ? type : [type];
  return types.some(item => String(item || "").toLowerCase() === "recipe");
}

function extractImageUrlsFromValue(value, baseUrl) {
  const urls = [];
  if (!value) {
    return urls;
  }
  if (typeof value === "string") {
    urls.push(resolveUrl(value, baseUrl));
  } else if (Array.isArray(value)) {
    for (const item of value) {
      urls.push(...extractImageUrlsFromValue(item, baseUrl));
    }
  } else if (typeof value === "object") {
    urls.push(...extractImageUrlsFromValue(value.url || value.contentUrl || value["@id"], baseUrl));
  }
  return urls.filter(isLikelyImageUrl);
}

function extractMetaImageUrls(html, baseUrl) {
  const urls = [];
  const regex = /<meta\s+[^>]*(?:property|name)=["'](?:og:image|twitter:image|image)["'][^>]*>/gi;
  let match;
  while ((match = regex.exec(html || "")) !== null) {
    const contentMatch = match[0].match(/\scontent=["']([^"']+)["']/i);
    if (contentMatch) {
      const url = resolveUrl(decodeHtmlEntities(contentMatch[1]), baseUrl);
      if (isLikelyImageUrl(url)) {
        urls.push(url);
      }
    }
  }
  return urls;
}

async function findRecipePagesViaGoogle(queries) {
  if (!GOOGLE_CSE_API_KEY || !GOOGLE_CSE_ID) {
    return [];
  }
  const urls = [];
  const seen = new Set();
  for (const query of queries) {
    const siteQuery = RECIPE_IMAGE_SOURCE_SITES.length
      ? `(${RECIPE_IMAGE_SOURCE_SITES.map(site => `site:${site}`).join(" OR ")})`
      : "";
    const searchQuery = `${query} cách nấu công thức ${siteQuery}`.trim();
    const apiUrl = "https://www.googleapis.com/customsearch/v1?"
      + `key=${encodeURIComponent(GOOGLE_CSE_API_KEY)}`
      + `&cx=${encodeURIComponent(GOOGLE_CSE_ID)}`
      + `&num=5&safe=active&hl=vi&q=${encodeURIComponent(searchQuery)}`;
    try {
      const payload = await fetchJsonWithTimeout(apiUrl, IMAGE_SEARCH_TIMEOUT_MS);
      for (const item of payload.items || []) {
        const url = cleanText(item.link);
        if (isHttpUrl(url) && !seen.has(url)) {
          seen.add(url);
          urls.push(url);
        }
      }
    } catch (error) {
      continue;
    }
  }
  return urls.slice(0, 12);
}

async function findGoogleImageCandidates(queries, recipe) {
  if (!GOOGLE_CSE_API_KEY || !GOOGLE_CSE_ID) {
    return [];
  }
  const candidates = [];
  for (const query of queries.slice(0, 4)) {
    const apiUrl = "https://www.googleapis.com/customsearch/v1?"
      + `key=${encodeURIComponent(GOOGLE_CSE_API_KEY)}`
      + `&cx=${encodeURIComponent(GOOGLE_CSE_ID)}`
      + "&searchType=image&imgType=photo&imgSize=large&safe=active&num=8"
      + `&hl=vi&q=${encodeURIComponent(query + " món ăn")}`;
    try {
      const payload = await fetchJsonWithTimeout(apiUrl, IMAGE_SEARCH_TIMEOUT_MS);
      for (const item of payload.items || []) {
        candidates.push(imageCandidate(item.link, "google-cse-image",
          item.image?.contextLink || item.link, cleanText(item.title || recipe.name), 72));
      }
    } catch (error) {
      continue;
    }
  }
  return candidates;
}

async function findPixabayImageCandidates(queries, recipe) {
  if (!PIXABAY_API_KEY) {
    return [];
  }
  const candidates = [];
  for (const query of queries.slice(0, 3)) {
    const apiUrl = "https://pixabay.com/api/?"
      + `key=${encodeURIComponent(PIXABAY_API_KEY)}`
      + `&q=${encodeURIComponent(query)}&lang=vi&image_type=photo&category=food`
      + "&safesearch=true&orientation=horizontal&per_page=8";
    try {
      const payload = await fetchJsonWithTimeout(apiUrl, IMAGE_SEARCH_TIMEOUT_MS);
      for (const hit of payload.hits || []) {
        candidates.push(imageCandidate(hit.webformatURL || hit.largeImageURL,
          "pixabay", hit.pageURL, cleanText(hit.tags || recipe.name), 58,
          "Pixabay"));
      }
    } catch (error) {
      continue;
    }
  }
  return candidates;
}

async function findPexelsImageCandidates(queries, recipe) {
  if (!PEXELS_API_KEY) {
    return [];
  }
  const candidates = [];
  for (const query of queries.slice(0, 3)) {
    const apiUrl = "https://api.pexels.com/v1/search?"
      + `query=${encodeURIComponent(query)}&locale=vi-VN&orientation=landscape&per_page=8`;
    try {
      const payload = await fetchJsonWithHeaders(apiUrl, {
        "Authorization": PEXELS_API_KEY
      }, IMAGE_SEARCH_TIMEOUT_MS);
      for (const photo of payload.photos || []) {
        candidates.push(imageCandidate(photo.src?.large || photo.src?.medium,
          "pexels", photo.url, cleanText(photo.alt || recipe.name), 56,
          photo.photographer ? `Photo by ${photo.photographer} on Pexels` : "Pexels"));
      }
    } catch (error) {
      continue;
    }
  }
  return candidates;
}

async function findUnsplashImageCandidates(queries, recipe) {
  if (!UNSPLASH_ACCESS_KEY) {
    return [];
  }
  const candidates = [];
  for (const query of queries.slice(0, 3)) {
    const apiUrl = "https://api.unsplash.com/search/photos?"
      + `query=${encodeURIComponent(query + " food")}&orientation=landscape&per_page=8`;
    try {
      const payload = await fetchJsonWithHeaders(apiUrl, {
        "Authorization": `Client-ID ${UNSPLASH_ACCESS_KEY}`
      }, IMAGE_SEARCH_TIMEOUT_MS);
      for (const photo of payload.results || []) {
        candidates.push(imageCandidate(photo.urls?.regular || photo.urls?.small,
          "unsplash", photo.links?.html || "", cleanText(photo.alt_description || recipe.name), 54,
          photo.user?.name ? `Photo by ${photo.user.name} on Unsplash` : "Unsplash"));
      }
    } catch (error) {
      continue;
    }
  }
  return candidates;
}

async function findWikimediaImageCandidates(queries, recipe) {
  const candidates = [];
  for (const query of queries) {
    candidates.push(...await findWikimediaSearchCandidates(query, recipe));
  }
  for (const query of queries) {
    const vi = await findWikipediaSummaryCandidate(query, "vi", recipe);
    if (vi) {
      candidates.push(vi);
    }
  }
  for (const query of queries) {
    const en = await findWikipediaSummaryCandidate(query, "en", recipe);
    if (en) {
      candidates.push(en);
    }
  }
  return candidates;
}

function buildImageQueries(recipe, rawRecipe) {
  const values = [
    cleanText(recipe.imageSearchQuery),
    cleanText(rawRecipe && rawRecipe.imageSearchQuery),
    cleanText(recipe.name),
    `${cleanText(recipe.name)} Vietnamese food`,
    `${cleanText(recipe.name)} món ăn Việt Nam`,
    `${cleanText(recipe.category)} ${cleanText(recipe.name)}`
  ];
  const seen = new Set();
  const queries = [];
  for (const value of values) {
    const query = value.replace(/\s+/g, " ").trim();
    const key = removeVietnameseTone(query);
    if (!query || seen.has(key)) {
      continue;
    }
    seen.add(key);
    queries.push(query);
  }
  return queries.slice(0, 6);
}

async function findWikimediaSearchCandidates(query, recipe) {
  const apiUrl = "https://commons.wikimedia.org/w/api.php?"
    + "action=query&generator=search&gsrnamespace=6&gsrlimit=8"
    + "&prop=imageinfo&iiprop=url|mime&iiurlwidth=1200&format=json&origin=*"
    + `&gsrsearch=${encodeURIComponent(query)}`;
  try {
    const payload = await fetchJsonWithTimeout(apiUrl, IMAGE_SEARCH_TIMEOUT_MS);
    const pages = Object.values(payload.query?.pages || {});
    const candidates = [];
    for (const page of pages) {
      const imageInfo = page.imageinfo && page.imageinfo[0];
      if (!imageInfo || !String(imageInfo.mime || "").startsWith("image/")) {
        continue;
      }
      const imageUrl = cleanText(imageInfo.thumburl || imageInfo.url);
      if (isLikelyImageUrl(imageUrl)) {
        candidates.push(imageCandidate(imageUrl, "wikimedia",
          imageInfo.descriptionurl || "https://commons.wikimedia.org",
          cleanText(page.title || recipe.name), 64, "Wikimedia Commons"));
      }
    }
    return candidates;
  } catch (error) {
    return [];
  }
}

async function findWikipediaSummaryCandidate(query, language, recipe) {
  const title = encodeURIComponent(query.replace(/\s+/g, "_"));
  const apiUrl = `https://${language}.wikipedia.org/api/rest_v1/page/summary/${title}`;
  try {
    const payload = await fetchJsonWithTimeout(apiUrl, IMAGE_SEARCH_TIMEOUT_MS);
    const imageUrl = cleanText(payload.originalimage?.source || payload.thumbnail?.source);
    if (!isLikelyImageUrl(imageUrl)) {
      return null;
    }
    return imageCandidate(imageUrl, `wikipedia-${language}`,
      payload.content_urls?.desktop?.page || apiUrl,
      cleanText(payload.title || recipe.name), 62, "Wikipedia");
  } catch (error) {
    return null;
  }
}

async function fetchJsonWithTimeout(url, timeoutMs) {
  return fetchJsonWithHeaders(url, {}, timeoutMs);
}

async function fetchJsonWithHeaders(url, headers, timeoutMs) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        "Accept": "application/json",
        "User-Agent": "SoTayNauAnAI/1.0 recipe-image-fetcher",
        ...headers
      }
    });
    if (!response.ok) {
      throw new Error(`Image search ${response.status}`);
    }
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

async function fetchTextWithTimeout(url, timeoutMs) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        "Accept": "text/html,application/xhtml+xml",
        "User-Agent": "SoTayNauAnAI/1.0 recipe-image-fetcher"
      }
    });
    if (!response.ok) {
      throw new Error(`Recipe page ${response.status}`);
    }
    return await response.text();
  } finally {
    clearTimeout(timeout);
  }
}

function imageCandidate(url, sourceType, sourceUrl, label, baseScore, attribution) {
  const imageUrl = cleanText(url);
  if (!isLikelyImageUrl(imageUrl)) {
    return null;
  }
  return {
    url: imageUrl,
    sourceType: cleanText(sourceType),
    sourceUrl: cleanText(sourceUrl),
    label: cleanText(label),
    baseScore,
    attribution: cleanText(attribution),
    score: 0
  };
}

function chooseBestImageCandidate(candidates, recipe) {
  const unique = [];
  const seen = new Set();
  for (const candidate of candidates) {
    if (!candidate || !candidate.url || seen.has(candidate.url)) {
      continue;
    }
    seen.add(candidate.url);
    candidate.score = scoreImageCandidate(candidate, recipe);
    unique.push(candidate);
  }
  unique.sort((left, right) => right.score - left.score);
  return unique[0] || null;
}

function scoreImageCandidate(candidate, recipe) {
  let score = Number(candidate.baseScore) || 0;
  const haystack = removeVietnameseTone([
    candidate.label,
    candidate.url,
    candidate.sourceUrl
  ].join(" "));
  const recipeName = removeVietnameseTone(recipe.name);
  const category = removeVietnameseTone(recipe.category);
  const query = removeVietnameseTone(recipe.imageSearchQuery);
  if (recipeName && haystack.includes(recipeName)) {
    score += 22;
  }
  if (category && haystack.includes(category)) {
    score += 8;
  }
  for (const token of importantFoodTokens(`${recipe.name} ${recipe.imageSearchQuery}`)) {
    if (haystack.includes(token)) {
      score += 5;
    }
  }
  if (query && haystack.includes(query)) {
    score += 10;
  }
  if (candidate.sourceType === "recipe-jsonld") {
    score += 18;
  }
  if (candidate.sourceType === "recipe-meta") {
    score += 8;
  }
  if (/logo|avatar|icon|placeholder|banner|cover|sprite/.test(haystack)) {
    score -= 45;
  }
  if (/upload\.wikimedia\.org|cooky|dienmayxanh|bachhoaxanh|thatlangon|ngonaz/.test(haystack)) {
    score += 5;
  }
  return Math.max(0, Math.min(100, Math.round(score)));
}

function importantFoodTokens(value) {
  const stopWords = new Set(["mon", "an", "viet", "nam", "food", "vietnamese", "cach", "nau", "cong", "thuc"]);
  return removeVietnameseTone(value)
    .split(/[^a-z0-9]+/)
    .filter(token => token.length >= 3 && !stopWords.has(token))
    .slice(0, 8);
}

function resolveUrl(value, baseUrl) {
  try {
    return new URL(cleanText(value), baseUrl).toString();
  } catch (error) {
    return "";
  }
}

function isHttpUrl(value) {
  return /^https?:\/\//i.test(cleanText(value));
}

function decodeHtmlEntities(value) {
  return String(value || "")
    .replace(/&quot;/g, "\"")
    .replace(/&#34;/g, "\"")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&#39;/g, "'");
}

function isLikelyImageUrl(value) {
  const imageUrl = cleanText(value);
  if (!/^https?:\/\//i.test(imageUrl)) {
    return false;
  }
  if (/\.(html?|php|aspx?)(\?|$)/i.test(imageUrl)) {
    return false;
  }
  return /\.(png|jpe?g|webp|avif)(\?|$)/i.test(imageUrl)
    || imageUrl.includes("upload.wikimedia.org/")
    || imageUrl.includes("images.pexels.com/")
    || imageUrl.includes("images.unsplash.com/")
    || imageUrl.includes("pixabay.com/")
    || /\/(image|images|photo|photos|media|uploads|cdn)\//i.test(imageUrl)
    || imageUrl.includes("=image")
    || imageUrl.includes("format=");
}

function normalizeDifficulty(value) {
  const text = cleanText(value).toLowerCase();
  if (text.includes("khó")) {
    return "Khó";
  }
  if (text.includes("dễ")) {
    return "Dễ";
  }
  return "Trung bình";
}

function normalizeRecipeCategory(recipe, fallbackName) {
  const rawCategory = cleanText(recipe.category);
  if (rawCategory && rawCategory !== "Công thức của tôi" && rawCategory !== "Món khác") {
    return rawCategory;
  }
  const text = removeVietnameseTone([
    fallbackName,
    recipe.name,
    recipe.description,
    ...(Array.isArray(recipe.ingredients) ? recipe.ingredients : [])
  ].join(" "));
  if (containsAnyText(text, "canh", "kho qua nhoi thit")) return "Canh";
  if (containsAnyText(text, "lau")) return "Lẩu";
  if (containsAnyText(text, "kho", "rim")) return "Món kho";
  if (containsAnyText(text, "xao")) return "Món xào";
  if (containsAnyText(text, "chien", "ran")) return "Món chiên";
  if (containsAnyText(text, "nuong")) return "Món nướng";
  if (containsAnyText(text, "hap")) return "Món hấp";
  if (containsAnyText(text, "luoc")) return "Món luộc";
  if (containsAnyText(text, "bun", "pho", "hu tieu", "mi quang")) return "Bún/Phở";
  if (containsAnyText(text, "com")) return "Món cơm";
  if (containsAnyText(text, "goi", "salad")) return "Gỏi & Salad";
  if (containsAnyText(text, "banh")) return "Món bánh";
  if (containsAnyText(text, "che", "flan")) return "Tráng miệng";
  if (containsAnyText(text, "sinh to", "ca phe", "nuoc cam")) return "Nước uống";
  return "Món gia đình";
}

function containsAnyText(value, ...needles) {
  return needles.some(needle => value.includes(needle));
}

function removeVietnameseTone(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

function cleanStringArray(rows) {
  const source = Array.isArray(rows) ? rows : [];
  const values = [];
  for (const row of source) {
    const value = cleanText(row);
    if (value) {
      values.push(value);
    }
  }
  return values;
}

function clampInt(value, min, max, fallback) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  return Math.max(min, Math.min(max, parsed));
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
