package org.phasorj.ui.controllerHelpers.plot;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import org.phasorj.ui.DataClass;
import org.phasorj.ui.PhasorProcessor;

/**
 * Manages phasor data points and their spatial coordinates.
 */

public class PhasorDataManager {

    private final PhasorProcessor processor;
    private final List<PhasorPoint> phasorPoints = new ArrayList<>();

    /**
     * Data structure for phasor points with spatial information.
     */
    public static class PhasorPoint {
        public final float g, s;
        public final int imageX, imageY;

        public PhasorPoint(float g, float s, int imageX, int imageY) {
            this.g = g;
            this.s = s;
            this.imageX = imageX;
            this.imageY = imageY;
        }
    }

    public PhasorDataManager(PhasorProcessor processor) {
        this.processor = processor;
    }

    public void updateData() {
        phasorPoints.clear();

        for (DataClass data : processor.getEntries()) {
            RandomAccessibleInterval<FloatType> gData = data.getGData();
            RandomAccessibleInterval<FloatType> sData = data.getSData();

            Cursor<FloatType> gCursor = gData.cursor();
            RandomAccess<FloatType> sAccess = sData.randomAccess();

            while (gCursor.hasNext()) {
                gCursor.fwd();
                sAccess.setPosition(gCursor);

                float g = gCursor.get().getRealFloat();
                float s = sAccess.get().getRealFloat();

                // Get spatial coordinates
                int imageX = gCursor.getIntPosition(0);
                int imageY = gCursor.getIntPosition(1);

                phasorPoints.add(new PhasorPoint(g, s, imageX, imageY));
            }
        }
    }

    public List<PhasorPoint> getPhasorPoints() {
        return new ArrayList<>(phasorPoints);
    }

    /**
     * Find points inside a circular cursor area.
     */
    public List<int[]> getPointsInsideCursor(double cursorDataX, double cursorDataY,
                                             double radiusDataX, double radiusDataY) {
        List<int[]> imageCoords = new ArrayList<>();

        for (PhasorPoint point : phasorPoints) {
            // Calculate distance from cursor center to point in data coordinates
            double deltaX = (point.g - cursorDataX) / radiusDataX;
            double deltaY = (point.s - cursorDataY) / radiusDataY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // Point is inside if distance is less than 1 (normalized radius)
            if (distance <= 1.0) {
                imageCoords.add(new int[]{point.imageX, point.imageY});
            }
        }

        return imageCoords;
    }
}