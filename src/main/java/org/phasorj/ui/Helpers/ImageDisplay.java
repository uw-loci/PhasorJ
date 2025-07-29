package org.phasorj.ui.Helpers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ColorTables;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;


/**
 * Helper functions for image displays used in PluginController and PhasorJCommand
 */


public class ImageDisplay
{
    final private ImageView view;

    /** Threshold of pixScale change that necessitates resampling */
    private static final double RELOAD_THR = 1.5;

    //heigh and width of the source image
    private int imgW, imgH;

    /** The actual size (in pixel) on screen of a pixel from the source image */
    private double pixScale, lastReloadPixScale;

    //writeable image for JavaFX display
    private WritableImage writableImage;

    //The intermediate image between ImageJ and JavaFX
    private ARGBScreenImage screenImage;

    //raw image and LUT-colored images
    private RandomAccessibleInterval<FloatType> rawImage;
    private RandomAccessibleInterval<ARGBType> coloredImage;

    public static final RealLUTConverter<FloatType> INTENSITY_CONV =
            new RealLUTConverter<>(0, 0, ColorTables.GRAYS);

    public ImageDisplay(ImageView view) {
        this.view = view;
        ChangeListener<Bounds> bChangeListener = (obs, oldVal, newVal) -> Platform.runLater(() -> {
            fitSize(newVal.getWidth() - 10, newVal.getHeight() - 10);
            reloadImageIfNecessary();
        });
        view.getParent().layoutBoundsProperty().addListener(bChangeListener);
    }


    @FunctionalInterface
    public interface ImageAnnotator {

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
            screenImage = new ARGBScreenImage(imgW, imgH);

        if (src != null && converter != null) {
            coloredImage = Converters.convert(src, converter, new ARGBType());
            // convert and annotate image
            Cursor<ARGBType> dstCsr = screenImage.localizingCursor();
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
            //resize with paren
        Bounds parentBounds = view.getParent().getLayoutBounds();
        fitSize(parentBounds.getWidth() - 10, parentBounds.getHeight() - 10);
//
//
//        writableImage = new WritableImage(imgW, imgH);
//
//        // Copy pixels from ARGBScreenImage to WritableImage
//        int[] pixels = screenImage.getData(); // get pixel array
//        writableImage.getPixelWriter().setPixels(0, 0, imgW, imgH,
//                javafx.scene.image.PixelFormat.getIntArgbPreInstance(),
//                pixels, 0, imgW);
//
//        view.setImage(writableImage);
        // force update as content may change
        lastReloadPixScale = Double.MIN_VALUE;
        reloadImageIfNecessary();
    }



    /**
     * //Adapted from  flimlib/flimj/ParamEstimator - calciMap
     *
     * @param data
     * @param lifetimeAxis
     * @return
     */

    //TODO: This is slow,try scijava ops sum function?
    public static Img<FloatType> sumIntensity(RandomAccessibleInterval<FloatType> data, int lifetimeAxis) {
        // Get dimensions of input data
        int numDims = data.numDimensions();
        long[] dims = new long[numDims];
        data.dimensions(dims);
        dims[lifetimeAxis] = 1;
        Img<FloatType> intensityMap = ArrayImgs.floats(dims);

        RandomAccess<FloatType> inRA = data.randomAccess();
        RandomAccess<FloatType> outRA = intensityMap.randomAccess();

        long[] pos = new long[numDims];
        long lifetimeSize = data.dimension(lifetimeAxis);

        for (long y = 0; y < dims[1]; y++) {
            for (long x = 0; x < dims[0]; x++) {
                pos[0] = x;
                pos[1] = y;
                double sum = 0;
                for (int t = 0; t < lifetimeSize; t++) {
                    pos[lifetimeAxis] = t;
                    inRA.setPosition(pos);
                    sum += inRA.get().getRealDouble();
                }

                pos[lifetimeAxis] = 0;
                outRA.setPosition(pos);
                outRA.get().setReal(sum);
            }
        }

        return intensityMap;
    }


    /**
     * @param dataset
     * Process the input Dataset and return a 3D RAI<FloatType>
     *
     */
    public static <T extends RealType<T>> RandomAccessibleInterval<FloatType> processDataset(Dataset dataset) {
        @SuppressWarnings("unchecked")
        ImgPlus<T> imp = (ImgPlus<T>) dataset.getImgPlus();

        RandomAccessibleInterval<T> img = imp;
        final OpService ops = dataset.context().service(OpService.class);
        final int xAxis = dataset.dimensionIndex(Axes.X);
        final int yAxis = dataset.dimensionIndex(Axes.Y);

        if (xAxis < 0 || yAxis < 0) {
            throw new IllegalArgumentException("Dataset missing X or Y axis");
        }

        int ltAxis = dataset.dimensionIndex(Axes.TIME);
        if (ltAxis < 0) ltAxis = 2;

        Localizable position = new Point(dataset.numDimensions());

        for (int d = imp.numDimensions() - 1; d >= 0; --d) {
            if (d == xAxis || d == yAxis || d == ltAxis) continue;
            img = Views.hyperSlice(img, d, position.getLongPosition(d));
            if (d < ltAxis) ltAxis--;
        }

        if (img.numDimensions() != 3) {
            throw new RuntimeException("Unexpected FLIM image dimensionality: " + img.numDimensions());
        }

        IterableInterval<T> iterable = Views.iterable(img);
        return ops.convert().float32(iterable);

    }

    /**
     * Annotates the intensity image and load to the on-screen Image.
     * @param intensity the intensity data
     */

    public static void loadAnotatedIntensityImage(final RandomAccessibleInterval<FloatType> intensity, ImageDisplay intensityDisplay) {
        IterableInterval<FloatType> itr = Views.iterable(intensity);

        double max = Double.NEGATIVE_INFINITY;
        for (FloatType val : itr) {
            max = Math.max(max, val.getRealDouble());
        }
        ImageDisplay.INTENSITY_CONV.setMax(max);

        intensityDisplay.setImage(intensity, ImageDisplay.INTENSITY_CONV,
                (srcRA, lutedRA) -> lutedRA.get());
    }

    /**
     * Reloads the image only if the ratio between {@link #pixScale} and {@link #lastReloadPixScale}
     * or the inverse is no less than RELOAD_THR because small pixScale steps (e.g. during window
     * resizing) marginally improves appearance.
     */
    private void reloadImageIfNecessary() {
        if (screenImage == null || (Math.max(pixScale / lastReloadPixScale,
                lastReloadPixScale / pixScale) < RELOAD_THR))
            return;
        lastReloadPixScale = pixScale;

        writableImage = new WritableImage((int) view.getFitWidth(), (int) view.getFitHeight());
        view.setImage(writableImage);

        // manual nearest neighbor sampling
        PixelWriter pw = writableImage.getPixelWriter();
        RandomAccess<ARGBType> ra = screenImage.randomAccess();
        long[] position = new long[2];
        final double wiW = writableImage.getWidth();
        final double wiH = writableImage.getHeight();
        for (int x = 0; x < wiW; x++)
            for (int y = 0; y < wiH; y++) {
                position[0] = Math.min((int)(x * imgW / wiW), imgW - 1);
                position[1] = Math.min((int)(y * imgH / wiH), imgH - 1);

                ra.setPosition(position);
                pw.setArgb(x, y, ra.get().get());
            }
    }

}
