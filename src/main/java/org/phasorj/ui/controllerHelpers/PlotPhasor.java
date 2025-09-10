package org.phasorj.ui.controllerHelpers;

import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.phasorj.ui.DataClass;
import org.phasorj.ui.PhasorProcessor;
import org.phasorj.ui.controllerHelpers.plot.*;

/**
 * Main controller class for the phasor plot visualization.
 * Coordinates between the drawing, interaction, and data management components.
 */
public class PlotPhasor {

    private final PlotRenderer plotRenderer;
    private final PlotInteractionHandler interactionHandler;
    private final PhasorDataManager dataManager;
    private final PlotTransform transform;

    private final Canvas plotCanvas;
    private final Canvas overlayCanvas;
    private final StackPane plotPane;
    private final ImageDisplay imageDisplay;

    private RandomAccessibleInterval<FloatType> intensity;

    public PlotPhasor(StackPane plotPane, ImageDisplay imageDisplay,
                      RandomAccessibleInterval<FloatType> intensity,
                      PhasorProcessor processor) {

        this.plotPane = plotPane;
        this.imageDisplay = imageDisplay;
        this.intensity = intensity;

        // Initialize components
        this.transform = new PlotTransform();
        this.dataManager = new PhasorDataManager(processor);

        // Create canvases
        this.plotCanvas = new Canvas(600, 500);
        this.overlayCanvas = new Canvas(600, 500);

        // Initialize renderer and interaction handler
        this.plotRenderer = new PlotRenderer(plotCanvas, overlayCanvas, transform, dataManager);
        this.interactionHandler = new PlotInteractionHandler(overlayCanvas, plotPane, transform,
                plotRenderer, dataManager, imageDisplay, intensity);

        setupCanvas();
        updatePhasorData();
    }

    public void setIntensityImage(RandomAccessibleInterval<FloatType> intensity) {
        this.intensity = intensity;
        interactionHandler.setIntensityImage(intensity);
    }

    private void setupCanvas() {
        // Configure canvas properties
        plotCanvas.setMouseTransparent(true);
        overlayCanvas.setMouseTransparent(false);

        // Setup interaction handlers
        interactionHandler.setupEventHandlers();

        // Add canvases to pane
        plotPane.getChildren().clear();
        plotPane.getChildren().addAll(plotCanvas, overlayCanvas);

        // Initial draw
        plotRenderer.redrawAll();
    }

    public void updatePhasorPlot() {
        updatePhasorData();
        plotRenderer.redrawPlot();
    }

    private void updatePhasorData() {
        dataManager.updateData();
    }

    public PlotInteractionHandler getInteractionHandler() {
        return this.interactionHandler;
    }


}