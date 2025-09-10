package org.phasorj.ui.controllerHelpers.plot;

import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

import org.phasorj.ui.controllerHelpers.ImageDisplay;

/**
 * Handles all user interactions including mouse events, keyboard shortcuts, and cursor management.
 */
public class PlotInteractionHandler {

    private final Canvas overlayCanvas;
    private final StackPane plotPane;
    private final PlotTransform transform;
    private final PlotRenderer renderer;
    private final PhasorDataManager dataManager;
    private final ImageDisplay imageDisplay;

    private RandomAccessibleInterval<FloatType> intensity;

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

    // Circle cursor variables
    private boolean showCircleCursor = true;
    private double cursorScreenX = 0;
    private double cursorScreenY = 0;
    private boolean cursorVisible = false;
    private double cursorCircleRadius = 10;

    private static final double MIN_CURSOR_RADIUS = 2;
    private static final double MAX_CURSOR_RADIUS = 300;

    // UI overlay toggle
    private boolean showOverlay = true;

    private Canvas highlightOverlay;
    private boolean highlightOverlaySetup = false;

    public PlotInteractionHandler(Canvas overlayCanvas, StackPane plotPane, PlotTransform transform,
                                  PlotRenderer renderer, PhasorDataManager dataManager,
                                  ImageDisplay imageDisplay, RandomAccessibleInterval<FloatType> intensity) {
        this.overlayCanvas = overlayCanvas;
        this.plotPane = plotPane;
        this.transform = transform;
        this.renderer = renderer;
        this.dataManager = dataManager;
        this.imageDisplay = imageDisplay;
        this.intensity = intensity;
    }

    public void setIntensityImage(RandomAccessibleInterval<FloatType> intensity) {
        this.intensity = intensity;
    }

    public void setupEventHandlers() {
        overlayCanvas.setOnMousePressed(this::handleMousePressed);
        overlayCanvas.setOnMouseDragged(this::handleMouseDragged);
        overlayCanvas.setOnMouseReleased(this::handleMouseReleased);
        overlayCanvas.setOnMouseMoved(this::handleMouseMoved);
        overlayCanvas.setOnMouseEntered(this::handleMouseEntered);
        overlayCanvas.setOnMouseExited(this::handleMouseExited);
        overlayCanvas.setOnScroll(this::handleScroll);

        overlayCanvas.setOnKeyPressed(this::handleKeyPressed);
        plotPane.setOnKeyPressed(this::handleKeyPressed);

        overlayCanvas.setFocusTraversable(true);
        plotPane.setFocusTraversable(true);

        overlayCanvas.setOnMouseClicked(event -> {
            overlayCanvas.requestFocus();
            plotPane.requestFocus();
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case R:
                transform.reset();
                renderer.redrawAll();
                break;
            case F:
                transform.fitToContent(overlayCanvas.getWidth(), overlayCanvas.getHeight());
                renderer.redrawAll();
                break;
            case H:
                showOverlay = !showOverlay;
                renderer.setOverlayVisible(showOverlay);
                renderer.redrawOverlay();
                break;
            case Z:
                toggleZoomBoxMode();
                break;
            case C:
                toggleCircleCursor();
                break;
        }
        event.consume();
    }

    private void handleMousePressed(MouseEvent event) {
        overlayCanvas.requestFocus();
        plotPane.requestFocus();

        if (event.getButton() == MouseButton.PRIMARY) {
            if (isZoomBoxActive) {
                startZoomBox(event.getX(), event.getY());
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (isZoomBoxActive) {
                cancelZoomBoxMode();
            } else {
                startPanning(event.getX(), event.getY());
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isDraggingZoomBox && event.getButton() == MouseButton.PRIMARY) {
            updateZoomBox(event.getX(), event.getY());
        } else if (isPanning && event.getButton() == MouseButton.SECONDARY) {
            updatePanning(event.getX(), event.getY());
        }

        updateCursorPosition(event.getX(), event.getY());
    }

    private void handleMouseReleased(MouseEvent event) {
        if (isDraggingZoomBox && event.getButton() == MouseButton.PRIMARY) {
            finishZoomBox();
        } else if (isPanning) {
            finishPanning();
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        updateCursorPosition(event.getX(), event.getY());
    }

    private void handleMouseEntered(MouseEvent event) {
        if (showCircleCursor) {
            cursorVisible = true;
            updateRendererCursorState();
        }
    }

    private void handleMouseExited(MouseEvent event) {
        if (showCircleCursor) {
            cursorVisible = false;
            updateRendererCursorState();
            clearHighlights();
        }
    }

    private void handleScroll(ScrollEvent event) {
        event.consume();

        if (showCircleCursor && cursorVisible) {
            double delta = (event.getDeltaY() > 0) ? 1.2 : 1 / 1.2;
            double newRadius = cursorCircleRadius * delta;

            cursorCircleRadius = Math.max(MIN_CURSOR_RADIUS, Math.min(MAX_CURSOR_RADIUS, newRadius));

            updateRendererCursorState();
            highlightImagePixels();
        }
    }

    private void toggleZoomBoxMode() {
        isZoomBoxActive = !isZoomBoxActive;
        if (!isZoomBoxActive) {
            isDraggingZoomBox = false;
        }

        // Update cursor
        if (isZoomBoxActive) {
            overlayCanvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
        } else {
            overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }

        updateRendererZoomBoxState();
    }

    private void toggleCircleCursor() {
        showCircleCursor = !showCircleCursor;
        if (!showCircleCursor) {
            cursorVisible = false;
            clearHighlights();
        }
        updateRendererCursorState();
    }

    private void startZoomBox(double x, double y) {
        zoomBoxStartX = x;
        zoomBoxStartY = y;
        zoomBoxCurrentX = x;
        zoomBoxCurrentY = y;
        isDraggingZoomBox = true;
        updateRendererZoomBoxState();
    }

    private void updateZoomBox(double x, double y) {
        zoomBoxCurrentX = x;
        zoomBoxCurrentY = y;
        updateRendererZoomBoxState();
    }

    private void finishZoomBox() {
        isDraggingZoomBox = false;

        // Calculate zoom box dimensions
        double boxLeft = Math.min(zoomBoxStartX, zoomBoxCurrentX);
        double boxRight = Math.max(zoomBoxStartX, zoomBoxCurrentX);
        double boxTop = Math.min(zoomBoxStartY, zoomBoxCurrentY);
        double boxBottom = Math.max(zoomBoxStartY, zoomBoxCurrentY);

        // Only zoom if the box is large enough (minimum 10x10 pixels)
        if (Math.abs(boxRight - boxLeft) > 10 && Math.abs(boxBottom - boxTop) > 10) {
            transform.zoomToBox(boxLeft, boxTop, boxRight, boxBottom,
                    overlayCanvas.getWidth(), overlayCanvas.getHeight());
            renderer.redrawAll();
        }

        // Exit zoom box mode after zooming
        isZoomBoxActive = false;
        overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        updateRendererZoomBoxState();
    }

    private void cancelZoomBoxMode() {
        isZoomBoxActive = false;
        isDraggingZoomBox = false;
        overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        updateRendererZoomBoxState();
    }

    private void startPanning(double x, double y) {
        isPanning = true;
        lastPanX = x;
        lastPanY = y;
        overlayCanvas.setCursor(javafx.scene.Cursor.MOVE);
    }

    private void updatePanning(double x, double y) {
        double deltaX = x - lastPanX;
        double deltaY = y - lastPanY;

        transform.translate(deltaX, deltaY);

        lastPanX = x;
        lastPanY = y;

        renderer.redrawAll();
    }

    private void finishPanning() {
        isPanning = false;
        overlayCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
    }

    private void updateCursorPosition(double x, double y) {
        if (showCircleCursor) {
            cursorScreenX = x;
            cursorScreenY = y;
           // cursorVisible = transform.isInsidePlotArea(x, y);
            updateRendererCursorState();

            if (cursorVisible) {
                updateHighlights(); // Replace highlightImagePixels() with this
            } else {
                clearHighlights(); // Clear when cursor not visible
            }
        }
    }
    private void updateRendererZoomBoxState() {
        renderer.setZoomBoxState(isZoomBoxActive, isDraggingZoomBox,
                zoomBoxStartX, zoomBoxStartY, zoomBoxCurrentX, zoomBoxCurrentY);
        renderer.redrawOverlay();
    }

    private void updateRendererCursorState() {
        renderer.setCursorState(showCircleCursor && cursorVisible && !isDraggingZoomBox,
                cursorScreenX, cursorScreenY, cursorCircleRadius);
        renderer.redrawOverlay();
    }

    private void highlightImagePixels() {
        if (!showCircleCursor || !cursorVisible || intensity == null) {
            return;
        }

        List<int[]> coords = getPointsInsideCursor();

        imageDisplay.setImage(intensity, ImageDisplay.INTENSITY_CONV, (srcRA, lutedRA) -> {
            long x = srcRA.getLongPosition(0);
            long y = srcRA.getLongPosition(1);

            for (int[] coord : coords) {
                if (coord[0] == y && coord[1] == x) {
                    ARGBType redPixel = new ARGBType();
                    redPixel.set(0xFFFF0000); // Red color (ARGB format)
                    return redPixel;
                }
            }
            return lutedRA.get();
        });
    }

    private List<int[]> getPointsInsideCursor() {
        if (!showCircleCursor || !cursorVisible) {
            return List.of();
        }

        // Convert cursor screen position to data coordinates
        double cursorDataX = transform.screenToDataX(cursorScreenX);
        double cursorDataY = transform.screenToDataY(cursorScreenY);

        // Convert cursor radius from screen pixels to data coordinates
        double radiusDataX = cursorCircleRadius / (transform.getScaleFactor() * PlotTransform.PLOT_WIDTH);
        double radiusDataY = cursorCircleRadius / (transform.getScaleFactor() * PlotTransform.PLOT_HEIGHT) * 0.6;

        List<int[]> coords = dataManager.getPointsInsideCursor(cursorDataX, cursorDataY, radiusDataX, radiusDataY);

        return coords;
    }

    public void setupHighlightOverlay(Pane imageParent) {
        if (highlightOverlaySetup) return;

        highlightOverlay = new Canvas();
        highlightOverlay.setMouseTransparent(true); // Let mouse events pass through

        // Get the ImageView to match its size
        ImageView imageView = imageDisplay.getView();

        // Don't bind any properties - just set initial size and let resize listener handle updates
        highlightOverlay.setWidth(imageView.getFitWidth());
        highlightOverlay.setHeight(imageView.getFitHeight());

        // For AnchorPane, set the anchors to fill the container like the ImageView
        if (imageParent instanceof javafx.scene.layout.AnchorPane) {
            javafx.scene.layout.AnchorPane anchorParent = (javafx.scene.layout.AnchorPane) imageParent;
            anchorParent.getChildren().add(highlightOverlay);

            // Set anchors to fill the container (same as ImageView in your FXML)
            javafx.scene.layout.AnchorPane.setTopAnchor(highlightOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setBottomAnchor(highlightOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setLeftAnchor(highlightOverlay, 0.0);
            javafx.scene.layout.AnchorPane.setRightAnchor(highlightOverlay, 0.0);
        } else {
            // For other container types, just add it
            imageParent.getChildren().add(highlightOverlay);
        }

        // Add listeners to update canvas size when ImageView size changes
        imageView.fitWidthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                highlightOverlay.setWidth(newVal.doubleValue());
            }
        });

        imageView.fitHeightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                highlightOverlay.setHeight(newVal.doubleValue());
            }
        });

        highlightOverlaySetup = true;
    }

    private void updateHighlights() {
        if (highlightOverlay == null) return;

        GraphicsContext gc = highlightOverlay.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightOverlay.getWidth(), highlightOverlay.getHeight());

        if (!showCircleCursor || intensity == null) {
            return;
        }

        // Convert cursor position to data coordinates
        double cursorDataX = transform.screenToDataX(cursorScreenX);
        double cursorDataY = transform.screenToDataY(cursorScreenY);

        List<int[]> coords = getPointsInsideCursor();

        if (coords.isEmpty()) return;

        // Calculate pixel size on screen
        double canvasWidth = highlightOverlay.getWidth();
        double canvasHeight = highlightOverlay.getHeight();
        double pixelWidth = canvasWidth / intensity.dimension(0);
        double pixelHeight = canvasHeight / intensity.dimension(1);

        // Set highlight style
        gc.setFill(Color.RED.deriveColor(0, 1, 1, 0.6));
        gc.setStroke(Color.RED.deriveColor(0, 1, 1, 0.8));
        gc.setLineWidth(0.5);

        // Draw highlights for each coordinate
        for (int[] coord : coords) {
            double screenX = coord[0] * pixelWidth;
            double screenY = coord[1] * pixelHeight;

            gc.fillRect(screenX, screenY, pixelWidth, pixelHeight);
            gc.strokeRect(screenX, screenY, pixelWidth, pixelHeight);
        }
    }
    private void clearHighlights() {
        if (highlightOverlay == null) return;

        GraphicsContext gc = highlightOverlay.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightOverlay.getWidth(), highlightOverlay.getHeight());
    }


}