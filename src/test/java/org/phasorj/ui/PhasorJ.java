package org.phasorj.ui;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.widget.FileWidget;

public class PhasorJ extends Application {

    //Adapted from  flimlib/flimj/ParamEstimator - calciMap
    public Img<FloatType> sumIntensity(RandomAccessibleInterval<FloatType> data, int lifetimeAxis) {
        // Get dimensions of input data
        int numDims = data.numDimensions();
        long[] dims = new long[numDims];
        data.dimensions(dims);
        dims[lifetimeAxis] = 1;
        Img<FloatType> intensityMap = ArrayImgs.floats(dims);

        RandomAccess<FloatType> inRA = data.randomAccess();
        RandomAccess<FloatType> outRA = intensityMap.randomAccess();

        long[] pos = new long[numDims];
        long lifetimeSize = data.dimension(lifetimeAxis);

        for (long y = 0; y < dims[1]; y++) {
            for (long x = 0; x < dims[0]; x++) {
                pos[0] = x;
                pos[1] = y;
                double sum = 0;
                for (int t = 0; t < lifetimeSize; t++) {
                    pos[lifetimeAxis] = t;
                    inRA.setPosition(pos);
                    sum += inRA.get().getRealDouble();
                }

                pos[lifetimeAxis] = 0;
                outRA.setPosition(pos);
                outRA.get().setReal(sum);
            }
        }

        return intensityMap;
    }

    @Override
    public void start(Stage stage) throws IOException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        final File file = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
        if (file == null) return;

        final Dataset dataset = ij.scifio().datasetIO().open(file.getAbsolutePath());
        ij.ui().show(dataset);

        // Load UI
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/phasorj/plugin-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();

        PluginController controller = fxmlLoader.getController();

        processDatasetGeneric(dataset, controller, ij);
    }

    private <T extends RealType<T>> void processDatasetGeneric(Dataset dataset, PluginController controller, ImageJ ij) {
        @SuppressWarnings("unchecked")
        ImgPlus<T> imp = (ImgPlus<T>) dataset.getImgPlus();

        RandomAccessibleInterval<T> img = imp;
        final OpService ops = dataset.context().service(OpService.class);
        final int xAxis = dataset.dimensionIndex(Axes.X);
        final int yAxis = dataset.dimensionIndex(Axes.Y);

        if (xAxis < 0 || yAxis < 0) {
            throw new IllegalArgumentException("Dataset missing X or Y axis");
        }

        int ltAxis = dataset.dimensionIndex(Axes.TIME);
        if (ltAxis < 0) ltAxis = 2;

        Localizable position = new Point(dataset.numDimensions());

        for (int d = imp.numDimensions() - 1; d >= 0; --d) {
            if (d == xAxis || d == yAxis || d == ltAxis) continue;
            img = Views.hyperSlice(img, d, position.getLongPosition(d));
            if (d < ltAxis) ltAxis--;
        }

        if (img.numDimensions() != 3) {
            throw new RuntimeException("Unexpected FLIM image dimensionality: " + img.numDimensions());
        }

        IterableInterval<T> iterable = Views.iterable(img);
        RandomAccessibleInterval<FloatType> data = ops.convert().float32(iterable);
        ij.ui().show(Views.hyperSlice(data, 2, 0));
        
        Img<FloatType> summedIntensity = sumIntensity(data, 2);
        controller.loadAnotatedIntensityImage(Views.hyperSlice(summedIntensity, 2, 0));

    }

    public static void main(String[] args) {
        launch();
    }
}

