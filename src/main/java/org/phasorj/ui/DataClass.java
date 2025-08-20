package org.phasorj.ui;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public class DataClass {
    private final Dataset dataset;
    private Dataset rawPhasor;
    private RandomAccessibleInterval<FloatType> gData;
    private RandomAccessibleInterval<FloatType> sData;

    public DataClass(Dataset dataset, RandomAccessibleInterval<FloatType> gData, RandomAccessibleInterval<FloatType> sData, Dataset rawPhasor) {
        this.dataset = dataset;
        this.rawPhasor = rawPhasor;
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