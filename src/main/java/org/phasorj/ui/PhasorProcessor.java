package org.phasorj.ui;

import net.imagej.Dataset;

import java.util.ArrayList;
import java.util.List;

public class PhasorProcessor {
    private final List<DataClass> dataArr;
    private Dataset calibDS;
    private int frequency;

    public PhasorProcessor(){
        dataArr = new ArrayList<>();
    }

    public void addDS(Dataset ds) {
        long[] dims = new long[ds.numDimensions()];
        ds.dimensions(dims);
        int rows = (int) dims[1];
        int cols = (int) dims[0];

        float[][] gData = MockData.generateMockData(rows, cols, 0, 1);
        float[][] sData = MockData.generateMockData(rows, cols, 0, 0.5f);

        dataArr.add(new DataClass(ds, gData, sData));
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
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
}
