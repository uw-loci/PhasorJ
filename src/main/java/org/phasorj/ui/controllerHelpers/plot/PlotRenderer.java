package org.phasorj.ui.controllerHelpers.plot;

import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import org.phasorj.ui.controllerHelpers.plot.PhasorDataManager.PhasorPoint;

/**
 * Handles all drawing operations for the phasor plot.
 */
public class PlotRenderer {

    private final Canvas plotCanvas;
    private final Canvas overlayCanvas;
    private final GraphicsContext plotGC;
    private final GraphicsContext overlayGC;

    private final PlotTransform transform;
    private final PhasorDataManager dataManager;

    private final PlotOverlay overlay;

    public PlotRenderer(Canvas plotCanvas, Canvas overlayCanvas,
                        PlotTransform transform, PhasorDataManager dataManager) {
        this.plotCanvas = plotCanvas;
        this.overlayCanvas = overlayCanvas;
        this.plotGC = plotCanvas.getGraphicsContext2D();
        this.overlayGC = overlayCanvas.getGraphicsContext2D();
        this.transform = transform;
        this.dataManager = dataManager;
        this.overlay = new PlotOverlay(overlayGC, transform);
    }

    public void redrawAll() {
        redrawPlot();
        redrawOverlay();
    }

    public void redrawPlot() {
        plotGC.clearRect(0, 0, plotCanvas.getWidth(), plotCanvas.getHeight());

        Affine oldTransform = plotGC.getTransform();

        // Apply zoom and pan transform
        plotGC.translate(transform.getOffsetX(), transform.getOffsetY());
        plotGC.scale(transform.getScaleFactor(), transform.getScaleFactor());

        drawPlotContent();

        // Restore transform
        plotGC.setTransform(oldTransform);
    }

    public void redrawOverlay() {
        overlay.redraw();
    }

    private void drawPlotContent() {
        drawAxes();
        drawGrid();
        drawLabels();
        drawUniversalCircle();
        drawPhasorPoints();
    }

    private void drawAxes() {
        plotGC.setStroke(Color.BLACK);
        plotGC.setLineWidth(2 / transform.getScaleFactor());

        // X-axis (horizontal) - G axis from 0 to 1
        plotGC.strokeLine(PlotTransform.PLOT_LEFT, PlotTransform.PLOT_BOTTOM,
                PlotTransform.PLOT_RIGHT, PlotTransform.PLOT_BOTTOM);
        // Y-axis (vertical) - S axis from 0 to 0.6
        plotGC.strokeLine(PlotTransform.PLOT_LEFT, PlotTransform.PLOT_BOTTOM,
                PlotTransform.PLOT_LEFT, PlotTransform.PLOT_TOP);
    }

    private void drawGrid() {
        plotGC.setStroke(Color.LIGHTGRAY);
        plotGC.setLineWidth(1 / transform.getScaleFactor());

        // Vertical grid lines
        for (int i = 1; i <= 10; i++) {
            double x = PlotTransform.PLOT_LEFT + (i * PlotTransform.PLOT_WIDTH / 10.0);
            plotGC.strokeLine(x, PlotTransform.PLOT_BOTTOM, x, PlotTransform.PLOT_TOP);
        }

        // Horizontal grid lines
        for (int i = 1; i <= 6; i++) {
            double y = PlotTransform.PLOT_BOTTOM - (i * PlotTransform.PLOT_HEIGHT / 6.0);
            plotGC.strokeLine(PlotTransform.PLOT_LEFT, y, PlotTransform.PLOT_RIGHT, y);
        }
    }

    private void drawLabels() {
        plotGC.setFill(Color.BLACK);
        double fontSize = 12 / transform.getScaleFactor();
        plotGC.setFont(javafx.scene.text.Font.font(fontSize));

        // G-axis labels (0 to 1)
        for (int i = 0; i <= 10; i++) {
            double x = PlotTransform.PLOT_LEFT + (i * PlotTransform.PLOT_WIDTH / 10.0);
            double value = i * 0.1;
            plotGC.fillText(String.format("%.1f", value), x - 8, PlotTransform.PLOT_BOTTOM + 20);

            plotGC.setStroke(Color.BLACK);
            plotGC.setLineWidth(1 / transform.getScaleFactor());
            plotGC.strokeLine(x, PlotTransform.PLOT_BOTTOM - 3, x, PlotTransform.PLOT_BOTTOM + 3);
        }

        // S-axis labels (0 to 0.6)
        for (int i = 0; i <= 6; i++) {
            double y = PlotTransform.PLOT_BOTTOM - (i * PlotTransform.PLOT_HEIGHT / 6.0);
            double value = i * 0.1;
            plotGC.fillText(String.format("%.1f", value), PlotTransform.PLOT_LEFT - 30, y + 4);

            plotGC.setStroke(Color.BLACK);
            plotGC.setLineWidth(1 / transform.getScaleFactor());
            plotGC.strokeLine(PlotTransform.PLOT_LEFT - 3, y, PlotTransform.PLOT_LEFT + 3, y);
        }

        // Axis titles
        plotGC.setFont(javafx.scene.text.Font.font(fontSize * 1.2));
        plotGC.fillText("G", PlotTransform.PLOT_LEFT + PlotTransform.PLOT_WIDTH / 2 - 5,
                PlotTransform.PLOT_BOTTOM + 45);
        plotGC.fillText("S", PlotTransform.PLOT_LEFT - 50,
                PlotTransform.PLOT_TOP + PlotTransform.PLOT_HEIGHT / 2 + 5);
    }

    private void drawUniversalCircle() {
        // Universal circle: semicircle centered at (0.5, 0) with radius 0.5
        double centerDataX = 0.5;
        double centerDataY = 0.0;
        double radiusData = 0.5;

        // Convert to screen coordinates
        double centerScreenX = transform.dataToScreenX(centerDataX);
        double centerScreenY = transform.dataToScreenY(centerDataY);
        double radiusScreenX = radiusData * PlotTransform.PLOT_WIDTH;
        double radiusScreenY = radiusData * (PlotTransform.PLOT_HEIGHT / 0.6);

        // Draw semicircle outline
        plotGC.setStroke(Color.BLACK);
        plotGC.setLineWidth(1 / transform.getScaleFactor());

        double arcX = centerScreenX - radiusScreenX;
        double arcY = centerScreenY - radiusScreenY;
        double arcWidth = 2 * radiusScreenX;
        double arcHeight = 2 * radiusScreenY;

        // Draw semicircle (upper half only)
        plotGC.strokeArc(arcX, arcY, arcWidth, arcHeight, 0, 180,
                javafx.scene.shape.ArcType.OPEN);
    }

    private void drawPhasorPoints() {
        plotGC.setFill(Color.BLUE);
        double pointSize = 1 / transform.getScaleFactor();

        List<PhasorPoint> points = dataManager.getPhasorPoints();
        for (PhasorPoint point : points) {
            if (point.g != 0 || point.s != 0) { // Skip zero points
                double screenX = transform.dataToScreenX(point.g);
                double screenY = transform.dataToScreenY(point.s);
                plotGC.fillOval(screenX - pointSize/2, screenY - pointSize/2,
                        pointSize, pointSize);
            }
        }
    }

    // overlay actions
    public void setZoomBoxState(boolean active, boolean dragging,
                                double startX, double startY, double currentX, double currentY) {
        overlay.setZoomBoxState(active, dragging, startX, startY, currentX, currentY);
    }

    public void setCursorState(boolean visible, double x, double y, double radius) {
        overlay.setCursorState(visible, x, y, radius);
    }

    public void setOverlayVisible(boolean visible) {
        overlay.setOverlayVisible(visible);
    }

    public PlotOverlay getOverlay() {
        return overlay;
    }
}