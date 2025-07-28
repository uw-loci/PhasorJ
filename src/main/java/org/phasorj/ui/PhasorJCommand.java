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
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class, menuPath = "Analyze>Lifetime>PhasorJ")
public class PhasorJCommand implements Command {

    @Parameter
    private DatasetView datasetView;



    @Override
    public void run() {

        new JFXPanel();
        Platform.setImplicitExit(false);
        runAndWait(() -> {
            try {
                loadUI();
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to load UI", e);
            }
        });
    }

    /***
     * Load the FXML layout, call main functions in the controller, and show the Stage
     */
        private void loadUI() throws IOException, ExecutionException, InterruptedException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/phasorj/plugin-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        PluginController controller = fxmlLoader.getController();
        controller.loadDatasetView(datasetView);
        Context ctx = datasetView.context();
        controller.loadCtx(ctx);
        controller.displayOriginalImage();
        controller.plotPhasor();


        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("PhasorJ");
        stage.show();



    }


    /**
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     * <p>
     * Credit:
     * <a href="https://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/">Christoph Nahr</a>
     * </p>
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     *
     * Adapted from FLIMJCommand
     */
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
