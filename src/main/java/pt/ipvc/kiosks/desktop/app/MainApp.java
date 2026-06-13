package pt.ipvc.kiosks.desktop.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication(scanBasePackages = "pt.ipvc.kiosks.desktop")
public class MainApp extends Application {

    private static ConfigurableApplicationContext springContext;
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        springContext = SpringApplication.run(MainApp.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Kiosks BackOffice");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        showLogin();
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void showLogin() throws IOException {
        loadScene("login.fxml", 480, 360);
        primaryStage.setTitle("Kiosks BackOffice — Login");
    }

    public static void showDashboard() throws IOException {
        loadScene("dashboard.fxml", 1100, 700);
        primaryStage.setTitle("Kiosks BackOffice");
    }

    private static void loadScene(String fxml, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/pt/ipvc/kiosks/desktop/views/" + fxml));
        loader.setControllerFactory(springContext::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                MainApp.class.getResource("/pt/ipvc/kiosks/desktop/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
