package org.phasorj.ui;

import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.script.DefaultScriptService;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import java.io.File;
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
    private double mod_factor;
    private double phase_shift;

    //  private DefaultScriptService scriptService;

    public PhasorProcessor(){
        dataArr = new ArrayList<>();
        frequency = 0;
        calibLT = 0;
    }

    public void addDS(Dataset ds) throws ExecutionException, InterruptedException {
//        Context ctx = ds.getContext();
//        scriptService  = ctx.service(DefaultScriptService.class);
        long[] dims = new long[ds.numDimensions()];
        ds.dimensions(dims);
        int rows = (int) dims[1];
        int cols = (int) dims[0];

        float[][] gData = MockData.generateMockData(rows, cols, 0, 1);
        float[][] sData = MockData.generateMockData(rows, cols, 0, 0.5f);

        //getting data using ScriptService
//        Map<String,Object> args = new HashMap<>();
//        args.put("img", ds);
//
//        Future<ScriptModule> result = scriptService.run(new File("C:\\Users\\hdoan3\\code\\PhasorJ\\src\\main\\resources\\phasor_fiji.py"), false, args);
//
//        Dataset outputDS  = (Dataset) result.get().getOutput("output");
//        System.out.println(outputDS.numDimensions());
        dataArr.add(new DataClass(ds, gData, sData));
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
