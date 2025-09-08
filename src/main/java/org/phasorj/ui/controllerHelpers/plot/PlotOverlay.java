package org.phasorj.ui.controllerHelpers.plot;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Handles drawing of overlay elements like cursor, zoom box, and instructions.
 */
public class PlotOverlay {

    private final GraphicsContext overlayGC;
    private final PlotTransform transform;

    // Zoom box state
    private boolean zoomBoxActive = false;
    private boolean zoomBoxDragging = false;
    private double zoomBoxStartX, zoomBoxStartY, zoomBoxCurrentX, zoomBoxCurrentY;

    // Cursor state
    private boolean cursorVisible = false;
    private double cursorX, cursorY, cursorRadius;

    // UI state
    private boolean overlayVisible = true;

    public PlotOverlay(GraphicsContext overlayGC, PlotTransform transform) {
        this.overlayGC = overlayGC;
        this.transform = transform;
    }

    public void redraw() {
        overlayGC.clearRect(0, 0, overlayGC.getCanvas().getWidth(), overlayGC.getCanvas().getHeight());

        if (zoomBoxDragging && zoomBoxActive) {
            drawZoomBox();
        }

        if (cursorVisible && !zoomBoxDragging) {
            drawCircleCursor();
        }

        if (overlayVisible) {
            drawInstructions();
        }
    }

    private void drawZoomBox() {
        double left = Math.min(zoomBoxStartX, zoomBoxCurrentX);
        double right = Math.max(zoomBoxStartX, zoomBoxCurrentX);
        double top = Math.min(zoomBoxStartY, zoomBoxCurrentY);
        double bottom = Math.max(zoomBoxStartY, zoomBoxCurrentY);

        // Draw zoom box outline
        overlayGC.setStroke(Color.LIGHTGRAY);
        overlayGC.setLineWidth(1);
        overlayGC.strokeRect(left, top, right - left, bottom - top);

        // Draw semi-transparent fill
        overlayGC.setFill(Color.color(0, 0, 1, 0.1));
        overlayGC.fillRect(left, top, right - left, bottom - top);
    }

    private void drawCircleCursor() {
        if (!transform.isInsidePlotArea(cursorX, cursorY)) {
            return;
        }

        overlayGC.setStroke(Color.RED);
        overlayGC.setLineWidth(1.5);

        overlayGC.strokeOval(cursorX - cursorRadius, cursorY - cursorRadius,
                2 * cursorRadius, 2 * cursorRadius);
    }

    private void drawInstructions() {
        overlayGC.setFill(Color.BLACK);
        overlayGC.setFont(javafx.scene.text.Font.font(12));

        String[] instructions = {
                String.format("Zoom: %.1f%%", transform.getScaleFactor() * 100),
                "Right-click + drag: Pan",
                "Mouse wheel: Change circle cursor's size",
                "Z: Toggle zoom box mode" + (zoomBoxActive ? " (ON)" : " (OFF)"),
                "C: Toggle cursor" + (cursorVisible ? " (ON)" : " (OFF)"),
                "R: Reset view",
                "F: Fit to content",
                "H: Toggle this overlay",
                "Click plot area first to enable keys"
        };

        if (zoomBoxActive) {
            String[] zoomInstructions = {
                    "",
                    "Zoom Box Mode:",
                    "Left-click + drag: Select area to zoom",
                    "Right-click: Cancel zoom mode"
            };

            String[] allInstructions = new String[instructions.length + zoomInstructions.length];
            System.arraycopy(instructions, 0, allInstructions, 0, instructions.length);
            System.arraycopy(zoomInstructions, 0, allInstructions, instructions.length, zoomInstructions.length);
            instructions = allInstructions;
        }

        // Semi-transparent background
        overlayGC.setFill(Color.color(1, 1, 1, 0.9));
        overlayGC.fillRect(10, 10, 200, instructions.length * 18 + 10);

        // Instructions text
        overlayGC.setFill(Color.BLACK);
        for (int i = 0; i < instructions.length; i++) {
            overlayGC.fillText(instructions[i], 15, 25 + i * 18);
        }
    }

    public void setZoomBoxState(boolean active, boolean dragging,
                                double startX, double startY, double currentX, double currentY) {
        this.zoomBoxActive = active;
        this.zoomBoxDragging = dragging;
        this.zoomBoxStartX = startX;
        this.zoomBoxStartY = startY;
        this.zoomBoxCurrentX = currentX;
        this.zoomBoxCurrentY = currentY;
    }

    public void setCursorState(boolean visible, double x, double y, double radius) {
        this.cursorVisible = visible;
        this.cursorX = x;
        this.cursorY = y;
        this.cursorRadius = radius;
    }

    public void setOverlayVisible(boolean visible) {
        this.overlayVisible = visible;
    }

    public boolean isZoomBoxActive() { return zoomBoxActive; }
    public boolean isCursorVisible() { return cursorVisible; }
}