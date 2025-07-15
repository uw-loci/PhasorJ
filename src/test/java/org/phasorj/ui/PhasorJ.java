package org.phasorj.ui;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.stage.Stage;

import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
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

        // Load UI
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/phasorj/plugin-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();

        PluginController controller = fxmlLoader.getController();
        controller.plotPhasor();
    }


    public static void main(String[] args) {
        launch();
    }
}

