package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import javafx.application.Platform;

import javafx.geometry.Bounds;

import javafx.scene.Node;
import javafx.scene.Parent;

import javafx.scene.chart.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;

import java.io.IOException;
import java.util.Stack;

public class plotPhasor {

    public static void plot(LineChart<Number, Number> chart, StackPane plotPane, Context ctx) throws IOException {
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
        chart.setId("PhasorPlot");

        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.setVerticalZeroLineVisible(false);


        //Scatter chart
        chart.getData().add(getPhasorSeries(gData, sData));

        chart.getData().add(getCircle());

        chart.getStylesheets().addAll(plotPhasor.class.getResource("/Css/plot.css").toExternalForm());

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

    private static XYChart.Series<Number, Number> getPhasorSeries(float[] gData, float[] sData) {

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Phasor Points");

        int nPoints = Math.min(gData.length, sData.length);
        for (int i = 0; i < nPoints; i++) {
            series.getData().add(new XYChart.Data<>(gData[i], sData[i]));
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
