package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectedIngredient {
    private final String name;
    private final String quantity;
    private final int count;
    private final double confidence;
    private final String source;
    private final List<Box> boxes;

    public DetectedIngredient(String name,
                              String quantity,
                              int count,
                              double confidence,
                              String source,
                              List<Box> boxes) {
        this.name = name == null ? "" : name.trim();
        this.quantity = quantity == null ? "" : quantity.trim();
        this.count = count;
        this.confidence = confidence;
        this.source = source == null || source.trim().isEmpty()
                ? ConfirmedIngredient.SOURCE_CAMERA
                : source.trim();
        this.boxes = boxes == null ? new ArrayList<>() : new ArrayList<>(boxes);
    }

    public String getName() {
        return name;
    }

    public String getQuantity() {
        return quantity;
    }

    public int getCount() {
        return count;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSource() {
        return source;
    }

    public List<Box> getBoxes() {
        return Collections.unmodifiableList(boxes);
    }

    public boolean isEmpty() {
        return name.isEmpty();
    }

    public static class Box {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;
        private final double confidence;

        public Box(double x1, double y1, double x2, double y2, double confidence) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.confidence = confidence;
        }

        public double getX1() {
            return x1;
        }

        public double getY1() {
            return y1;
        }

        public double getX2() {
            return x2;
        }

        public double getY2() {
            return y2;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
