package org.phasorj.ui.Helpers;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.phasorj.ui.DataClass;
import org.phasorj.ui.PhasorProcessor;

public class PlotPhasor {

    //TODO: refactor this function. All drawing function should be moved to another class (

    // Main plot canvas
    private Canvas plotCanvas;
    private GraphicsContext plotGC;

    // Overlay canvas for UI elements (circle cursor, instructions, etc.)
    private Canvas overlayCanvas;
    private GraphicsContext overlayGC;

    // Zoom and pan variables
    private double scaleFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 20.0;

    // Pan variables
    private boolean isPanning = false;
    private double lastPanX = 0;
    private double lastPanY = 0;

    // Zoom box variables
    private boolean isZoomBoxActive = false;
    private boolean isDraggingZoomBox = false;
    private double zoomBoxStartX = 0;
    private double zoomBoxStartY = 0;
    private double zoomBoxCurrentX = 0;
    private double zoomBoxCurrentY = 0;

    // UI overlay toggle
    private boolean showOverlay = true;

    // Plot area constants (in world coordinates)
    private static final double PLOT_LEFT = 100;
    private static final double PLOT_BOTTOM = 400;
    private static final double PLOT_WIDTH = 500;
    private static final double PLOT_HEIGHT = 300;
    private static final double PLOT_RIGHT = PLOT_LEFT + PLOT_WIDTH;
    private static final double PLOT_TOP = PLOT_BOTTOM - PLOT_HEIGHT;

    private PhasorProcessor processor;
    private StackPane plotPane;
    private ImageDisplay imageDisplay;
    private RandomAccessibleInterval<FloatType> intensity;

    private List<PhasorPoint> phasorPoints = new ArrayList<>();

    // Data structure for phasor points
    private static class PhasorPoint {
        public final float g, s;
        public final int imageX, imageY;

        public PhasorPoint(float g, float s, int imageX, int imageY) {
            this.g = g;
            this.s = s;
            this.imageX = imageX;
            this.imageY = imageY;
        }
    }

    public PlotPhasor(StackPane plotPane, ImageDisplay imageDisplay,
                      RandomAccessibleInterval<FloatType> intensity,
                      PhasorProcessor processor) {

        this.plotPane = plotPane;
        this.imageDisplay = imageDisplay;
        this.intensity = intensity;
        this.processor = processor;

        setupCanvas();
        updatePhasorData();
    }

    public void setIntensityImage(RandomAccessibleInterval<FloatType> intensity) {
        this.intensity = intensity;
    }

    private void setupCanvas() {
        // Create both canvases
        plotCanvas = new Canvas(600, 500);
        plotGC = plotCanvas.getGraphicsContext2D();

        overlayCanvas = new Canvas(600, 500);
        overlayGC = overlayCanvas.getGraphicsContext2D();

        // Only the overlay canvas receives mouse events
        plotCanvas.setMouseTransparent(true);
        overlayCanvas.setMouseTransparent(false);

        // Event handlers on overlay canvas
        overlayCanvas.setOnMousePressed(this::handleMousePressed);
        overlayCanvas.setOnMouseDragged(this::handleMouseDragged);
        overlayCanvas.setOnMouseReleased(this::handleMouseReleased);
        overlayCanvas.setOnScroll(this::handleScroll);

        overlayCanvas.setOnKeyPressed(this::handleKeyPressed);
        plotPane.setOnKeyPressed(this::handleKeyPressed);

        // FIXED: Make both focusTraversable
        overlayCanvas.setFocusTraversable(true);
        plotPane.setFocusTraversable(true);

        // Clear existing content and add canvases
        plotPane.getChildren().clear();
        plotPane.getChildren().addAll(plotCanvas, overlayCanvas);

        // Initial draw
        redrawPlot();
        redrawOverlay();
    }

    private void toggleZoomBoxMode() {
        isZoomBoxActive = !isZoomBoxActive;
        if (!isZoomBoxActive) {
            isDraggingZoomBox = false;
        }
        // Update cursor based on mode
        if (isZoomBoxActive) {
            overlayCanvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
        } else {
            overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
        redrawOverlay();
    }

    private void zoomToBox(double screenLeft, double screenTop, double screenRight, double screenBottom) {
        // Convert screen coordinates to world coordinates (accounting for current transform)
        double worldLeft = (screenLeft - offsetX) / scaleFactor;
        double worldRight = (screenRight - offsetX) / scaleFactor;
        double worldTop = (screenTop - offsetY) / scaleFactor;
        double worldBottom = (screenBottom - offsetY) / scaleFactor;

        // Calculate required scale to fit the box
        double boxWidth = worldRight - worldLeft;
        double boxHeight = worldBottom - worldTop;

        double canvasWidth = overlayCanvas.getWidth();
        double canvasHeight = overlayCanvas.getHeight();

        // Calculate scale with some padding (90% of available space)
        double scaleX = (canvasWidth * 0.9) / boxWidth;
        double scaleY = (canvasHeight * 0.9) / boxHeight;
        double newScale = Math.min(scaleX, scaleY);

        // Clamp scale to limits
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        // Calculate new offset to center the box
        double boxCenterX = (worldLeft + worldRight) / 2;
        double boxCenterY = (worldTop + worldBottom) / 2;

        offsetX = canvasWidth / 2 - boxCenterX * newScale;
        offsetY = canvasHeight / 2 - boxCenterY * newScale;

        scaleFactor = newScale;

        redrawPlot();

        plotPane.requestFocus();
        overlayCanvas.requestFocus();

        overlayCanvas.setOnMouseClicked(event -> {
            overlayCanvas.requestFocus();
            plotPane.requestFocus();
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.R) {
            resetView();
        } else if (event.getCode() == KeyCode.F) {
            fitToContent();
        } else if (event.getCode() == KeyCode.H) {
            showOverlay = !showOverlay;
            redrawOverlay();
        } else if (event.getCode() == KeyCode.Z) {
            toggleZoomBoxMode();
        }
        event.consume();
    }

    private void handleMousePressed(MouseEvent event) {
        // ADDED: Ensure focus when mouse is pressed
        overlayCanvas.requestFocus();
        plotPane.requestFocus();

        if (event.getButton() == MouseButton.PRIMARY) {
            if (isZoomBoxActive) {
                // Start zoom box selection
                zoomBoxStartX = event.getX();
                zoomBoxStartY = event.getY();
                zoomBoxCurrentX = event.getX();
                zoomBoxCurrentY = event.getY();
                isDraggingZoomBox = true;
                redrawOverlay();
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (isZoomBoxActive) {
                // Right-click cancels zoom box mode
                isZoomBoxActive = false;
                isDraggingZoomBox = false;
                redrawOverlay();
            } else {
                // Right-click starts panning
                isPanning = true;
                lastPanX = event.getX();
                lastPanY = event.getY();
                overlayCanvas.setCursor(javafx.scene.Cursor.MOVE);
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isDraggingZoomBox && event.getButton() == MouseButton.PRIMARY) {
            zoomBoxCurrentX = event.getX();
            zoomBoxCurrentY = event.getY();
            redrawOverlay();
        } else if (isPanning && event.getButton() == MouseButton.SECONDARY) {
            double deltaX = event.getX() - lastPanX;
            double deltaY = event.getY() - lastPanY;

            offsetX += deltaX;
            offsetY += deltaY;

            lastPanX = event.getX();
            lastPanY = event.getY();

            redrawPlot();
            redrawOverlay();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (isDraggingZoomBox && event.getButton() == MouseButton.PRIMARY) {
            isDraggingZoomBox = false;

            // Calculate zoom box dimensions
            double boxLeft = Math.min(zoomBoxStartX, zoomBoxCurrentX);
            double boxRight = Math.max(zoomBoxStartX, zoomBoxCurrentX);
            double boxTop = Math.min(zoomBoxStartY, zoomBoxCurrentY);
            double boxBottom = Math.max(zoomBoxStartY, zoomBoxCurrentY);

            // Only zoom if the box is large enough (minimum 10x10 pixels)
            if (Math.abs(boxRight - boxLeft) > 10 && Math.abs(boxBottom - boxTop) > 10) {
                zoomToBox(boxLeft, boxTop, boxRight, boxBottom);
            }

            // Exit zoom box mode after zooming
            isZoomBoxActive = false;
            redrawOverlay();
        } else if (isPanning) {
            isPanning = false;
            overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private void handleScroll(ScrollEvent event) {
        event.consume();
        if (!isZoomBoxActive) {
            double delta = (event.getDeltaY() > 0) ? 1.1 : 1 / 1.1;
            zoomAtPoint(event.getX(), event.getY(), delta);
        }
    }



    private void zoomAtPoint(double x, double y, double delta) {
        double newScale = scaleFactor * delta;
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        if (newScale != scaleFactor) {
            offsetX = x - (x - offsetX) * (newScale / scaleFactor);
            offsetY = y - (y - offsetY) * (newScale / scaleFactor);
            scaleFactor = newScale;
            redrawPlot();
            redrawOverlay();
        }
    }

    private void resetView() {
        scaleFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        redrawPlot();
        redrawOverlay();
    }

    private void fitToContent() {
        // Fit the plot area to the canvas
        double scaleX = plotCanvas.getWidth() * 0.8 / (PLOT_WIDTH + 100);
        double scaleY = plotCanvas.getHeight() * 0.8 / (PLOT_HEIGHT + 100);
        scaleFactor = Math.min(scaleX, scaleY);

        // Center the plot
        double plotCenterX = PLOT_LEFT + PLOT_WIDTH / 2;
        double plotCenterY = PLOT_TOP + PLOT_HEIGHT / 2;

        offsetX = plotCanvas.getWidth() / 2 - plotCenterX * scaleFactor;
        offsetY = plotCanvas.getHeight() / 2 - plotCenterY * scaleFactor;

        redrawPlot();
        redrawOverlay();
    }

    /**
     * manually request focus - call this from your main application
     */
    public void requestFocus() {
        plotPane.requestFocus();
        overlayCanvas.requestFocus();
    }

    /**
     * Redraws only the main plot content
     */
    private void redrawPlot() {
        plotGC.clearRect(0, 0, plotCanvas.getWidth(), plotCanvas.getHeight());

        Affine oldTransform = plotGC.getTransform();

        // Apply zoom and pan transform
        plotGC.translate(offsetX, offsetY);
        plotGC.scale(scaleFactor, scaleFactor);

        drawPlotContent();

        // Restore transform
        plotGC.setTransform(oldTransform);
    }

    /**
     * Redraws only the overlay elements
     */
    private void redrawOverlay() {
        overlayGC.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

        // Draw zoom box if active
        if (isDraggingZoomBox && isZoomBoxActive) {
            drawZoomBox();
        }

        if (showOverlay) {
            drawInstructions();
        }
    }

    private void drawPlotContent() {
        // Draw axes
        plotGC.setStroke(Color.BLACK);
        plotGC.setLineWidth(2 / scaleFactor);

        // X-axis (horizontal) - G axis from 0 to 1
        plotGC.strokeLine(PLOT_LEFT, PLOT_BOTTOM, PLOT_RIGHT, PLOT_BOTTOM);
        // Y-axis (vertical) - S axis from 0 to 0.6
        plotGC.strokeLine(PLOT_LEFT, PLOT_BOTTOM, PLOT_LEFT, PLOT_TOP);

        // Draw grid
        plotGC.setStroke(Color.LIGHTGRAY);
        plotGC.setLineWidth(1 / scaleFactor);

        // Vertical grid lines
        for (int i = 1; i <= 10; i++) {
            double x = PLOT_LEFT + (i * PLOT_WIDTH / 10.0);
            plotGC.strokeLine(x, PLOT_BOTTOM, x, PLOT_TOP);
        }

        // Horizontal grid lines
        for (int i = 1; i <= 6; i++) {
            double y = PLOT_BOTTOM - (i * PLOT_HEIGHT / 6.0);
            plotGC.strokeLine(PLOT_LEFT, y, PLOT_RIGHT, y);
        }

        // Draw labels
        plotGC.setFill(Color.BLACK);
        double fontSize = 12 / scaleFactor;
        plotGC.setFont(javafx.scene.text.Font.font(fontSize));

        // G-axis labels (0 to 1)
        for (int i = 0; i <= 10; i++) {
            double x = PLOT_LEFT + (i * PLOT_WIDTH / 10.0);
            double value = i * 0.1;
            plotGC.fillText(String.format("%.1f", value), x - 8, PLOT_BOTTOM + 20);

            plotGC.setStroke(Color.BLACK);
            plotGC.setLineWidth(1 / scaleFactor);
            plotGC.strokeLine(x, PLOT_BOTTOM - 3, x, PLOT_BOTTOM + 3);
        }

        // S-axis labels (0 to 0.6)
        for (int i = 0; i <= 6; i++) {
            double y = PLOT_BOTTOM - (i * PLOT_HEIGHT / 6.0);
            double value = i * 0.1;
            plotGC.fillText(String.format("%.1f", value), PLOT_LEFT - 30, y + 4);

            plotGC.setStroke(Color.BLACK);
            plotGC.setLineWidth(1 / scaleFactor);
            plotGC.strokeLine(PLOT_LEFT - 3, y, PLOT_LEFT + 3, y);
        }

        // Axis titles
        plotGC.setFont(javafx.scene.text.Font.font(fontSize * 1.2));
        plotGC.fillText("G", PLOT_LEFT + PLOT_WIDTH / 2 - 5, PLOT_BOTTOM + 45);
        plotGC.fillText("S", PLOT_LEFT - 50, PLOT_TOP + PLOT_HEIGHT / 2 + 5);

        // Draw universal circle
        drawUniversalCircle();

        // Draw phasor data points
        drawPhasorPoints();
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


    private void drawUniversalCircle() {
        // Universal circle: semicircle centered at (0.5, 0) with radius 0.5
        double centerDataX = 0.5;
        double centerDataY = 0.0;
        double radiusData = 0.5;

        // Convert to screen coordinates
        double centerScreenX = dataToScreenX(centerDataX);
        double centerScreenY = dataToScreenY(centerDataY);
        double radiusScreenX = radiusData * PLOT_WIDTH;
        double radiusScreenY = radiusData * (PLOT_HEIGHT / 0.6); // S axis goes to 0.6

        // Draw semicircle outline
        plotGC.setStroke(Color.BLACK);
        plotGC.setLineWidth(1 / scaleFactor);

        double arcX = centerScreenX - radiusScreenX;
        double arcY = centerScreenY - radiusScreenY;
        double arcWidth = 2 * radiusScreenX;
        double arcHeight = 2 * radiusScreenY;

        // Draw semicircle (upper half only)
        plotGC.strokeArc(arcX, arcY, arcWidth, arcHeight, 0, 180, javafx.scene.shape.ArcType.OPEN);
    }

    private void drawPhasorPoints() {
        plotGC.setFill(Color.BLUE);
        double pointSize = 2 / scaleFactor;

        for (PhasorPoint point : phasorPoints) {
            if (point.g != 0 || point.s != 0) { // Skip zero points
                double screenX = dataToScreenX(point.g);
                double screenY = dataToScreenY(point.s);
                plotGC.fillOval(screenX - pointSize/2, screenY - pointSize/2, pointSize, pointSize);
            }
        }
    }

    private void drawInstructions() {
        overlayGC.setFill(Color.BLACK);
        overlayGC.setFont(javafx.scene.text.Font.font(12));

        String[] instructions = {
                String.format("Zoom: %.1f%%", scaleFactor * 100),
                "Right-click + drag: Pan",
                "Mouse wheel: Zoom" + (isZoomBoxActive ? " (disabled in zoom mode)" : ""),
                "Z: Toggle zoom box mode" + (isZoomBoxActive ? " (ON)" : " (OFF)"),
                "R: Reset view",
                "F: Fit to content",
                "H: Toggle this overlay",
                // Focus instruction
                "Click plot area first to enable keys"
        };

        if (isZoomBoxActive) {
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

    // Helper methods to convert between data coordinates and screen coordinates
    private double dataToScreenX(double dataX) {
        // Map data x (0 to 1) to screen x (PLOT_LEFT to PLOT_RIGHT)
        return PLOT_LEFT + dataX * PLOT_WIDTH;
    }

    private double dataToScreenY(double dataY) {
        // Map data y (0 to 0.6) to screen y (PLOT_BOTTOM to PLOT_TOP)
        return PLOT_BOTTOM - (dataY / 0.6) * PLOT_HEIGHT;
    }
//      TODO:: Add circle cursors
//    private double screenToDataX(double screenX) {
//        // Convert screen coordinate back to data coordinate
//        return (screenX - PLOT_LEFT) / PLOT_WIDTH;
//    }
//
//    private double screenToDataY(double screenY) {
//        // Convert screen coordinate back to data coordinate
//        return (PLOT_BOTTOM - screenY) / PLOT_HEIGHT * 0.6;
//    }

    public void updatePhasorPlot() {
        updatePhasorData();
        redrawPlot();
    }

    private void updatePhasorData() {
        phasorPoints.clear();

        for (DataClass data : processor.getEntries()) {
            RandomAccessibleInterval<FloatType> gData = data.getGData();
            RandomAccessibleInterval<FloatType> sData = data.getSData();

            Cursor<FloatType> gCursor = gData.cursor();
            RandomAccess<FloatType> sAccess = sData.randomAccess();

            while (gCursor.hasNext()) {
                gCursor.fwd();
                sAccess.setPosition(gCursor);

                float g = gCursor.get().getRealFloat();
                float s = sAccess.get().getRealFloat();

                // Get spatial coordinates
                int imageX = (int) gCursor.getIntPosition(0);
                int imageY = (int) gCursor.getIntPosition(1);

                phasorPoints.add(new PhasorPoint(g, s, imageX, imageY));
            }
        }
    }


}