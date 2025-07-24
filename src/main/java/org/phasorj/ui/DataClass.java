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

    public float[][] getGData() {
        return gData;
    }

    public float[][] getSData() {
        return sData;
    }
}