package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;

import java.io.IOException;

public class plotPhasor {

    public static void plot(ScatterChart<Number, Number> chart, Context ctx) throws IOException {
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

        drawPhasor(chart, gData, sData);
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
        chart.getData().clear(); // clear old data

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Phasor Points");

        int nPoints = Math.min(gData.length, sData.length);
        for (int i = 0; i < nPoints; i++) {
            series.getData().add(new XYChart.Data<>(gData[i], sData[i]));
        }

        chart.getData().add(series);
    }
}
