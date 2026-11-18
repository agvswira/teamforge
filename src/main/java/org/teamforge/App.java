package org.teamforge;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.teamforge.util.DatabaseConnection;

public class App extends Application {
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("LoginView"), 1100, 720);
        stage.setTitle("TeamForge");
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
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("view/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
