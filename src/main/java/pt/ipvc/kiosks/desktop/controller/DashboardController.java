package pt.ipvc.kiosks.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.app.MainApp;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private VBox sideMenu;
    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnStores;
    @FXML private Button btnKiosks;
    @FXML private Button btnProducts;
    @FXML private Button btnOrders;
    @FXML private Button btnStock;
    @FXML private Separator sepGestao;
    @FXML private Label lblGestao;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Bem-vindo, " + user.getUsername());
        roleLabel.setText(user.getRole().getRoleName());

        boolean admin = SessionManager.getInstance().isAdmin();
        boolean manager = SessionManager.getInstance().isManager();

        btnUsers.setVisible(admin);
        btnUsers.setManaged(admin);
        btnStores.setVisible(admin);
        btnStores.setManaged(admin);
        btnKiosks.setVisible(admin);
        btnKiosks.setManaged(admin);
        btnProducts.setVisible(manager);
        btnProducts.setManaged(manager);

        // Stock visível para quem não é admin (operator e manager) — leitura do armazém da sua loja
        boolean operator = !manager;
        btnStock.setVisible(operator);
        btnStock.setManaged(operator);

        // esconder secção GESTÃO inteira se não tiver nenhum botão visível (OPERATOR)
        boolean temGestao = manager; // manager inclui admin
        sepGestao.setVisible(temGestao);
        sepGestao.setManaged(temGestao);
        lblGestao.setVisible(temGestao);
        lblGestao.setManaged(temGestao);

        showHome();
    }

    @FXML
    private void showHome() {
        loadView("home.fxml");
    }

    @FXML
    private void showUsers() {
        loadView("users.fxml");
    }

    @FXML
    private void showStores() {
        loadView("stores.fxml");
    }

    @FXML
    private void showKiosks() {
        loadView("kiosks.fxml");
    }

    @FXML
    private void showProducts() {
        loadView("products.fxml");
    }

    @FXML
    private void showOrders() {
        loadView("orders.fxml");
    }

    @FXML
    private void showStock() {
        loadView("stock.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            MainApp.showLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/pt/ipvc/kiosks/desktop/views/" + fxml));
            loader.setControllerFactory(MainApp.getSpringContext()::getBean);
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
