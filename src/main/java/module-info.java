module org.teamforge {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens org.teamforge.controller to javafx.fxml;
    exports org.teamforge;
}
