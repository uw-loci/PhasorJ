package org.phasorj.ui;


import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.phasorj.ui.controllerHelpers.PlotPhasor;
import org.scijava.Context;
import org.scijava.script.DefaultScriptService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PhasorProcessor {
    private final List<DataClass> dataArr;

    //Calibration parameters
    // autoCalib  = false when user select manual calibration
    private boolean autoCalib;

    //parameters for auto calibration
    private Dataset calibDS;
    private double frequency;
    private double calibLT;

    //parameters for manual calibration
    private double mod_factor = 1;
    private double phase_shift = 0;

    private Context ctx;
    private DefaultScriptService scriptService;
    private ScriptLanguage scriptLang;

    private PlotPhasor plotPhasor;



    public PhasorProcessor() {
        dataArr = new ArrayList<>();
        frequency = 0;
        calibLT = 0;
    }

    public void addDS(Dataset ds) throws ExecutionException, InterruptedException, IOException, URISyntaxException {
        ctx = ds.getContext();
        scriptService = ctx.service(DefaultScriptService.class);
        scriptLang = scriptService.getLanguageByName("Python (scyjava)");
        ScriptInfo scriptInfo;

        URL resourceUrl = getClass().getClassLoader().getResource("python_scripts/phasor_fiji.py");
        if (resourceUrl != null) {
            scriptInfo = new ScriptInfo(ctx, resourceUrl, "phasor_fiji.py");
        } else {
            throw new IllegalArgumentException("Could not find phasor_fiji.py in resources");
        }

        scriptInfo.setLanguage(scriptLang);

        Map<String, Object> args = new HashMap<>();
        args.put("img", ds);
        Future<ScriptModule> result = scriptService.run(scriptInfo, true, args);
        Dataset outputDS = (Dataset) result.get().getOutput("output");
        System.out.println(outputDS.numDimensions());
        var img = outputDS.getImgPlus();

        var gData = Views.hyperSlice(img, 2, 1);
        var sData = Views.hyperSlice(img, 2, 2);
        dataArr.add(new DataClass(ds, (RandomAccessibleInterval<FloatType>) gData, (RandomAccessibleInterval<FloatType>) sData, outputDS));
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public Dataset getCalibImg() {
        return calibDS;
    }

    public void setCalibImG(Dataset ds, Runnable onComplete){
        this.calibDS = ds;
        this.autoCalib = true;

        // Create background task for heavy processing
        javafx.concurrent.Task<Void> processingTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateAllPhasors();
                    return null;
                } catch (Exception e) {
                    System.err.println("Error during phasor recalculation: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    if (plotPhasor != null) {
                        plotPhasor.updatePhasorPlot();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    System.out.println("Phasor plot updated successfully");
                });
            }

            @Override
            protected void failed() {
                super.failed();
                Throwable exception = getException();
                javafx.application.Platform.runLater(() -> {
                    System.err.println("Failed to process calibration: " + exception.getMessage());
                    // Show error to user
                    javafx.scene.control.Alert error = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    error.setHeaderText("Processing Error");
                    error.setContentText("Failed to process calibration image: " + exception.getMessage());
                    error.showAndWait();
                });
            }
        };

        // Run processing in background thread
        Thread processingThread = new Thread(processingTask);
        processingThread.setDaemon(true);
        processingThread.start();
    }

    public List<DataClass> getEntries() {
        return new ArrayList<>(dataArr); // return a copy to avoid external mutation
    }

    public DataClass getEntry(int index) {
        return dataArr.get(index);
    }

    public int getEntryCount() {
        return dataArr.size();
    }

    public double getMod_factor() {
        return mod_factor;

    }

    public void setMod_factor(double mod_factor) throws IOException, ExecutionException, InterruptedException {
        this.mod_factor = mod_factor;
        autoCalib = false;
        updateAllPhasors();
        plotPhasor.updatePhasorPlot();

    }

    public double getPhase_shift() {
        return phase_shift;
    }

    public void setPhase_shift(double phase_shift) throws IOException, ExecutionException, InterruptedException {
        this.phase_shift = phase_shift;
        autoCalib = false;
        updateAllPhasors();
        plotPhasor.updatePhasorPlot();

    }

    public double getCalibLT() {
        return calibLT;
    }

    public void setCalibLT(double calibLT) {
        this.calibLT = calibLT;
    }

    public boolean isAutoCalib() {
        return autoCalib;
    }

    public void setAutoCalib(boolean autoCalib) {
        this.autoCalib = autoCalib;
    }

    private void recomputePhasorManual(DataClass entry) throws ExecutionException, InterruptedException, IOException {
        ScriptInfo scriptInfo;
        URL resourceUrl = getClass().getClassLoader().getResource("python_scripts/manualCalib.py");
        if (resourceUrl != null) {
            scriptInfo = new ScriptInfo(ctx, resourceUrl, "phasor_fiji.py");
        } else {
            throw new IllegalArgumentException("Could not find phasor_fiji.py in resources");
        }
        scriptInfo.setLanguage(scriptLang);

        Map<String, Object> args = new HashMap<>();
        args.put("mod_factor", mod_factor);
        args.put("phase_shift", phase_shift);
        args.put("raw_phasor", entry.getRawPhasor());

        Future<ScriptModule> result = scriptService.run(scriptInfo, true, args);
        Dataset outputDS = (Dataset) result.get().getOutput("output");

        var img = outputDS.getImgPlus();
        var gData = Views.hyperSlice(img, 2, 0);
        var sData = Views.hyperSlice(img, 2, 1);

        entry.updatePhasor((RandomAccessibleInterval<FloatType>) gData,
                (RandomAccessibleInterval<FloatType>) sData);
    }

    private void recomputePhasorAuto(DataClass entry) throws ExecutionException, InterruptedException, IOException {
        ScriptInfo scriptInfo;
        URL resourceUrl = getClass().getClassLoader().getResource("python_scripts/autoCalib.py");
        if (resourceUrl != null) {
            scriptInfo = new ScriptInfo(ctx, resourceUrl, "phasor_fiji.py");
        } else {
            throw new IllegalArgumentException("Could not find phasor_fiji.py in resources");
        }        scriptInfo.setLanguage(scriptLang);
        Map<String, Object> args = new HashMap<>();
        args.put("raw_phasor", entry.getRawPhasor());
        args.put("calib_img", calibDS);
        System.out.println(calibDS.numDimensions());

        Future<ScriptModule> result = scriptService.run(scriptInfo, true, args);
        Dataset outputDS = (Dataset) result.get().getOutput("output");

        var img = outputDS.getImgPlus();
        var gData = Views.hyperSlice(img, 2, 0);
        var sData = Views.hyperSlice(img, 2, 1);

        entry.updatePhasor((RandomAccessibleInterval<FloatType>) gData,
                (RandomAccessibleInterval<FloatType>) sData);
    }

    private void updateAllPhasors() throws ExecutionException, InterruptedException, IOException {
        for (DataClass entry : dataArr) {
            if (autoCalib && calibDS != null) {
                recomputePhasorAuto(entry);
            } else {
                recomputePhasorManual(entry);
            }
        }
    }

    public void setPlotPhasor(PlotPhasor plot) {
        this.plotPhasor = plot;
    }

}