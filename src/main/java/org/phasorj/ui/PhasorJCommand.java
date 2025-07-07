package org.phasorj.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import net.imagej.display.DatasetView;
import org.scijava.Context;
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

        PluginController controller = fxmlLoader.getController();
        controller.loadDatasetView(datasetView);
        Context ctx = datasetView.context();
        controller.loadCtx(ctx);
        controller.displayOriginalImage();


        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("PhasorJ");
        stage.show();



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
