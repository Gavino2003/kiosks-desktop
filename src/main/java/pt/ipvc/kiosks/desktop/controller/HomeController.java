package pt.ipvc.kiosks.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.bll.services.OrderService;
import pt.ipvc.kiosks.dal.repository.KioskRepository;
import pt.ipvc.kiosks.dal.repository.ProductRepository;
import pt.ipvc.kiosks.dal.repository.UserRepository;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class HomeController implements Initializable {

    @FXML private Label lblPendingOrders;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblActiveKiosks;
    @FXML private Label lblTotalUsers;

    @Autowired private OrderService orderService;
    @Autowired private KioskRepository kioskRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblPendingOrders.setText(String.valueOf(orderService.getPendingOrders().size()));
        lblActiveKiosks.setText(String.valueOf(kioskRepository.findByStatus(
                pt.ipvc.kiosks.dal.entities.KioskStatus.ACTIVE).size()));
        lblTotalUsers.setText(String.valueOf(userRepository.findByActiveTrue().size()));
        lblTotalProducts.setText(String.valueOf(
                productRepository.findAll().stream().filter(p -> p.getActive()).count()));
    }
}
