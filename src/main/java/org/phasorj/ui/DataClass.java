package org.phasorj.ui;

import net.imagej.Dataset;

public class DataClass {
    private final Dataset dataset;
    private final float[][] gData;
    private final float[][] sData;

   public DataClass(Dataset dataset, float[][] gData, float[][] sData) {
        this.dataset = dataset;
        this.gData = gData;
        this.sData = sData;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public RandomAccessibleInterval<FloatType> getGData() {
        return gData;
    }

    public RandomAccessibleInterval<FloatType> getSData() {
        return sData;
    }

    public Dataset getRawPhasor() {
        return rawPhasor;
    }
    public void updatePhasor(RandomAccessibleInterval<FloatType> g, RandomAccessibleInterval<FloatType> s) {
        this.gData = g;
        this.sData = s;
    }
}