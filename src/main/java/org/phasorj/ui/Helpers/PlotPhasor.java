package org.phasorj.ui.Helpers;

import java.util.ArrayList;
import java.util.List;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.FloatDataSet;
import io.fair_acc.dataset.spi.Histogram2;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

import org.phasorj.ui.DataClass;
import org.phasorj.ui.PhasorProcessor;

import static io.fair_acc.dataset.events.ChartBits.DataSetStyle;
import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;


public class PlotPhasor {

    private PhasorProcessor processor;
    private StackPane plotPane;
    private XYChart phasorPlot;

    public PlotPhasor(StackPane plotPane,
                      PhasorProcessor processor) {

        this.plotPane = plotPane;
        this.processor = processor;
        setupPlot();
    }

    public void setupPlot() {
        plotPane.getChildren().clear();
        phasorPlot = getXYChart();
        plotPane.getChildren().add(phasorPlot);
    }

    /**
     * @return the phasor plot
     */
    private XYChart getXYChart() {
        //setup
        DefaultNumericAxis xAxis = new DefaultNumericAxis("g", 0, 1.1, 0.1);
        DefaultNumericAxis yAxis = new DefaultNumericAxis("s", 0, 0.6, 0.1);

        XYChart chart = new XYChart(xAxis, yAxis);
        chart.setId("PhasorPlot");
        chart.setLegendVisible(false);


        //Create heatmap
        Histogram2 heatmapHist = new Histogram2(
                "phasor-heatmap",
                200, 0.0, 1,   // x-bins (g)
                100, 0.0, 0.5,  // y-bins (s)
                BINS_ALIGNED_WITH_BOUNDARY
        );

        for (var data : processor.getEntries()) {
            FloatDataSet series = getPhasorSeries(data.getGData(), data.getSData());
            double[] gVals = series.getXValues();
            double[] sVals = series.getYValues();
            for (int i = 0; i < gVals.length; i++) {
                heatmapHist.fill(gVals[i], sVals[i]);
            }
        }

        ContourDataSetRenderer heatmapRenderer = new ContourDataSetRenderer();
        heatmapRenderer.getAxes().addAll(xAxis, yAxis);
        heatmapRenderer.getDatasets().add(heatmapHist);


        ErrorDataSetRenderer circleRenderer = new ErrorDataSetRenderer();
        circleRenderer.getAxes().addAll(xAxis, yAxis);
        circleRenderer.getDatasets().add(getCircle());


        chart.getRenderers().addAll(heatmapRenderer, circleRenderer);

        return chart;
    }

    /**
     * @return a series of 100 points on the universal circle centered at (0.5, 0)
     */
    private DoubleDataSet getCircle() {
        DoubleDataSet series = new DoubleDataSet("uniCircle");
        series.setStyle("-fx-stroke: black; -fx-fill: transparent; -fx-draw-marker: false;");

        int numPoints = 100;
        double[] xcords = new double[100];
        double[] ycords = new double[100];

        double radius = 0.5;

        for (int i = 0; i < numPoints; i++) {
            double angle = Math.PI * i / numPoints;
            xcords[i] = radius * Math.cos(angle) + 0.5;
            ycords[i] = radius * Math.sin(angle);
        }
        series.set(xcords, ycords);


        return series;
    }


    /**
     * @param gData
     * @param sData
     * @return a XYChart series of data points (g, s) based on gData and sData
     * and record the spatial coordinate as ExtraValue
     */
    private FloatDataSet getPhasorSeries(RandomAccessibleInterval<FloatType> gData, RandomAccessibleInterval<FloatType> sData) {

        int width = (int) gData.dimension(0);
        int height = (int) gData.dimension(1);
        int numPoints = width * height;

        float[] gValues = new float[numPoints];
        float[] sValues = new float[numPoints];

        Cursor<FloatType> gCursor = gData.cursor();
        RandomAccess<FloatType> sAccess = sData.randomAccess();

        int index = 0;
        while (gCursor.hasNext()) {
            gCursor.fwd();
            sAccess.setPosition(gCursor);

            float g = gCursor.get().getRealFloat();
            float s = sAccess.get().getRealFloat();

            gValues[index] = g;
            sValues[index] = s;
            index++;
        }

        FloatDataSet series = new FloatDataSet("Phasor Points");
        series.set(gValues, sValues);

        return series;
    }
}
//
//
//    /**
//     *
//     * @param event
//     * @param circle
//     *
//     * TODO: the size number was pick randomly. Need to be changed after I work on the window/image resizing.
//     */
//    private void resize(ScrollEvent event, Circle circle) {
//        double delta = event.getDeltaY();
//        double newRadius = circle.getRadius() + delta * 0.1;
//        newRadius = Math.max(5, Math.min(newRadius, 200));
//        circle.setRadius(newRadius);
//
//    }
//
//    /**
//     *
//     * @param event
//     */
//    private void drag(MouseEvent event) {
//        Node n = (Node)event.getSource();
//        n.setTranslateX(n.getTranslateX() + event.getX());
//        n.setTranslateY(n.getTranslateY() + event.getY());
//    }
//


//    /**
//     * Create a circle with transparent at position (0, 0)
//     * @param xAxis
//     * @param yAxis
//     * @return
//     */
//    private Circle getCircleCrs(NumberAxis xAxis, NumberAxis yAxis){
//        Circle CrsCircle = new Circle(20);
//        CrsCircle.setFill(Color.TRANSPARENT);
//        CrsCircle.setStroke(Color.RED);
//        CrsCircle.setStrokeWidth(2);
//
//        double startX = xAxis.getDisplayPosition(0.0);
//        double startY = yAxis.getDisplayPosition(0.0);
//        CrsCircle.setTranslateX(startX);
//        CrsCircle.setTranslateY(startY);
//        return CrsCircle;
//    }

//    /**
//     * @param cir
//     * @param phasorplot
//     * @return a list of [x,y] locations of pixels with (g, s) value inside the circle
//     */
//    private List<int[]> getPointInsideCursors(Circle cir, ScatterChart<Number, Number> phasorplot){
//        Bounds circleBounds = cir.localToScene(cir.getBoundsInLocal());
//
//        List<int[]> locations = new ArrayList<>();
//
//        for (XYChart.Series<Number, Number> series : phasorplot.getData()){
//            for (XYChart.Data<Number, Number> data : series.getData()){
//                Node node = data.getNode();
//                Bounds pointBounds = node.localToScene(node.getBoundsInLocal());
//                double pointX = (pointBounds.getMinX() + pointBounds.getMaxX()) / 2;
//                double pointY = (pointBounds.getMinY() + pointBounds.getMaxY()) / 2;
//
//                double circleCenterX = (circleBounds.getMinX() + circleBounds.getMaxX()) / 2;
//                double circleCenterY = (circleBounds.getMinY() + circleBounds.getMaxY()) / 2;
//                double radius = cir.getRadius();
//
//                double dx = pointX - circleCenterX;
//                double dy = pointY - circleCenterY;
//                double sum_of_squared = dx * dx + dy * dy;
//
//                if (sum_of_squared <= (radius * radius)){
//                    int[] loc = (int[]) data.getExtraValue();
//                    locations.add(loc);
//                }
//            }
//        }
//        return locations;
//    }
//
//    /**
//     * @param circleCrs
//     * @param coords
//     * @param imageDisplay
//     * @param intenistyImage
//     *
//     * Displays an annotated intensity image with the pixel having (g, s) value inside the cursor circle highlighted in the cursor’s color.
//     */
//    private void highlightImage(Circle circleCrs, List<int[]> coords, ImageDisplay imageDisplay, RandomAccessibleInterval<FloatType> intenistyImage){
//        Color strokeColor = (Color) circleCrs.getStroke();
//
//        int red = (int) (strokeColor.getRed() * 255);
//        int green = (int) (strokeColor.getGreen() * 255);
//        int blue = (int) (strokeColor.getBlue() * 255);
//        int alpha = (int) (strokeColor.getOpacity() * 255);
//        int highlightColor = ARGBType.rgba(red, green, blue, alpha);
//
//        //TODO: We don't want to load the whole image everytime the cursor get updated. Might do overlaying instead.
//        imageDisplay.setImage(intenistyImage, ImageDisplay.INTENSITY_CONV, (srcRA, lutedRA) -> {
//            long x = srcRA.getLongPosition(0);
//            long y = srcRA.getLongPosition(1);
//
//            for (int[] coord : coords) {
//                if (coord[0] == x && coord[1] == y) {
//                    return new ARGBType(highlightColor);
//                }
//            }
//            return lutedRA.get();
//        });
//    }}
////
//    public void updatePhasorPlot(){
//        phasorPlot.getData().clear();
//        for (DataClass data : processor.getEntries()) {
//            RandomAccessibleInterval<FloatType> gData = data.getGData();
//            RandomAccessibleInterval<FloatType> sData = data.getSData();
//            phasorPlot.getData().add(getPhasorSeries(gData, sData));
//        }
//    }
//
//    private void setupCircle(Circle circleCrs){
//        //Create a circle to be a cluster selector on the same axis as the plots
//        //TODO: Make the cursor appear when the "Add Circle" button is clicked instead of automatically.
//        //TODO: Allow multiple circles to be added
//        //TODO: somehow now the circle can go beyond the chart bound.
//
//        circleCrs.setVisible(false);
//        plotPane.getChildren().add(circleCrs);
//        plotPane.setOnMouseClicked(mouseEvent -> {
//            circleCrs.setVisible(true);
//            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
//        });
//        circleCrs.setOnMouseDragged(event -> {
//            drag(event);
//            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
//        });
//        circleCrs.setOnScroll(event -> {
//            resize(event, circleCrs);
//            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
//        });
//    }
//}
