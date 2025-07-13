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

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

public class PhasorJ {


    public static void main(String[] args) throws IOException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        final File file = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
        if (file == null) return;
        final Dataset d = ij.scifio().datasetIO().open(file.getAbsolutePath());
        ij.ui().show(d);

        ij.command().run(PhasorJCommand.class, true);

    }
}

