package org.phasorj.ui;


import javafx.scene.chart.*;
import javafx.scene.layout.StackPane;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;
import java.io.IOException;
import java.util.Arrays;


public class plotPhasor {

    public static void plot(StackPane plotPane, Context ctx) throws IOException {
//        //getting mock data
//        String filepath = "C:/Users/hdoan3/code/PhasorJ/src/phasor_components.tif";
//
//        DatasetIOService datasetIOService = ctx.getService(DatasetIOService.class);
//        OpService ops = ctx.getService(OpService.class);
//
//        Dataset ds = datasetIOService.open(filepath);
//        Img<FloatType> img = (Img<FloatType>) ds.getImgPlus();
//
//        int planeDim = 2;
//
//        RandomAccessibleInterval<FloatType> meanImg = Views.hyperSlice(img, planeDim, 0);
//        RandomAccessibleInterval<FloatType> gImg = Views.hyperSlice(img, planeDim, 1);
//        RandomAccessibleInterval<FloatType> sImg = Views.hyperSlice(img, planeDim, 2);
//
//        float[] meanData = convertToFloatArray(meanImg);
//        float[] gData = convertToFloatArray(gImg);
//        float[] sData = convertToFloatArray(sImg);

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


        //Ceating the uniCircle
        LineChart<Number, Number> uniCircle = getLineChart(xAxis, yAxis);
        uniCircle.getStylesheets().addAll(plotPhasor.class.getResource("/Css/plot.css").toExternalForm());

        plotPane.getChildren().addAll(phasor_plot, uniCircle);
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

    public static float[] flatten(float[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        float[] result = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(array[i], 0, result, i * cols, cols);
        }
        return result;
    }

    private static XYChart.Series<Number, Number> getPhasorSeries(float[][] gData, float[][] sData) {

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        float[] flattenG = flatten(gData);
        float[] flattenS = flatten(sData);
        series.setName("Phasor Points");
        int len = flattenS.length;
        for (int i = 0; i < len; i++) {
            float g = flattenG[i];
            float s = flattenS[i];
            if (g != 0 || s != 0) {
                series.getData().add(new XYChart.Data<>(flattenG[i], flattenS[i]));
            }
        }
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


}
