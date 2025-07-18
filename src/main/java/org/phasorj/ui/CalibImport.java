package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Context;


import java.io.File;
import java.io.IOException;

public class CalibImport {

    /**
     * @param stage
     * @param filenameDisplay
     * @param ctx
     *  Open a new window with the filechooser
     * @return the calibration image
     */
    public static Dataset handleImportCalibrationFile(Stage stage,
                                                   TextArea filenameDisplay,
                                                   Context ctx) {
        Dataset calibDS = null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Calibration File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image", "*.tif")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            filenameDisplay.setText(selectedFile.getName());
            try {
                DatasetIOService dss = ctx.getService(DatasetIOService.class);
                calibDS = dss.open(selectedFile.getAbsolutePath());
            } catch (IOException ex) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Failed to import calibration file: " + ex.getMessage());
                error.showAndWait();
            }
        }
        return calibDS;
    }

}
