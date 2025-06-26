package org.phasorj.ui;

import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Random;

public class ImageDisplay
{
    final private ImageView view;


    //heigh and width of the source image
    private int imgW, imgH;

    //the actual size on screen of a pixel from the source image
    private double pixScale;

    //writeable image for JavaFX display
    private WritableImage writableImage;

    //The intermediate image between ImageJ and JavaFX
    private ARGBScreenImage interImage;

    //raw image and LUT-colored images
    private RandomAccessibleInterval<FloatType> rawImage;
    private RandomAccessibleInterval<ARGBType> coloredImage;


    public ImageDisplay(ImageView view) {
        this.view = view;
    }


    @FunctionalInterface
    public static interface ImageAnnotator {

        /**
         * Recolors the rendered pixel given a pointer to the source float value and the LUT
         * converted color. The value and color can be retrived directly from
         * <code>srcRA.get()</code> and <code>lutedRA.get()</code>. The implementation may refer to
         * the location through e.g. <code>srcRA.getPosition()</code>.
         *
         * @param srcRA   the {@link RandomAccess} pointing at the value being converted
         * @param lutedRA the {@link RandomAccess} pointing at the converted color
         * @return the annotated color
         */
        public ARGBType annotate(RandomAccess<FloatType> srcRA, RandomAccess<ARGBType> lutedRA);
    }


    /**
     * @return The LUT colored image, may be used by another display to composite the image
     * @see #setImage
     */
    public RandomAccessibleInterval<ARGBType> getColorImage() {
        return coloredImage;
    }
    /**
     * @param pixScale The new {@link #pixScale}
     */
    public void setPixScale(final double pixScale) {
        if (!Double.isFinite(pixScale) || Math.abs(this.pixScale - pixScale) < 1e-6
                || pixScale <= 0)
            return;

        this.pixScale = pixScale;
        view.setFitWidth(imgW * pixScale);
        view.setFitHeight(imgH * pixScale);
    }
    /**
     * Make the view fit the size.
     *
     * @param w the desired width
     * @param h the desired height
     */
    private void fitSize(final double w, final double h) {
        double pixScaleX = w / imgW;
        double pixScaleY = h / imgH;
        setPixScale(Math.min(pixScaleX, pixScaleY));
    }
    /**
     * Shows an float-valued image, colored by a converter and possibly annotated by an annotator.
     *
     * @param src       The source image
     * @param converter The LUT converter
     * @param annotator The post-conversion processor functional */
    public void setImage(final RandomAccessibleInterval<FloatType> src,
                         final RealLUTConverter<FloatType> converter,
                         final ImageAnnotator annotator) {
        rawImage = src;

        final int oldW = imgW;
        final int oldH = imgH;

        imgW = (int) src.dimension(0);
        imgH = (int) src.dimension(1);

        // reallocate buffers
        if (oldW != imgW || oldH != imgH)
            interImage = new ARGBScreenImage(imgW, imgH);

        if (src != null && converter != null) {
            coloredImage = Converters.convert(src, converter, new ARGBType());
            // convert and annotate image
            Cursor<ARGBType> dstCsr = interImage.localizingCursor();
            RandomAccess<ARGBType> lutedRA = coloredImage.randomAccess();
            RandomAccess<FloatType> valRA = src.randomAccess();
            while (dstCsr.hasNext()) {
                dstCsr.fwd();
                lutedRA.setPosition(dstCsr);
                valRA.setPosition(dstCsr);

                dstCsr.get().set(annotator != null ? //
                        annotator.annotate(valRA, lutedRA) : lutedRA.get());
            }

        }
        // resize with parent
        Bounds parentBounds = view.getParent().getLayoutBounds();
        fitSize(parentBounds.getWidth() - 10, parentBounds.getHeight() - 10);

        // At the end of your setImage() method:

        if (writableImage == null || writableImage.getWidth() != imgW || writableImage.getHeight() != imgH) {
            writableImage = new WritableImage(imgW, imgH);
        }

        // Copy pixels from ARGBScreenImage to WritableImage
        int[] pixels = interImage.getData(); // get pixel array
        writableImage.getPixelWriter().setPixels(0, 0, imgW, imgH,
                javafx.scene.image.PixelFormat.getIntArgbPreInstance(),
                pixels, 0, imgW);

        // Finally, set the WritableImage to the ImageView
        view.setImage(writableImage);

    }
}
