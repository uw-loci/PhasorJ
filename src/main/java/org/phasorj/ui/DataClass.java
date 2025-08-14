package org.phasorj.ui;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public class DataClass {
    private final Dataset dataset;
    private final RandomAccessibleInterval<FloatType> gData;
    private final RandomAccessibleInterval<FloatType> sData;

   public DataClass(Dataset dataset, RandomAccessibleInterval<FloatType> gData, RandomAccessibleInterval<FloatType> sData) {
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
}