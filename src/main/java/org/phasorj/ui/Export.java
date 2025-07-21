package org.phasorj.ui;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Export {

    /**
     * Export a
     * @param pane
     * @param filename
     * @throws IOException
     */
    public static void exportPane(Pane pane, String filename) throws IOException {
        WritableImage snapshot = pane.snapshot(new SnapshotParameters(), null);
        //folderpath for now
        String folder_path = "C:/Users/hdoan3/Documents";
        saveImage(snapshot,folder_path, filename);
    }

    /**
     *
     * @param imageView
     * @param filename
     * @throws IOException
     */
    public static void exportImageView(ImageView imageView, String filename) throws IOException {
        WritableImage snapshot = imageView.snapshot(new SnapshotParameters(), null);
        //folderpath for now
        String folder_path = "C:/Users/hdoan3/Documents";
        saveImage(snapshot,folder_path, filename);

    }

    /**
     *
     * @param snapshot
     * @param folder_path
     * @param filename
     * @throws IOException
     */
    private static void saveImage(WritableImage snapshot, String folder_path, String filename) throws IOException {
        BufferedImage bufferedImage = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage image;
        image = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, bufferedImage);
        File file = new File(folder_path + "/phasor.tif");
        ImageIO.write(image, "tif", file);
        }
}
