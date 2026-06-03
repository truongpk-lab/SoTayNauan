# YOLO model registry

Each detector model lives in its own folder:

```text
models/
├── models.json
└── original_yolov8s/
    ├── model.pt
    └── metadata.json
```

To add or upgrade a model:

1. Create `models/<model_id>/`.
2. Put the trained weight at `models/<model_id>/model.pt`.
3. Add `metadata.json` with model notes and limitations.
4. Add an entry in `models.json`.
5. Set `YOLO_MODEL_ID=<model_id>` in the detector environment.

`YOLO_MODEL_PATH` can still override the registry for one-off testing, but regular app runs should use `YOLO_MODEL_ID`.
