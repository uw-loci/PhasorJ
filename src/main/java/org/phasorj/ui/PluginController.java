package org.phasorj.ui;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.imagej.Dataset;
import net.imagej.display.ColorTables;
import net.imagej.display.DatasetView;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.phasorj.ui.controls.NumericSpinner;
import org.phasorj.ui.controls.NumericTextField;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.scijava.Context;

import java.io.IOException;
import java.util.Stack;

import static org.phasorj.ui.ImageDisplay.processDataset;


public class PluginController {
    private Context ctx;
    private DatasetView datasetView;
    private DataClass d;



    //Image Display
    @FXML private StackPane plotPane;
    LineChart<Number, Number> phasor_plot;
    @FXML private ImageView image_view;
    /** The converter for the intensity image */
    private ImageDisplay intensityDisplay;
    private Img<FloatType> summedIntensity;

    //Parameters
    @FXML private NumericSpinner intensity_up;
    @FXML private NumericSpinner intensity_low;
    @FXML private NumericSpinner median_filter_size;

    //Calibration
    @FXML private CheckBox manualCalibrationCheckbox;
    @FXML private NumericSpinner phase_shift;
    @FXML private NumericSpinner modulation_factor;
    @FXML private CheckBox calibCurveCheckbox;
    @FXML private CheckBox calibImageCheckbox;
    @FXML private Button importFileButton;
    @FXML private NumericTextField calibLifetime;
    @FXML private TextArea importedFilenameDisplay;



    //Export
    @FXML private Button exportPhasorButton;
    @FXML private Button exportImageButton;




    /**
     *  perform setup tasks after the FXML file has been loaded.
     */
    @FXML
    private void initialize() throws IOException {

        /**
         * Set up a dataclass instance
         */
        d = new DataClass();

       /**
        * Image Display
        */
        intensityDisplay = new ImageDisplay(image_view);

        /**
         * Calibration section
         * */

        //Set initial state
        phase_shift.setDisable(true);
        modulation_factor.setDisable(true);
        importFileButton.setDisable(true);
        calibLifetime.setDisable(true);
        importedFilenameDisplay.setDisable(true);

        calibImageCheckbox.setOnAction(e -> {
            boolean isCalibImage = calibImageCheckbox.isSelected();
            if (isCalibImage) {
                manualCalibrationCheckbox.setSelected(false);
                calibCurveCheckbox.setSelected(false);
            };
            importFileButton.setDisable(!isCalibImage);
            calibLifetime.setDisable(!isCalibImage);
            importedFilenameDisplay.setDisable(!isCalibImage);
        });

        importFileButton.setOnAction(e -> {
            Dataset calibDs = CalibImport.handleImportCalibrationFile((Stage) importFileButton.getScene().getWindow(),
                                                                        importedFilenameDisplay,
                                                                        ctx);
            if (calibDs != null) d.setCalibImG(calibDs);
        });

        /**
         *  Export section
         * */

        exportPhasorButton.setOnAction(e -> {
            try {
                Export.exportPane(plotPane, "phasorPlot");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        exportImageButton.setOnAction(e -> {
            try {
                Export.exportImageView(image_view, "image");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void loadCtx(Context ctx) {
        this.ctx = ctx;
    }

    public void loadDatasetView(DatasetView datasetView) {
        this.datasetView = datasetView;
        d.setOriginalDS(datasetView.getData());
    }

    /**
     * Sum the datasetView along the lifetime axis and load the image to the ImageView
     * */
    public void displayOriginalImage() {
        RandomAccessibleInterval<FloatType> originalImg = processDataset(datasetView.getData());
        summedIntensity = ImageDisplay.sumIntensity(originalImg, 2);

        ImageDisplay.loadAnotatedIntensityImage(Views.hyperSlice(summedIntensity, 2, 0), intensityDisplay);

    }

    /**
     * Start the phasor plot and cluster selection actvities
     * @throws IOException
     */
    public void plotPhasor() throws IOException {
        plotPhasor.plot(plotPane, intensityDisplay, Views.hyperSlice(summedIntensity, 2, 0));
    }
}