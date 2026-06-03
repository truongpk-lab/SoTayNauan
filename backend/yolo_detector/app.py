import base64
import io
import json
import os
import re
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, List

from fastapi import FastAPI, HTTPException
from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel, Field
from ultralytics import YOLO


BASE_DIR = Path(__file__).resolve().parent
MODEL_REGISTRY_PATH = Path(os.getenv(
    "YOLO_MODEL_REGISTRY",
    str(BASE_DIR / "models" / "models.json"),
))
MODEL_ID = os.getenv("YOLO_MODEL_ID", "")

app = FastAPI(title="So Tay Nau An Ingredient YOLO Detector")


class DetectRequest(BaseModel):
    imageBase64: str = Field(min_length=1)
    mimeType: str = "image/jpeg"


class BoxResponse(BaseModel):
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float


class IngredientResponse(BaseModel):
    name: str
    quantity: str
    count: int
    confidence: float
    boxes: List[BoxResponse]


class DetectResponse(BaseModel):
    ingredients: List[IngredientResponse]
    model: str


@lru_cache(maxsize=1)
def load_model_config():
    registry = load_model_registry()
    default_model_id = MODEL_ID or str(registry.get("defaultModelId", "")).strip()
    models = registry.get("models", {})
    if not default_model_id or default_model_id not in models:
        raise RuntimeError(f"YOLO model id is not configured: {default_model_id}")
    model_config = dict(models[default_model_id])
    model_config["id"] = default_model_id
    model_config["weightPath"] = resolve_weight_path(model_config)
    model_config["modelName"] = os.getenv(
        "YOLO_MODEL_NAME",
        str(model_config.get("modelName") or default_model_id),
    )
    model_config["confidence"] = float(os.getenv(
        "YOLO_CONFIDENCE",
        str(model_config.get("confidence", 0.35)),
    ))
    model_config["iou"] = float(os.getenv(
        "YOLO_IOU",
        str(model_config.get("iou", 0.55)),
    ))
    model_config["imageSize"] = int(os.getenv(
        "YOLO_IMAGE_SIZE",
        str(model_config.get("imageSize", 960)),
    ))
    model_config["maxResults"] = int(os.getenv(
        "YOLO_MAX_RESULTS",
        str(model_config.get("maxResults", 12)),
    ))
    return model_config


@lru_cache(maxsize=1)
def load_model():
    model_config = load_model_config()
    weight_path = Path(model_config["weightPath"])
    if not weight_path.exists():
        raise RuntimeError(f"YOLO model file not found: {weight_path}")
    return YOLO(str(weight_path))


@app.get("/health")
def health():
    model_config = load_model_config()
    return {
        "ok": True,
        "modelId": model_config["id"],
        "model": model_config["modelName"],
        "modelPath": str(model_config["weightPath"]),
        "confidence": model_config["confidence"],
        "iou": model_config["iou"],
        "imageSize": model_config["imageSize"],
        "maxResults": model_config["maxResults"],
    }


@app.post("/detect", response_model=DetectResponse)
def detect(payload: DetectRequest):
    if not payload.mimeType.startswith("image/"):
        raise HTTPException(status_code=400, detail="mimeType must be an image")
    image = decode_image(payload.imageBase64)
    model_config = load_model_config()
    model = load_model()
    results = model.predict(
        image,
        conf=model_config["confidence"],
        iou=model_config["iou"],
        imgsz=model_config["imageSize"],
        verbose=False,
    )
    names = model.names if isinstance(model.names, dict) else {}
    ingredients = aggregate_boxes(results, names)
    return DetectResponse(
        ingredients=ingredients[:model_config["maxResults"]],
        model=model_config["modelName"],
    )


def load_model_registry() -> Dict[str, Any]:
    if not MODEL_REGISTRY_PATH.exists():
        raise RuntimeError(f"YOLO model registry not found: {MODEL_REGISTRY_PATH}")
    with MODEL_REGISTRY_PATH.open("r", encoding="utf-8") as file:
        return json.load(file)


def resolve_weight_path(model_config: Dict[str, Any]) -> str:
    explicit_path = os.getenv("YOLO_MODEL_PATH", "").strip()
    if explicit_path:
        return explicit_path
    raw_path = Path(str(model_config.get("weightPath", "")).strip())
    if raw_path.is_absolute():
        return str(raw_path)
    return str(MODEL_REGISTRY_PATH.parent / raw_path)


def decode_image(image_base64: str) -> Image.Image:
    try:
        image_bytes = base64.b64decode(image_base64, validate=True)
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except (ValueError, UnidentifiedImageError) as exc:
        raise HTTPException(status_code=400, detail="Invalid base64 image") from exc


def aggregate_boxes(results, names: Dict[int, str]) -> List[IngredientResponse]:
    grouped = {}
    for result in results:
        if result.boxes is None:
            continue
        for box in result.boxes:
            class_id = int(box.cls[0])
            raw_name = str(names.get(class_id, class_id))
            name = ingredient_name(raw_name)
            if not name:
                continue
            coords = box.xyxy[0].tolist()
            confidence = clamp(float(box.conf[0]))
            group = grouped.setdefault(name, {"boxes": [], "confidenceTotal": 0.0})
            group["boxes"].append(BoxResponse(
                x1=float(coords[0]),
                y1=float(coords[1]),
                x2=float(coords[2]),
                y2=float(coords[3]),
                confidence=confidence,
            ))
            group["confidenceTotal"] += confidence

    rows = []
    for name, group in grouped.items():
        boxes = group["boxes"]
        count = len(boxes)
        confidence = clamp(group["confidenceTotal"] / max(1, count))
        rows.append(IngredientResponse(
            name=name,
            quantity=quantity_from_count(name, count),
            count=count,
            confidence=round(confidence, 4),
            boxes=boxes,
        ))
    rows.sort(key=lambda row: row.confidence, reverse=True)
    return rows


def ingredient_name(raw_name: str) -> str:
    key = normalize_key(raw_name)
    mapped = INGREDIENT_NAME_MAP.get(key)
    if mapped:
        return mapped
    # Custom models may already use Vietnamese labels.
    fallback = raw_name.replace("_", " ").replace("-", " ").strip().lower()
    fallback = re.sub(r"\s+", " ", fallback)
    return fallback if is_allowed_vietnamese_ingredient(fallback) else ""


def quantity_from_count(name: str, count: int) -> str:
    if count <= 0:
        return ""
    unit = unit_for_name(name)
    return f"{count} {unit}" if unit else ""


def unit_for_name(name: str) -> str:
    normalized = name.lower()
    if re.search(r"(trứng|cà chua|chanh|ớt|dưa leo)", normalized):
        return "quả"
    if re.search(r"(tỏi|hành tím|hành tây|cà rốt|khoai tây|gừng|sả)", normalized):
        return "củ"
    if re.search(r"(hành lá|rau muống|cải xanh|bắp cải)", normalized):
        return "bó"
    if re.search(r"(thịt|cá|tôm|mực|đậu hũ|bún|mì|gạo|cơm|nấm|bí đỏ)", normalized):
        return "phần"
    return ""


def normalize_key(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def clamp(value: float) -> float:
    return max(0.0, min(1.0, value))


def is_allowed_vietnamese_ingredient(value: str) -> bool:
    if not value:
        return False
    return bool(re.search(
        r"(trứng|cà chua|hành|tỏi|ớt|cà rốt|khoai|rau|cải|bắp cải|"
        r"dưa leo|bí đỏ|nấm|đậu hũ|thịt|cá|tôm|mực|bún|mì|gạo|cơm|"
        r"chanh|gừng|sả|chuối|táo|cam|bông cải)",
        value,
    ))


INGREDIENT_NAME_MAP = {
    "egg": "trứng",
    "eggs": "trứng",
    "chicken_egg": "trứng",
    "tomato": "cà chua",
    "tomatoes": "cà chua",
    "green_onion": "hành lá",
    "spring_onion": "hành lá",
    "garlic": "tỏi",
    "shallot": "hành tím",
    "onion": "hành tây",
    "chili": "ớt",
    "chilli": "ớt",
    "carrot": "cà rốt",
    "broccoli": "bông cải",
    "banana": "chuối",
    "apple": "táo",
    "orange": "cam",
    "potato": "khoai tây",
    "water_spinach": "rau muống",
    "mustard_green": "cải xanh",
    "cabbage": "bắp cải",
    "cucumber": "dưa leo",
    "pumpkin": "bí đỏ",
    "mushroom": "nấm",
    "tofu": "đậu hũ",
    "pork": "thịt heo",
    "beef": "thịt bò",
    "chicken": "thịt gà",
    "fish": "cá",
    "shrimp": "tôm",
    "prawn": "tôm",
    "squid": "mực",
    "rice_noodle": "bún",
    "noodle": "mì",
    "rice": "gạo",
    "cooked_rice": "cơm",
    "lime": "chanh",
    "lemon": "chanh",
    "ginger": "gừng",
    "lemongrass": "sả",
}
