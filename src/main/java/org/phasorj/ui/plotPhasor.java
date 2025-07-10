package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

    public static void plot(Canvas canvas, Context ctx) throws IOException {
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

        drawPhasor(canvas, gData, sData);
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

    private static void drawPhasor(Canvas canvas, float[] gData, float[] sData) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        double margin = 20;
        double radius = Math.min(width, height) / 2 - margin;
        double centerX = width / 2;
        double centerY = height / 2;

        // Draw phasor circle boundary
        gc.setStroke(Color.GRAY);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Plot points
        gc.setFill(Color.BLUE);

        int nPoints = Math.min(gData.length, sData.length);

        for (int i = 0; i < nPoints; i++) {
            double x = centerX + gData[i] * radius;
            double y = centerY - sData[i] * radius; // invert y-axis

            gc.fillOval(x, y, 2, 2);
        }
    }
}
