package org.phasorj.ui;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import io.scif.services.DatasetIOService;
import javafx.scene.chart.LineChart;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.controlsfx.control.CheckListView;
import org.phasorj.ui.Helpers.Calibration;
import org.phasorj.ui.Helpers.Export;
import org.phasorj.ui.Helpers.ImageDisplay;
import org.phasorj.ui.Helpers.PlotPhasor;
import org.phasorj.ui.controls.NumericSpinner;
import org.phasorj.ui.controls.NumericTextField;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.scijava.Context;

import static org.phasorj.ui.Helpers.ImageDisplay.processDataset;

public class PluginController {
    private Context ctx;
    private DatasetView datasetView;
    private PhasorProcessor processor;
    private PlotPhasor plt;
    //Image Display
    @FXML private StackPane plotPane;
    LineChart<Number, Number> phasor_plot;
    @FXML private ImageView image_view;
    private ImageDisplay intensityDisplay;
    private Img<FloatType> summedIntensity;
    @FXML private Button addImageButton;
    @FXML private CheckListView<String> dsList;

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
    @FXML private NumericTextField frequency;

    //Export
    @FXML private Button exportPhasorButton;
    @FXML private Button exportImageButton;
    @FXML private TextField exportFolderPath;

    /**
     *  perform setup tasks after the FXML file has been loaded.
     */
    @FXML
    private void initialize() throws IOException {

        /**
         * Set up a PhasorProcess and PlotPhasor instance
         */
        processor = new PhasorProcessor();

        /**
         * Image Display
         */
        intensityDisplay = new ImageDisplay(image_view);
        plt = new PlotPhasor(plotPane, intensityDisplay, null, processor);

        /**
         * Adding Image
         */
        addImageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Add new FLIM image");
            File newFLIM = fileChooser.showOpenDialog(addImageButton.getScene().getWindow());
            String newFLIMPath = newFLIM.getPath();
            DatasetIOService dss = ctx.service(DatasetIOService.class);
            if (dss.canOpen(newFLIMPath)){
                try {
                    Dataset newDS = dss.open(newFLIMPath);
                    processor.addDS(newDS);
                    dsList.getItems().add(newDS.getName());
                    plt.updatePhasorPlot();
                } catch (IOException | ExecutionException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
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

    public void loadDatasetView(DatasetView datasetView) throws ExecutionException, InterruptedException {
        this.datasetView = datasetView;
        processor.addDS(datasetView.getData());
        dsList.getItems().add(datasetView.getData().getName());

        if (plt != null) {
            plt.updatePhasorPlot();
        }
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
    public void plotPhasor() {
        if (plt != null && summedIntensity != null) {
            plt.setIntensityImage(Views.hyperSlice(summedIntensity, 2, 0));
            plt.updatePhasorPlot();
        }
    }

    public void calibration(){
        Calibration.setup(ctx,
                manualCalibrationCheckbox,
                phase_shift,
                modulation_factor,
                calibCurveCheckbox,
                calibImageCheckbox,
                importFileButton,
                calibLifetime,
                importedFilenameDisplay,
                frequency,
                processor);
    }
}