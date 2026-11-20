package org.teamforge;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.teamforge.util.DatabaseConnection;

public class App extends Application {
    // Dipakai AlertUtil; halaman dan dialog FXML memasang CSS lewat atribut stylesheets
    public static final String STYLESHEET = App.class.getResource("css/teamforge.css").toExternalForm();

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Format tanggal Indonesia (contoh: 03/12/2026) pada DatePicker
        Locale.setDefault(Locale.forLanguageTag("id-ID"));
        scene = new Scene(loadFXML("LoginView"), 1180, 760);
        stage.setTitle("TeamForge");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    // Menutup koneksi database ketika aplikasi ditutup
    @Override
    public void stop() {
        DatabaseConnection.close();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Membuka jendela modal; setup dipakai untuk mengirim data ke controller jendela tersebut
    public static <T> void openDialog(String fxml, String title, Consumer<T> setup) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/" + fxml + ".fxml"));
        Parent root = loader.load();
        T controller = loader.getController();
        setup.accept(controller);

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(scene.getWindow());
        // Dialog tanpa bingkai jendela; sudut membulat diatur oleh CSS kelas "dialog"
        stage.initStyle(StageStyle.TRANSPARENT);
        Scene dialogScene = new Scene(root);
        dialogScene.setFill(Color.TRANSPARENT);
        stage.setScene(dialogScene);

        // Jendela utama diredupkan selama dialog terbuka
        scene.getRoot().setEffect(new ColorAdjust(0, 0, -0.3, 0));
        stage.showAndWait();
        scene.getRoot().setEffect(null);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("view/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
