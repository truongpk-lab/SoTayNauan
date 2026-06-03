import argparse

from ultralytics import YOLO


def main():
    parser = argparse.ArgumentParser(description="Train ingredient detector")
    parser.add_argument("--data", default="ingredients_v1.yaml")
    parser.add_argument("--model", default="yolo26s.pt")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=960)
    parser.add_argument("--batch", type=int, default=-1)
    parser.add_argument("--project", default="runs/ingredients")
    parser.add_argument("--name", default="yolo26s-ingredients-v1")
    args = parser.parse_args()

    model = YOLO(args.model)
    model.train(
        data=args.data,
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        project=args.project,
        name=args.name,
        patience=20,
        cos_lr=True,
        close_mosaic=10,
    )


if __name__ == "__main__":
    main()
