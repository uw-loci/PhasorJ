package org.phasorj.ui.controllerHelpers;

import io.scif.services.DatasetIOService;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.util.concurrent.ExecutionException;

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

        // FIXED: Use asynchronous approach
        importFileButton.setOnAction(e -> {
            handleImportCalibrationFile(
                    (Stage) importFileButton.getScene().getWindow(),
                    importedFilenameDisplay,
                    ctx,
                    processor
            );
        });

        modulation_factor.setMin(Double.MIN_VALUE);
        modulation_factor.setStepSize(0.1);
        modulation_factor.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            try {
                processor.setMod_factor(modulation_factor.getNumberProperty().get());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });

        phase_shift.setMin(-Math.PI);
        phase_shift.setMax(Math.PI);
        phase_shift.setStepSize(0.1);
        phase_shift.getNumberProperty().addListener((obs, oldVal, newVal) -> {
            try {
                processor.setPhase_shift(phase_shift.getNumberProperty().get());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
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
     * Asynchronous file loading that won't freeze the UI
     */
    public static void handleImportCalibrationFile(Stage stage,
                                                        TextArea filenameDisplay,
                                                        Context ctx,
                                                        PhasorProcessor processor) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Calibration File");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.tif", "*.tiff", "*.jpg", "*.jpeg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        DatasetIOService dss = ctx.getService(DatasetIOService.class);
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            System.out.println("File selection cancelled by user");
            return;
        }

        String filePath = selectedFile.getAbsolutePath();
        System.out.println("Selected file path: " + filePath);

        if (!dss.canOpen(filePath)) {
            Alert error = new Alert(Alert.AlertType.INFORMATION);
            error.setHeaderText("Invalid File Format");
            error.setContentText("The selected file format is not supported for calibration.");
            error.showAndWait();
            return;
        }

        // Show loading indicator
        filenameDisplay.setText("Loading " + selectedFile.getName() + "...");

        // Task to load the file
        Task<Dataset> loadTask = new Task<Dataset>() {
            @Override
            protected Dataset call() throws Exception {
                System.out.println("Starting to load calibration file...");
                Dataset dataset = dss.open(filePath);
                System.out.println("Successfully loaded calibration file");
                return dataset;
            }

            @Override
            protected void succeeded() {
                Dataset result = getValue();
                if (result != null) {
                    // Update UI to show processing stage
                    Platform.runLater(() -> {
                        filenameDisplay.setText("Processing " + selectedFile.getName() + "...");
                    });

                    // Use the async processor method with callback
                    processor.setCalibImG(result, () -> {
                        // This callback runs when processing is complete
                        Platform.runLater(() -> {
                            filenameDisplay.setText(selectedFile.getName());
                            System.out.println("Calibration file processed successfully: " + selectedFile.getName());
                        });
                    });
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                Platform.runLater(() -> {
                    filenameDisplay.setText("Failed to load file");
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setHeaderText("File Loading Error");
                    error.setContentText("Failed to import calibration file: " +
                            (exception != null ? exception.getMessage() : "Unknown error"));
                    error.showAndWait();
                });
                System.err.println("Failed to load calibration file: " + exception);
            }
        };

        // Run the load task in a background thread
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

}