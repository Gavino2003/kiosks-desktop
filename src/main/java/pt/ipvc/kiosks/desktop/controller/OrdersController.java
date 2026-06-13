package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.OrderDto;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class OrdersController implements Initializable {

    @FXML private TableView<OrderDto>          tableOrders;
    @FXML private TableColumn<OrderDto,String> colRef;
    @FXML private TableColumn<OrderDto,String> colStatus;
    @FXML private TableColumn<OrderDto,String> colTotal;
    @FXML private TableColumn<OrderDto,String> colStore;
    @FXML private TableColumn<OrderDto,String> colKiosk;
    @FXML private TableColumn<OrderDto,String> colDate;

    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private TextField        txtSearchRef;
    @FXML private ComboBox<String> cmbNewStatus;
    @FXML private HBox             paneGestao;
    @FXML private Label            lblStatus;
    @FXML private Label            lblOrderDetail;

    @Autowired private CoreApiClient api;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colRef.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().reference));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colTotal.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().orderTotal != null ? c.getValue().orderTotal + " €" : ""));
        colStore.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().storeName != null ? c.getValue().storeName : "—"));
        colKiosk.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().kioskName != null ? c.getValue().kioskName : "—"));
        colDate.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().createdAt != null
                        ? c.getValue().createdAt.substring(0, Math.min(16, c.getValue().createdAt.length()))
                        : ""));

        cmbFilterStatus.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbFilterStatus.getSelectionModel().selectFirst();

        cmbNewStatus.setItems(FXCollections.observableArrayList("PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbNewStatus.getSelectionModel().select("READY");

        txtSearchRef.textProperty().addListener((obs, o, n) -> handleSearch());

        tableOrders.getSelectionModel().selectedItemProperty().addListener((obs, o, order) -> {
            if (order != null) {
                String location = (order.storeName != null ? order.storeName : "?")
                        + " > " + (order.kioskName != null ? order.kioskName : "?");
                lblOrderDetail.setText("Reference: " + order.reference
                        + "  |  Total: " + order.orderTotal + " €"
                        + "  |  " + location);
            }
        });

        loadOrders();
    }

    private void loadOrders() {
        String filter  = cmbFilterStatus.getValue();
        String status  = "ALL".equals(filter) ? null : filter;
        Long   storeId = SessionManager.getInstance().isAdmin() ? null
                       : SessionManager.getInstance().getCurrentUser().storeId;
        List<OrderDto> orders = api.getOrders(status, storeId, null);
        tableOrders.setItems(FXCollections.observableArrayList(orders));
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @FXML
    private void handleFilter() {
        loadOrders();
        lblStatus.setText("");
    }

    @FXML
    private void handleSearch() {
        String ref     = txtSearchRef.getText().trim();
        Long   storeId = SessionManager.getInstance().isAdmin() ? null
                       : SessionManager.getInstance().getCurrentUser().storeId;
        String filter  = cmbFilterStatus.getValue();
        String status  = "ALL".equals(filter) ? null : filter;

        List<OrderDto> results = api.getOrders(status, storeId, ref.isEmpty() ? null : ref);
        tableOrders.setItems(FXCollections.observableArrayList(results));

        if (!ref.isEmpty()) {
            showStatus(results.isEmpty()
                    ? "No orders found for: " + ref
                    : results.size() + " order(s) found.", results.isEmpty());
        } else {
            lblStatus.setText("");
        }
    }

    @FXML
    private void handleUpdateStatus() {
        OrderDto selected = tableOrders.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Please select an order.", true); return; }
        String newStatus = cmbNewStatus.getValue();
        try {
            api.updateOrderStatus(selected.id, newStatus);
            showStatus("Status updated to " + newStatus + ".", false);
            loadOrders();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrders();
        lblStatus.setText("");
    }
}
