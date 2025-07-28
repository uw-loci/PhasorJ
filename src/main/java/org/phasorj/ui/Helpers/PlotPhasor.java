package org.phasorj.ui.Helpers;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

import org.phasorj.ui.DataClass;
import org.phasorj.ui.MockData;
import org.phasorj.ui.PhasorProcessor;


public class PlotPhasor {

    private ScatterChart<Number, Number> phasorPlot;
    private PhasorProcessor processor;
    private StackPane plotPane;
    private ImageDisplay imageDisplay;
    private RandomAccessibleInterval<FloatType> intensity;

    public PlotPhasor(StackPane plotPane, ImageDisplay imageDisplay,
                      RandomAccessibleInterval<FloatType> intensity,
                      PhasorProcessor processor) {

        this.plotPane = plotPane;
        this.imageDisplay = imageDisplay;
        this.intensity = intensity;
        this.processor = processor;

        setupPlot();
    }

    public void setIntensityImage(RandomAccessibleInterval<FloatType> intensity) {
        this.intensity = intensity;
    }


    public void setupPlot(){
        //Set up x and y axis
        NumberAxis xAxis = new NumberAxis(0, 1, 0.1);
        xAxis.setLabel("G");
        xAxis.setAutoRanging(false);
        xAxis.setOpacity(0.5);

        NumberAxis yAxis = new NumberAxis(0, 0.6, 0.1);
        yAxis.setLabel("S");
        yAxis.setAutoRanging(false);
        yAxis.setOpacity(0.5);

        //Create the phasorplot
        phasorPlot = getScatterChart(xAxis, yAxis);
        phasorPlot.getStylesheets().addAll(getClass().getResource("/Css/plot.css").toExternalForm());

        //Creating the uniCircle
        LineChart<Number, Number> uniCircle = getLineChart(xAxis, yAxis);
        uniCircle.getStylesheets().addAll(getClass().getResource("/Css/plot.css").toExternalForm());

        //Add both the phasor plot and the universal circle to the pane
        plotPane.getChildren().addAll(phasorPlot, uniCircle);

        //Create a circle to be a cluster selector on the same axis as the plots
        //TODO: Make the cursor appear when the "Add Circle" button is clicked instead of automatically.
        //TODO: Allow multiple circles to be added
        //TODO: somehow now the circle can go beyond the chart bound.
        Circle circleCrs = getCircleCrs(xAxis, yAxis);
        circleCrs.setVisible(false);
        plotPane.getChildren().add(circleCrs);
        plotPane.setOnMouseClicked(mouseEvent -> {
            circleCrs.setVisible(true);
            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
        });
        circleCrs.setOnMouseDragged(event -> {
            drag(event);
            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
        });
        circleCrs.setOnScroll(event -> {
            resize(event, circleCrs);
            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasorPlot), imageDisplay, intensity);
        });
    }

    /**
     *
     * @param event
     * @param circle
     *
     * TODO: the size number was pick randomly. Need to be changed after I work on the window/image resizing.
     */
    private void resize(ScrollEvent event, Circle circle) {
        double delta = event.getDeltaY();
        double newRadius = circle.getRadius() + delta * 0.1;
        newRadius = Math.max(5, Math.min(newRadius, 200));
        circle.setRadius(newRadius);

    }

    /**
     *
     * @param event
     */
    private void drag(MouseEvent event) {
        Node n = (Node)event.getSource();
        n.setTranslateX(n.getTranslateX() + event.getX());
        n.setTranslateY(n.getTranslateY() + event.getY());
    }

    /**
     * @param xAxis
     * @param yAxis
     * @return the phasor plot
     */
    private ScatterChart<Number, Number> getScatterChart(NumberAxis xAxis, NumberAxis yAxis) {
        ScatterChart<Number, Number> phasor_plot = new ScatterChart<>(xAxis, yAxis);
        //format chart
        phasor_plot.setId("PhasorPlot");
        phasor_plot.setLegendVisible(false);
        phasor_plot.setHorizontalGridLinesVisible(false);
        phasor_plot.setVerticalGridLinesVisible(false);
        phasor_plot.setAlternativeColumnFillVisible(false);
        phasor_plot.setAlternativeRowFillVisible(false);
        phasor_plot.setHorizontalZeroLineVisible(false);
        phasor_plot.setVerticalZeroLineVisible(false);
        return phasor_plot;
    }

    /**
     *
     * @param xAxis
     * @param yAxis
     * @return a linechart of the universal circle
     */
    private LineChart<Number, Number> getLineChart(NumberAxis xAxis, NumberAxis yAxis){
        LineChart<Number, Number> uniCircle = new LineChart<>(xAxis, yAxis);
        uniCircle.setId("UniCircle");
        uniCircle.getData().add(getCircle());
        uniCircle.setLegendVisible(false);
        uniCircle.setHorizontalGridLinesVisible(false);
        uniCircle.setVerticalGridLinesVisible(false);
        uniCircle.setAlternativeColumnFillVisible(false);
        uniCircle.setAlternativeRowFillVisible(false);
        uniCircle.setHorizontalZeroLineVisible(false);
        uniCircle.setVerticalZeroLineVisible(false);
        return uniCircle;
    }

    /**
     * @param gData
     * @param sData
     * @return a XYChart series of data points (g, s) based on gData and sData
     * and record the spatial coordinate as ExtraValue
     */
    private XYChart.Series<Number, Number> getPhasorSeries(float[][] gData, float[][] sData) {

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Phasor Points");
        List<XYChart.Data<Number, Number>> pointList = new ArrayList<>();

        int row_num = gData.length;
        int col_num = gData[0].length;
        for (int i = 0; i < row_num; i++) {
            for (int j = 0; j < col_num; j++){
            float g = gData[i][j];
            float s = sData[i][j];

            //Points with both g and s equal to 0 are ignored.
            if (g != 0 || s != 0) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(g, s);
                //Recording the spatial coordinate of the value on the 2D image.
                point.setExtraValue(new int[]{i, j});
                pointList.add(point);
                }
            }
        }
        series.getData().addAll(pointList);
        return series;
    }

    /**
     *
     * @return a series of 100 points on the universal circle centered at (0.5, 0)
     */
    private XYChart.Series<Number, Number> getCircle() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int numPoints = 100;
        double radius = 0.5;

        for (int i = 0; i < (numPoints+1); i++) {
            double angle = Math.PI * i / numPoints;
            double x = radius * Math.cos(angle) + 0.5;
            double y = radius * Math.sin(angle);
            series.getData().add(new XYChart.Data<>(x, y));
        }
        return  series;
    }

    /**
     * Create a circle with transparent at position (0, 0)
     * @param xAxis
     * @param yAxis
     * @return
     */
    private Circle getCircleCrs(NumberAxis xAxis, NumberAxis yAxis){
        Circle CrsCircle = new Circle(20);
        CrsCircle.setFill(Color.TRANSPARENT);
        CrsCircle.setStroke(Color.RED);
        CrsCircle.setStrokeWidth(2);

        double startX = xAxis.getDisplayPosition(0.0);
        double startY = yAxis.getDisplayPosition(0.0);
        CrsCircle.setTranslateX(startX);
        CrsCircle.setTranslateY(startY);
        return CrsCircle;
    }

    /**
     * @param cir
     * @param phasorplot
     * @return a list of [x,y] locations of pixels with (g, s) value inside the circle
     */
    private List<int[]> getPointInsideCursors(Circle cir, ScatterChart<Number, Number> phasorplot){
        Bounds circleBounds = cir.localToScene(cir.getBoundsInLocal());

        List<int[]> locations = new ArrayList<>();

        for (XYChart.Series<Number, Number> series : phasorplot.getData()){
            for (XYChart.Data<Number, Number> data : series.getData()){
                Node node = data.getNode();
                Bounds pointBounds = node.localToScene(node.getBoundsInLocal());
                double pointX = (pointBounds.getMinX() + pointBounds.getMaxX()) / 2;
                double pointY = (pointBounds.getMinY() + pointBounds.getMaxY()) / 2;

                double circleCenterX = (circleBounds.getMinX() + circleBounds.getMaxX()) / 2;
                double circleCenterY = (circleBounds.getMinY() + circleBounds.getMaxY()) / 2;
                double radius = cir.getRadius();

                double dx = pointX - circleCenterX;
                double dy = pointY - circleCenterY;
                double sum_of_squared = dx * dx + dy * dy;

                if (sum_of_squared <= (radius * radius)){
                    int[] loc = (int[]) data.getExtraValue();
                    locations.add(loc);
                }
            }
        }
        return locations;
    }

    /**
     * @param circleCrs
     * @param coords
     * @param imageDisplay
     * @param intenistyImage
     *
     * Displays an annotated intensity image with the pixel having (g, s) value inside the cursor circle highlighted in the cursor’s color.
     */
    private void highlightImage(Circle circleCrs, List<int[]> coords, ImageDisplay imageDisplay, RandomAccessibleInterval<FloatType> intenistyImage){
        Color strokeColor = (Color) circleCrs.getStroke();

        int red = (int) (strokeColor.getRed() * 255);
        int green = (int) (strokeColor.getGreen() * 255);
        int blue = (int) (strokeColor.getBlue() * 255);
        int alpha = (int) (strokeColor.getOpacity() * 255);
        int highlightColor = ARGBType.rgba(red, green, blue, alpha);

        //TODO: We don't want to load the whole image everytime the cursor get updated. Might do overlaying instead.
        imageDisplay.setImage(intenistyImage, ImageDisplay.INTENSITY_CONV, (srcRA, lutedRA) -> {
            long x = srcRA.getLongPosition(0);
            long y = srcRA.getLongPosition(1);

            for (int[] coord : coords) {
                if (coord[0] == y && coord[1] == x) {
                    return new ARGBType(highlightColor);
                }
            }
            return lutedRA.get();
        });
    }

    public void updatePhasorPlot(){
        phasorPlot.getData().clear();
        for (DataClass data : processor.getEntries()) {
            float[][] gData = data.getGData();
            float[][] sData = data.getSData();
            phasorPlot.getData().add(getPhasorSeries(gData, sData));
        }
    }
}
