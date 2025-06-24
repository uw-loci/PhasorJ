package org.phasorj.ui;

import org.phasorj.ui.controls.NumericSpinner;
import org.phasorj.ui.controls.NumericTextField;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;


public class PluginController {
    //Canvas
    @FXML private Canvas phasor_plot;
    @FXML private Canvas image_view;

    //Parameters
    @FXML private NumericSpinner intensity_up;
    @FXML private NumericSpinner intensity_low;
    @FXML private NumericSpinner median_filter_size;

    //Calibration
    @FXML private CheckBox manualCalibrationCheckbox;
    @FXML private CheckBox useCalibrationFileCheckbox;
    @FXML private NumericTextField calibrationTextField;
    @FXML private TextArea calibrationFileTextArea;
    @FXML private Button importCalibrationFileButton;

    //Export
    @FXML private Button exportPhasorButton;
    @FXML private Button exportImageButton;

    @FXML
    private void initialize() {


        //Calibration section

        // Loading file area is disabled until the box is checked
        calibrationTextField.setDisable(true);
        calibrationFileTextArea.setDisable(true);

        manualCalibrationCheckbox.setOnAction(e ->
                calibrationTextField.setDisable(!manualCalibrationCheckbox.isSelected()));

        useCalibrationFileCheckbox.setOnAction(e ->
                calibrationFileTextArea.setDisable(!useCalibrationFileCheckbox.isSelected()));

        // Export section
        exportPhasorButton.setOnAction(e -> {
            System.out.println("Exporting Phasor Plot:");
        });

        exportImageButton.setOnAction(e -> {
            System.out.println("Exporting Image:");
        });
    }
}