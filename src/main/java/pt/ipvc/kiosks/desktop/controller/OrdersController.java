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

    @FXML private TableView<OrderDto>         tableOrders;
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
                "TODOS", "PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbFilterStatus.getSelectionModel().selectFirst();

        cmbNewStatus.setItems(FXCollections.observableArrayList("PENDING", "READY", "COLLECTED", "CANCELLED"));
        cmbNewStatus.getSelectionModel().select("READY");

        txtSearchRef.textProperty().addListener((obs, o, n) -> handleSearch());

        tableOrders.getSelectionModel().selectedItemProperty().addListener((obs, o, order) -> {
            if (order != null) {
                String loc = (order.storeName != null ? order.storeName : "?")
                        + " › " + (order.kioskName != null ? order.kioskName : "?");
                lblOrderDetail.setText("Referência: " + order.reference
                        + " | Total: " + order.orderTotal + " €"
                        + " | " + loc);
            }
        });

        loadOrders();
    }

    private Long currentUserStoreId() {
        return SessionManager.getInstance().getCurrentUser().storeId;
    }

    private void loadOrders() {
        String filter  = cmbFilterStatus.getValue();
        Long   storeId = SessionManager.getInstance().isAdmin() ? null : currentUserStoreId();
        List<OrderDto> orders = api.getOrders(filter, storeId, null);
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
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    @FXML
    private void handleSearch() {
        String ref = txtSearchRef.getText().trim();
        Long storeId = SessionManager.getInstance().isAdmin() ? null : currentUserStoreId();
        List<OrderDto> results = api.getOrders(cmbFilterStatus.getValue(), storeId,
                ref.isEmpty() ? null : ref);

        if (results.isEmpty()) {
            showStatus(ref.isEmpty() ? "" : "Nenhuma encomenda encontrada para: " + ref, true);
        } else {
            showStatus(results.size() + " encomenda(s) encontrada(s).", false);
        }
        tableOrders.setItems(FXCollections.observableArrayList(results));
    }

    @FXML
    private void handleUpdateStatus() {
        OrderDto selected = tableOrders.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione uma encomenda.", true); return; }
        String newStatus = cmbNewStatus.getValue();
        try {
            api.updateOrderStatus(selected.id, newStatus);
            showStatus("Estado atualizado para " + newStatus, false);
            loadOrders();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrders();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }
}
