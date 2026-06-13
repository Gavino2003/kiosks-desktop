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

    @FXML private Label    welcomeLabel;
    @FXML private Label    roleLabel;
    @FXML private VBox     sideMenu;
    @FXML private StackPane contentArea;

    @FXML private Button    btnDashboard;
    @FXML private Button    btnUsers;
    @FXML private Button    btnStores;
    @FXML private Button    btnKiosks;
    @FXML private Button    btnProducts;
    @FXML private Button    btnOrders;
    @FXML private Button    btnStock;
    @FXML private Separator sepGestao;
    @FXML private Label     lblGestao;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user    = SessionManager.getInstance().getCurrentUser();
        var session = SessionManager.getInstance();

        welcomeLabel.setText("Bem-vindo, " + user.username);
        roleLabel.setText(user.roleName);

        boolean admin   = session.isAdmin();
        boolean manager = session.isManager();

        btnUsers.setVisible(admin);    btnUsers.setManaged(admin);
        btnStores.setVisible(admin);   btnStores.setManaged(admin);
        btnKiosks.setVisible(admin);   btnKiosks.setManaged(admin);
        btnProducts.setVisible(manager); btnProducts.setManaged(manager);

        boolean operator = !manager;
        btnStock.setVisible(operator); btnStock.setManaged(operator);

        sepGestao.setVisible(manager); sepGestao.setManaged(manager);
        lblGestao.setVisible(manager); lblGestao.setManaged(manager);

        showHome();
    }

    @FXML private void showHome()     { loadView("home.fxml"); }
    @FXML private void showUsers()    { loadView("users.fxml"); }
    @FXML private void showStores()   { loadView("stores.fxml"); }
    @FXML private void showKiosks()   { loadView("kiosks.fxml"); }
    @FXML private void showProducts() { loadView("products.fxml"); }
    @FXML private void showOrders()   { loadView("orders.fxml"); }
    @FXML private void showStock()    { loadView("stock.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try { MainApp.showLogin(); } catch (IOException e) { e.printStackTrace(); }
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
