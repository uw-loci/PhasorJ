package org.phasorj.ui;

import net.imagej.Dataset;

public class DataClass {

    private Dataset originalDS;
    private Dataset calibImG;

    public Dataset getOriginalDS() {
        return originalDS;
    }

    public void setOriginalDS(Dataset ds) {
        this.originalDS = ds;
    }

    public Dataset getCalibImG() {
        return calibImG;
    }

    public void setCalibImG(Dataset ds) {
        this.calibImG = ds;
    }

}
