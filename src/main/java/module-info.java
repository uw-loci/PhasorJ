module org.phasorj {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens org.phasorj to javafx.fxml;
    exports org.phasorj.ui;
    opens org.phasorj.ui to javafx.fxml;
}