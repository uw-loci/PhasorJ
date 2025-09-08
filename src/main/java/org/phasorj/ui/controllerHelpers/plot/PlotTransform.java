package org.phasorj.ui.controllerHelpers.plot;

/**
 * This class includes functions that handle coordinate transformations, zoom, and pan operations for the plot.
 */

public class PlotTransform {

    // Plot area constants (in world coordinates)
    public static final double PLOT_LEFT = 100;
    public static final double PLOT_BOTTOM = 400;
    public static final double PLOT_WIDTH = 500;
    public static final double PLOT_HEIGHT = 300;
    public static final double PLOT_RIGHT = PLOT_LEFT + PLOT_WIDTH;
    public static final double PLOT_TOP = PLOT_BOTTOM - PLOT_HEIGHT;

    // Zoom and pan variables
    private double scaleFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 20.0;

    public double getScaleFactor() { return scaleFactor; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    // Transform operations
    public void translate(double deltaX, double deltaY) {
        offsetX += deltaX;
        offsetY += deltaY;
    }

    public void setOffset(double x, double y) {
        offsetX = x;
        offsetY = y;
    }

    public void setScale(double scale) {
        scaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public void reset() {
        scaleFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
    }

    public void fitToContent(double canvasWidth, double canvasHeight) {
        // Fit the plot area to the canvas
        double scaleX = canvasWidth * 0.8 / (PLOT_WIDTH + 100);
        double scaleY = canvasHeight * 0.8 / (PLOT_HEIGHT + 100);
        scaleFactor = Math.min(scaleX, scaleY);

        // Center the plot
        double plotCenterX = PLOT_LEFT + PLOT_WIDTH / 2;
        double plotCenterY = PLOT_TOP + PLOT_HEIGHT / 2;

        offsetX = canvasWidth / 2 - plotCenterX * scaleFactor;
        offsetY = canvasHeight / 2 - plotCenterY * scaleFactor;
    }

    public void zoomToBox(double screenLeft, double screenTop, double screenRight, double screenBottom,
                          double canvasWidth, double canvasHeight) {
        // Convert screen coordinates to world coordinates
        double worldLeft = (screenLeft - offsetX) / scaleFactor;
        double worldRight = (screenRight - offsetX) / scaleFactor;
        double worldTop = (screenTop - offsetY) / scaleFactor;
        double worldBottom = (screenBottom - offsetY) / scaleFactor;

        // Calculate required scale to fit the box
        double boxWidth = worldRight - worldLeft;
        double boxHeight = worldBottom - worldTop;

        // Calculate scale with padding
        double scaleX = (canvasWidth * 0.9) / boxWidth;
        double scaleY = (canvasHeight * 0.9) / boxHeight;
        double newScale = Math.min(scaleX, scaleY);

        // Clamp scale
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        // Calculate new offset to center the box
        double boxCenterX = (worldLeft + worldRight) / 2;
        double boxCenterY = (worldTop + worldBottom) / 2;

        offsetX = canvasWidth / 2 - boxCenterX * newScale;
        offsetY = canvasHeight / 2 - boxCenterY * newScale;

        scaleFactor = newScale;
    }

    // Coordinate conversion methods
    public double dataToScreenX(double dataX) {
        return PLOT_LEFT + dataX * PLOT_WIDTH;
    }

    public double dataToScreenY(double dataY) {
        return PLOT_BOTTOM - (dataY / 0.6) * PLOT_HEIGHT;
    }

    public double screenToDataX(double screenX) {
        double worldX = (screenX - offsetX) / scaleFactor;
        return (worldX - PLOT_LEFT) / PLOT_WIDTH;
    }

    public double screenToDataY(double screenY) {
        double worldY = (screenY - offsetY) / scaleFactor;
        return (PLOT_BOTTOM - worldY) / PLOT_HEIGHT * 0.6;
    }

    public boolean isInsidePlotArea(double screenX, double screenY) {
        double worldX = (screenX - offsetX) / scaleFactor;
        double worldY = (screenY - offsetY) / scaleFactor;

        return worldX >= PLOT_LEFT && worldX <= PLOT_RIGHT &&
                worldY >= PLOT_TOP && worldY <= PLOT_BOTTOM;
    }
}