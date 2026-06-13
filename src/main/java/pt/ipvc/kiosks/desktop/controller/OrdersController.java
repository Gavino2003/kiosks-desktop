package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.bll.services.OrderService;
import pt.ipvc.kiosks.dal.entities.Order;
import pt.ipvc.kiosks.dal.entities.OrderStatus;
import pt.ipvc.kiosks.dal.repository.OrderRepository;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class OrdersController implements Initializable {

    @FXML private TableView<Order> tableOrders;
    @FXML private TableColumn<Order, String> colRef;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStore;
    @FXML private TableColumn<Order, String> colKiosk;
    @FXML private TableColumn<Order, String> colDate;

    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private TextField txtSearchRef;
    @FXML private ComboBox<String> cmbNewStatus;
    @FXML private HBox paneGestao;
    @FXML private Label lblStatus;
    @FXML private Label lblOrderDetail;

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReference()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrderTotal() + " €"));
        colStore.setCellValueFactory(c -> {
            if (c.getValue().getKiosk() == null) return new SimpleStringProperty("");
            var store = c.getValue().getKiosk().getStore();
            return new SimpleStringProperty(store != null ? store.getStoreName() : "—");
        });
        colKiosk.setCellValueFactory(c -> {
            if (c.getValue().getKiosk() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(c.getValue().getKiosk().getKioskName());
        });
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().toString().substring(0, 16) : ""));

        cmbFilterStatus.setItems(FXCollections.observableArrayList(
                "TODOS", "PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbFilterStatus.getSelectionModel().selectFirst();

        cmbNewStatus.setItems(FXCollections.observableArrayList("PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbNewStatus.getSelectionModel().select("READY");

        txtSearchRef.textProperty().addListener((obs, o, n) -> handleSearch());

        tableOrders.getSelectionModel().selectedItemProperty().addListener((obs, o, order) -> {
            if (order != null) {
                String kioskInfo = "—";
                if (order.getKiosk() != null) {
                    var store = order.getKiosk().getStore();
                    kioskInfo = (store != null ? store.getStoreName() : "?")
                            + " › " + order.getKiosk().getKioskName();
                }
                lblOrderDetail.setText("Referência: " + order.getReference()
                        + " | Total: " + order.getOrderTotal() + " €"
                        + " | " + kioskInfo);
            }
        });

        loadOrders();
    }

    private Long currentUserStoreId() {
        var store = SessionManager.getInstance().getCurrentUser().getStore();
        return store != null ? store.getIdStore() : null;
    }

    private boolean isAdmin() {
        var role = SessionManager.getInstance().getCurrentUser().getRole();
        return role != null && "ADMIN".equals(role.getRoleName());
    }

    private void loadOrders() {
        String filter = cmbFilterStatus.getValue();
        Long storeId = currentUserStoreId();
        List<Order> orders;

        if (isAdmin()) {
            if (filter == null || "TODOS".equals(filter)) {
                orders = orderRepository.findAll();
            } else {
                orders = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.valueOf(filter));
            }
        } else {
            if (filter == null || "TODOS".equals(filter)) {
                orders = storeId != null
                        ? orderRepository.findByKioskStoreIdStoreOrderByCreatedAtDesc(storeId)
                        : List.of();
            } else {
                orders = storeId != null
                        ? orderRepository.findByStatusAndKioskStoreIdStoreOrderByCreatedAtDesc(OrderStatus.valueOf(filter), storeId)
                        : List.of();
            }
        }
        tableOrders.setItems(FXCollections.observableArrayList(orders));
    }

    @FXML
    private void handleFilter() {
        loadOrders();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    @FXML
    private void handleSearch() {
        String ref = txtSearchRef.getText().trim();
        if (ref.isEmpty()) { loadOrders(); return; }
        Long storeId = currentUserStoreId();
        List<Order> results;

        if (isAdmin()) {
            results = orderRepository.findByReferenceContainingIgnoreCaseOrderByCreatedAtDesc(ref);
        } else {
            results = storeId != null
                    ? orderRepository.findByReferenceContainingIgnoreCaseAndKioskStoreIdStoreOrderByCreatedAtDesc(ref, storeId)
                    : List.of();
        }

        if (results.isEmpty()) {
            showStatus("Nenhuma encomenda encontrada para: " + ref, true);
            tableOrders.setItems(FXCollections.observableArrayList());
        } else {
            tableOrders.setItems(FXCollections.observableArrayList(results));
            showStatus(results.size() + " encomenda(s) encontrada(s).", false);
        }
    }

    @FXML
    private void handleUpdateStatus() {
        Order selected = tableOrders.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione uma encomenda.", true); return; }
        String newStatus = cmbNewStatus.getValue();
        try {
            orderService.updateStatus(selected.getIdOrder(), newStatus);
            showStatus("Estado atualizado para " + newStatus, false);
            loadOrders();
        } catch (IllegalArgumentException e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRefresh() { loadOrders(); lblStatus.setText(""); lblStatus.getStyleClass().removeAll("error", "success"); }
}
