package org.phasorj.ui;


import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class plotPhasor {

    public static void plot(StackPane plotPane, ImageDisplay imageDisplay,RandomAccessibleInterval<FloatType> intensity) throws IOException {
        //getting mock data
        float[][] gData = MockData.generateMockData(256, 256, 0, 1);
        float[][] sData = MockData.generateMockData(256, 256, (float) 0, 0.5F);


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
        ScatterChart<Number, Number> phasor_plot = getScatterChart(xAxis, yAxis);
        phasor_plot.getStylesheets().addAll(plotPhasor.class.getResource("/Css/plot.css").toExternalForm());
        phasor_plot.getData().add(getPhasorSeries(gData, sData));


        //Creating the uniCircle
        LineChart<Number, Number> uniCircle = getLineChart(xAxis, yAxis);
        uniCircle.getStylesheets().addAll(plotPhasor.class.getResource("/Css/plot.css").toExternalForm());

        plotPane.getChildren().addAll(phasor_plot, uniCircle);

        Circle circleCrs = getCircleCrs(xAxis, yAxis);
        circleCrs.setVisible(false);
        plotPane.getChildren().add(circleCrs);
        plotPane.setOnMouseClicked(mouseEvent -> {
            circleCrs.setVisible(true);
            highlightImage(circleCrs, getPointInsideCursors(circleCrs, phasor_plot), imageDisplay, intensity);
        });
        circleCrs.setOnMouseDragged(event -> drag(event));
        circleCrs.setOnScroll(event -> resize(event, circleCrs));
    }

    private static void resize(ScrollEvent event, Circle circle) {
        double delta = event.getDeltaY();
        double newRadius = circle.getRadius() + delta * 0.1;

        // Clamp radius
        newRadius = Math.max(5, Math.min(newRadius, 200));

        circle.setRadius(newRadius);
    }

    private static void drag(MouseEvent event) {
        Node n = (Node)event.getSource();
        n.setTranslateX(n.getTranslateX() + event.getX());
        n.setTranslateY(n.getTranslateY() + event.getY());
    }

    private static ScatterChart<Number, Number> getScatterChart(NumberAxis xAxis, NumberAxis yAxis) {
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

    private static LineChart<Number, Number> getLineChart(NumberAxis xAxis, NumberAxis yAxis){
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
//    private static float[] convertToFloatArray(RandomAccessibleInterval<FloatType> img) {
//        long size = img.size();
//
//        float[] data = new float[(int) size];
//        int i = 0;
//        for (FloatType val : Views.iterable(img)) {
//            data[i++] = val.getRealFloat();
//        }
//        return data;
//    }

//    public static float[] flatten(float[][] array) {
//        int rows = array.length;
//        int cols = array[0].length;
//        float[] result = new float[rows * cols];
//        for (int i = 0; i < rows; i++) {
//            System.arraycopy(array[i], 0, result, i * cols, cols);
//        }
//        return result;
//    }

    private static XYChart.Series<Number, Number> getPhasorSeries(float[][] gData, float[][] sData) {

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Phasor Points");


        List<XYChart.Data<Number, Number>> pointList = new ArrayList<>();

        int row_num = gData.length;
        int col_num = gData[0].length;
        for (int i = 0; i < row_num; i++) {
            for (int j = 0; j < col_num; j++){
            float g = gData[i][j];
            float s = sData[i][j];
            if (g != 0 || s != 0) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(g, s);
                point.setExtraValue(new int[]{i, j});
                pointList.add(point);
                }
            }
        }

        series.getData().addAll(pointList);
        return series;
    }

    private static XYChart.Series<Number, Number> getCircle() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int numPoints = 100;
        double radius = 0.5;

        for (int i = 0; i < (numPoints+1); i++) {
            double angle = Math.PI * i / numPoints;
            double x = radius * Math.cos(angle) + 0.501;
            double y = radius * Math.sin(angle);
            series.getData().add(new XYChart.Data<>(x, y));
        }
        return  series;
    }

    private static Circle getCircleCrs(NumberAxis xAxis, NumberAxis yAxis){
        Circle CrsCircle = new Circle(20);
        CrsCircle.setFill(Color.TRANSPARENT);
        CrsCircle.setStroke(Color.RED);
        CrsCircle.setStrokeWidth(2);


        double startX = xAxis.getDisplayPosition(0.5);
        double startY = yAxis.getDisplayPosition(0.3);
        CrsCircle.setTranslateX(startX);
        CrsCircle.setTranslateY(startY);
        return CrsCircle;
    }

    private static List<int[]> getPointInsideCursors(Circle cir, ScatterChart<Number, Number> phasorplot){
        Bounds circleBounds = cir.localToScene(cir.getBoundsInLocal());

        List<int[]> locations = new ArrayList<>();

        System.out.println("points inside the circle:");
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
                double sum_of_squared = dx*dx + dy * dy;

                if (sum_of_squared <= radius * radius){
                    int[] loc = (int[]) data.getExtraValue();
                    locations.add(loc);
                    System.out.printf("G: %.3f, S: %.3f%n", data.getXValue().doubleValue(), data.getYValue().doubleValue());
                }

            }
        }
        return locations;
    }

    private static void highlightImage(Circle circleCrs, List<int[]> coords, ImageDisplay imageDisplay, RandomAccessibleInterval<FloatType> intenistyImage){
        Color strokeColor = (Color) circleCrs.getStroke();

        int red = (int) (strokeColor.getRed() * 255);
        int green = (int) (strokeColor.getGreen() * 255);
        int blue = (int) (strokeColor.getBlue() * 255);
        int alpha = (int) (strokeColor.getOpacity() * 255);
        int highlightColor = ARGBType.rgba(red, green, blue, alpha);


        imageDisplay.setImage(intenistyImage, ImageDisplay.INTENSITY_CONV, (srcRA, lutedRA) -> {
            long x = srcRA.getLongPosition(0);
            long y = srcRA.getLongPosition(1);

            for (int[] coord : coords) {
                if (coord[0] == y && coord[1] == x) {
                    return new ARGBType(highlightColor);
                }
            }
            return lutedRA.get(); // Use default color otherwise
        });
    }

}
