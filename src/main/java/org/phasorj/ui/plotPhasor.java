package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import javafx.application.Platform;

import javafx.geometry.Bounds;

import javafx.scene.Node;
import javafx.scene.Parent;

import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;

import java.io.IOException;
import java.util.Stack;

public class plotPhasor {

    public static void plot(ScatterChart<Number, Number> chart, AnchorPane plotPane, Context ctx) throws IOException {
        String filepath = "C:/Users/hdoan3/code/PhasorJ/src/phasor_components.tif";

        DatasetIOService datasetIOService = ctx.getService(DatasetIOService.class);
        OpService ops = ctx.getService(OpService.class);

        Dataset ds = datasetIOService.open(filepath);
        Img<FloatType> img = (Img<FloatType>) ds.getImgPlus();

        int planeDim = 2;

        RandomAccessibleInterval<FloatType> meanImg = Views.hyperSlice(img, planeDim, 0);
        RandomAccessibleInterval<FloatType> gImg = Views.hyperSlice(img, planeDim, 1);
        RandomAccessibleInterval<FloatType> sImg = Views.hyperSlice(img, planeDim, 2);

        float[] meanData = convertToFloatArray(meanImg);
        float[] gData = convertToFloatArray(gImg);
        float[] sData = convertToFloatArray(sImg);

        //format chart
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.setVerticalZeroLineVisible(false);


        //drawing phasor and the universal circle
        drawPhasor(chart, gData, sData);
        drawCircle(chart);

    }

    private static float[] convertToFloatArray(RandomAccessibleInterval<FloatType> img) {
        long size = img.size();

        float[] data = new float[(int) size];
        int i = 0;
        for (FloatType val : Views.iterable(img)) {
            data[i++] = val.getRealFloat();
        }
        return data;
    }

    private static void drawPhasor(ScatterChart<Number, Number> chart, float[] gData, float[] sData) {

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Phasor Points");

        int nPoints = Math.min(gData.length, sData.length);
        for (int i = 0; i < nPoints; i++) {
            series.getData().add(new XYChart.Data<>(gData[i], sData[i]));
        }

        chart.getData().add(series);
    }

    private static void drawCircle(ScatterChart<Number, Number> chart) {
        Platform.runLater(() -> {
            ValueAxis<Number> xAxis = (ValueAxis<Number>) chart.getXAxis();
            ValueAxis<Number> yAxis = (ValueAxis<Number>) chart.getYAxis();

            //get coordinates in the chart
            double centerX = xAxis.getDisplayPosition(0.5);
            double centerY = yAxis.getDisplayPosition(0.0);
            double radiusX = Math.abs(xAxis.getDisplayPosition(1.0) - centerX);
            double radiusY = Math.abs(yAxis.getDisplayPosition(0.5) - centerY);

            //get  the bounds of chart and the plotting area inside the cart
            Node plotArea = chart.lookup(".chart-plot-background");
            Bounds chartBounds = chart.localToScene(chart.getBoundsInLocal());
            Bounds plotBounds = plotArea.localToScene(plotArea.getBoundsInLocal());

            //calculate offsets
            double offsetX = plotBounds.getMinX() - chartBounds.getMinX();
            double offsetY = plotBounds.getMinY() - chartBounds.getMinY();

            centerX = centerX + offsetX;
            centerY = centerY + offsetY;

            Arc arc = new Arc(centerX, centerY, radiusX, radiusY, 0, 180);
            arc.setType(ArcType.OPEN);
            arc.setStroke(Color.RED);
            arc.setStrokeWidth(2);
            arc.setFill(Color.TRANSPARENT);



            Parent parent = plotArea.getParent();
            if (parent instanceof Pane pane) {
                pane.getChildren().removeIf(n -> n instanceof Arc);
                pane.getChildren().add(arc);
            }
        });
    }




}
