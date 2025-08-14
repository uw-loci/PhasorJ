package org.phasorj.ui;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    private double mod_factor;
    private double phase_shift;

    //  private DefaultScriptService scriptService;

    public PhasorProcessor(){
        dataArr = new ArrayList<>();
        frequency = 0;
        calibLT = 0;
    }

    public void addDS(Dataset ds) throws ExecutionException, InterruptedException, IOException {
        Context ctx = ds.getContext();
//        scriptService  = ctx.service(DefaultScriptService.class);

        //getting data using ScriptService
//        Map<String,Object> args = new HashMap<>();
//        args.put("img", ds);
//
//        Future<ScriptModule> result = scriptService.run(new File("C:\\Users\\hdoan3\\code\\PhasorJ\\src\\main\\resources\\phasor_fiji.py"), false, args);
//
//        Dataset outputDS  = (Dataset) result.get().getOutput("output");
//        System.out.println(outputDS.numDimensions());
//        long[] dims = new long[ds.numDimensions()];
//        ds.dimensions(dims);
//        int rows = (int) dims[1];
//        int cols = (int) dims[0];
//
//        float[][] gData = MockData.generateMockData(rows, cols, 0, 1);
//        float[][] sData = MockData.generateMockData(rows, cols, 0, 0.5f);

        String filename = ds.getName();
        filename = filename.substring(0, filename.length() - 4);
        int lastUnderscoreIndex = filename.lastIndexOf('_');

        String prefix = filename.substring(0, lastUnderscoreIndex);
        String suffix = filename.substring(lastUnderscoreIndex + 1);

        String gsFilename  = "C:\\Users\\hdoan3\\code\\PhasorJ\\src\\main\\resources\\Sample_data\\" + prefix + "_gs_" + suffix + "_cal" + ".tif";
        Dataset gsDataset = ctx.service(DatasetIOService.class).open(gsFilename);

        var img = gsDataset.getImgPlus();

        var gData = Views.hyperSlice(img, 2, 1);
        var sData = Views.hyperSlice(img, 2, 0);

        dataArr.add(new DataClass(ds, (RandomAccessibleInterval<FloatType>) gData, (RandomAccessibleInterval<FloatType>) sData));
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

    public void setCalibImG(Dataset ds) {
        this.calibDS = ds;
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

    public void setMod_factor(double mod_factor) {
        this.mod_factor = mod_factor;
    }

    public double getPhase_shift() {
        return phase_shift;
    }

    public void setPhase_shift(double phase_shift) {
        this.phase_shift = phase_shift;
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

//    public void setScriptService(DefaultScriptService scriptService){
//        this.scriptService = scriptService;
//    }
}
