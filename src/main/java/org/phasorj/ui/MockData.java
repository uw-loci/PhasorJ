package org.phasorj.ui;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class MockData {
    public static float[][] generateMockData(int rows, int cols, float min, float max) {
        Random rand = new Random();
        float[][] array = new float[rows][cols];
        int totalCells = rows * cols;
        int nonZeroCount = totalCells /10 ;
        // Fill first `nonZeroCount` positions with non-zero values
        for (int index = 0; index < nonZeroCount; index++) {
            int i = index / cols;
            int j = index % cols;
            array[i][j] = min + rand.nextFloat() * (max - min);
        }
        return array;
    }
}
