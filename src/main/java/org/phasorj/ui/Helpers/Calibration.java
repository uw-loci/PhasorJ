package org.phasorj.ui.Helpers;

import io.scif.services.DatasetIOService;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.imagej.Dataset;
import org.phasorj.ui.PhasorProcessor;
import org.phasorj.ui.controls.NumericSpinner;
import org.phasorj.ui.controls.NumericTextField;
import org.scijava.Context;


import java.io.File;
import java.io.IOException;

public class Calibration {

    public static void setup(
           Context ctx,
           CheckBox manualCalibrationCheckbox,
           NumericSpinner phase_shift,
           NumericSpinner modulation_factor,
           CheckBox calibCurveCheckbox,
           CheckBox calibImageCheckbox,
           Button importFileButton,
           NumericTextField calibLifetime,
           TextArea importedFilenameDisplay,
           NumericTextField frequency,
           PhasorProcessor processor
    ){
        calibImageCheckbox.setOnAction(e -> {
            boolean isCalibImage = calibImageCheckbox.isSelected();
            if (isCalibImage) {
                manualCalibrationCheckbox.setSelected(false);
                calibCurveCheckbox.setSelected(false);
                modulation_factor.setDisable(true);
                phase_shift.setDisable(true);
                processor.setAutoCalib(true);
            };
            importFileButton.setDisable(!isCalibImage);
            calibLifetime.setDisable(!isCalibImage);
            importedFilenameDisplay.setDisable(!isCalibImage);
            frequency.setDisable(!isCalibImage);
        });

        calibCurveCheckbox.setOnAction(e -> {
            boolean isCalibCurve = calibCurveCheckbox.isSelected();
            if (isCalibCurve) {
                manualCalibrationCheckbox.setSelected(false);
                calibImageCheckbox.setSelected(false);
                modulation_factor.setDisable(true);
                phase_shift.setDisable(true);
                processor.setAutoCalib(true);
            };
            importFileButton.setDisable(!isCalibCurve);
            calibLifetime.setDisable(!isCalibCurve);
            importedFilenameDisplay.setDisable(!isCalibCurve);
            frequency.setDisable(!isCalibCurve);
        });

        manualCalibrationCheckbox.setOnAction(e -> {
            boolean isManual = manualCalibrationCheckbox.isSelected();
            if (isManual) {
                calibCurveCheckbox.setSelected(false);
                calibImageCheckbox.setSelected(false);
                importFileButton.setDisable(true);
                calibLifetime.setDisable(true);
                importedFilenameDisplay.setDisable(true);
                frequency.setDisable(true);

                processor.setAutoCalib(false);
            };
            modulation_factor.setDisable(!isManual);
            phase_shift.setDisable(!isManual);

        });


        importFileButton.setOnAction(e -> {
            Dataset calibDs = Calibration.handleImportCalibrationFile((Stage) importFileButton.getScene().getWindow(),
                    importedFilenameDisplay,
                    ctx);
            if (calibDs != null) processor.setCalibImG(calibDs);
        });

        modulation_factor.setMin(Double.MIN_VALUE);
        modulation_factor.setStepSize(0.1);
        modulation_factor.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            processor.setMod_factor(modulation_factor.getNumberProperty().get());
        });

        phase_shift.setMin(-Math.PI);
        phase_shift.setMax(Math.PI);
        phase_shift.setStepSize(0.1);
        phase_shift.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            processor.setPhase_shift(phase_shift.getNumberProperty().get());
        });

        frequency.setMin(0);

        frequency.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            processor.setFrequency(frequency.getNumberProperty().get());
        });

        calibLifetime.setMin(0);
        calibLifetime.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            processor.setCalibLT(calibLifetime.getNumberProperty().get());
        });



    }

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
//        fileChooser.getExtensionFilters().addAll(
//                new FileChooser.ExtensionFilter("Image", "*.tif")
//        );
        DatasetIOService dss = ctx.getService(DatasetIOService.class);
        File selectedFile = fileChooser.showOpenDialog(stage);
        String filePath = selectedFile.getAbsolutePath();
        if (selectedFile != null) {
            if(dss.canOpen(filePath)) {
                filenameDisplay.setText(selectedFile.getName());
                try {
                    calibDS = dss.open(selectedFile.getAbsolutePath());
                } catch (IOException ex) {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setContentText("Failed to import calibration file: " + ex.getMessage());
                    error.showAndWait();
                }
            } else{
                Alert error = new Alert(Alert.AlertType.INFORMATION);
                error.setContentText("Invalid calibration file");
                error.showAndWait();
            }
        }
        return calibDS;
    }

}
