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
    //initiate a dataclass

    /* *
     * Display Image
     */

    @FXML private StackPane plotPane;
    LineChart<Number, Number> phasor_plot;
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



    @FXML
    private void initialize() throws IOException {

        /**
         * Set up a dataclass instance
         */

        d = new DataClass();


       /* *
        * Display Image
        */
        intensityDisplay = new ImageDisplay(image_view);

        /**
         * Display phasor plot
         */

        NumberAxis xAxis = new NumberAxis(0, 1, 0.1);
        xAxis.setLabel("G");
        xAxis.setAutoRanging(false);
        xAxis.setOpacity(0.5);

        NumberAxis yAxis = new NumberAxis(0, 0.6, 0.1);
        yAxis.setLabel("S");
        yAxis.setAutoRanging(false);
        yAxis.setOpacity(0.5);

        phasor_plot = new LineChart<>(xAxis, yAxis);

        AnchorPane.setTopAnchor(phasor_plot, 0.0);
        AnchorPane.setBottomAnchor(phasor_plot, 0.0);
        AnchorPane.setLeftAnchor(phasor_plot, 0.0);
        AnchorPane.setRightAnchor(phasor_plot, 0.0);

        plotPane.getChildren().add(phasor_plot);


        /* *
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
        d.setOriginalDS(datasetView.getData());
    }

    public void displayOriginalImage() {
        RandomAccessibleInterval<FloatType> originalImg = processDataset(datasetView.getData());
        var summedIntensity = ImageDisplay.sumIntensity(originalImg, 2);

        //no need to hyperslice
        loadAnotatedIntensityImage(Views.hyperSlice(summedIntensity, 2, 0));

    }

    public void plotPhasor() throws IOException {
        plotPhasor.plot(phasor_plot, plotPane, ctx);
    }
}