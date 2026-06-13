package pt.ipvc.kiosks.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.KioskDto;
import pt.ipvc.kiosks.desktop.dto.OrderDto;
import pt.ipvc.kiosks.desktop.dto.ProductDto;
import pt.ipvc.kiosks.desktop.dto.UserDto;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class HomeController implements Initializable {

    @FXML private Label lblPendingOrders;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblActiveKiosks;
    @FXML private Label lblTotalUsers;

    @Autowired private CoreApiClient api;
    @Autowired private DashboardController dashboard;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<OrderDto>   pending  = api.getOrders("PENDING", null, null);
        List<KioskDto>   kiosks   = api.getKiosks(null);
        List<UserDto>    users    = api.getUsers();
        List<ProductDto> products = api.getProducts(null, null, null);

        lblPendingOrders.setText(String.valueOf(pending.size()));
        lblActiveKiosks.setText(String.valueOf(
                kiosks.stream().filter(k -> "ACTIVE".equals(k.status)).count()));
        lblTotalUsers.setText(String.valueOf(users.size()));
        lblTotalProducts.setText(String.valueOf(
                products.stream().filter(p -> Boolean.TRUE.equals(p.active)).count()));
    }

    @FXML private void goOrders()   { dashboard.navigateTo("orders.fxml"); }
    @FXML private void goProducts() { dashboard.navigateTo("products.fxml"); }
    @FXML private void goStores()   { dashboard.navigateTo("stores.fxml"); }
    @FXML private void goKiosks()   { dashboard.navigateTo("kiosks.fxml"); }
}
