package org.phasorj.ui;

import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import net.imagej.display.ColorTables;
import net.imagej.display.DatasetView;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.phasorj.ui.CalibHelper.CalibImport;
import org.phasorj.ui.controls.NumericSpinner;
import org.phasorj.ui.controls.NumericTextField;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.scijava.Context;

import static org.phasorj.ui.ImageDisplay.processDataset;


public class PluginController {
    private Context ctx;
    private DatasetView datasetView;
    /* *
     * Display Image
     */

    @FXML private ImageView phasor_plot;
    @FXML private ImageView image_view;
    /** The converter for the intensity (left) image */
    private static final RealLUTConverter<FloatType> INTENSITY_CONV =
            new RealLUTConverter<>(0, 0, ColorTables.GRAYS);
    private ImageDisplay intensityDisplay;



    /**
     * Annotates the intensity image and load to the on-screen Image.
     *
     * @param intensity the intensity data
     */

    public void loadAnotatedIntensityImage(final RandomAccessibleInterval<FloatType> intensity) {
        IterableInterval<FloatType> itr = Views.iterable(intensity);

        double max = Double.NEGATIVE_INFINITY;
        for (FloatType val : itr) {
            max = Math.max(max, val.getRealDouble());
        }
        INTENSITY_CONV.setMax(max);

        intensityDisplay.setImage(intensity, INTENSITY_CONV,
                (srcRA, lutedRA) -> lutedRA.get());
    }

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

    @FXML
    private void handleImportCalibrationFile(){

    }

    //Export
    @FXML private Button exportPhasorButton;
    @FXML private Button exportImageButton;



    @FXML
    private void initialize() {

       /* *
        * Display Image
        */
        intensityDisplay = new ImageDisplay(image_view);

        /* *
         * Calibration section
         * */

        // Set initial disabled state
        calibrationTextField.setDisable(true);
        calibrationFileTextArea.setDisable(true);
        importCalibrationFileButton.setDisable(true);

        // Manual calibration selected
        manualCalibrationCheckbox.setOnAction(e -> {
            boolean isManual = manualCalibrationCheckbox.isSelected();
            if (isManual) useCalibrationFileCheckbox.setSelected(false);
            calibrationTextField.setDisable(!isManual);
            calibrationFileTextArea.setDisable(true);
            importCalibrationFileButton.setDisable(true);
        });

        // File calibration selected
        useCalibrationFileCheckbox.setOnAction(e -> {
            boolean isFileBased = useCalibrationFileCheckbox.isSelected();
            if (isFileBased) manualCalibrationCheckbox.setSelected(false);
            calibrationFileTextArea.setDisable(!isFileBased);
            importCalibrationFileButton.setDisable(!isFileBased);
            calibrationTextField.setDisable(true);
        });


        /* *
         *  Export section
         * */

        exportPhasorButton.setOnAction(e -> {
            System.out.println("Exporting Phasor Plot:");
        });

        exportImageButton.setOnAction(e -> {
            System.out.println("Exporting Image:");
        });
    }


    public void loadCtx(Context ctx) {
        this.ctx = ctx;
    }

    public void loadDatasetView(DatasetView datasetView) {
        this.datasetView = datasetView;
    }

    public void displayOriginalImage() {
        RandomAccessibleInterval<FloatType> originalImg = processDataset(datasetView.getData());
        Img<FloatType> summedIntensity = ImageDisplay.sumIntensity(originalImg, 2);
        loadAnotatedIntensityImage(Views.hyperSlice(summedIntensity, 2, 0));

    }
}