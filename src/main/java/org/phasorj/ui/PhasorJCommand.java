package org.phasorj.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.DatasetView;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.phasorj.ui.ImageDisplay.processDataset;

@Plugin(type = Command.class, menuPath = "Analyze>Lifetime>PhasorJ")
public class PhasorJCommand implements Command {

    @Parameter
    private DatasetView datasetView;

    @Override
    public void run() {
        // Force JavaFX runtime initialization
        new JFXPanel();

        // Keep JavaFX alive across calls
        Platform.setImplicitExit(false);

        runAndWait(() -> {
            try {
                loadUI();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load UI", e);
            }
        });
    }

    private void loadUI() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/phasorj/plugin-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("PhasorJ");
        stage.show();

        PluginController controller = fxmlLoader.getController();
        processDataset(datasetView.getData(), controller);
    }

    private void runAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
