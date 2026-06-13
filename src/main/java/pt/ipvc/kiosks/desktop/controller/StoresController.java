package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.StoreDto;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class StoresController implements Initializable {

    @FXML private TableView<StoreDto>          tableStores;
    @FXML private TableColumn<StoreDto,String> colName;
    @FXML private TableColumn<StoreDto,String> colType;
    @FXML private TableColumn<StoreDto,String> colCity;
    @FXML private TableColumn<StoreDto,String> colActive;

    @FXML private Label            lblFormTitle;
    @FXML private TextField        txtName;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField        txtAddress;
    @FXML private TextField        txtCity;
    @FXML private TextField        txtPostal;
    @FXML private Button           btnSave;
    @FXML private Button           btnDelete;
    @FXML private Button           btnCancelEdit;
    @FXML private Label            lblStatus;

    @Autowired private CoreApiClient api;

    private StoreDto editingStore = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().storeName));
        colType.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().storeType));
        colCity.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().city != null ? c.getValue().city : ""));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Active" : "Inactive"));

        cmbType.setItems(FXCollections.observableArrayList("EYEWEAR", "MAKEUP", "JEWELLERY"));
        cmbType.getSelectionModel().selectFirst();

        tableStores.getSelectionModel().selectedItemProperty().addListener((obs, o, store) -> {
            if (store != null) enterEditMode(store);
        });

        enterCreateMode();
        loadStores();
    }

    private void enterCreateMode() {
        editingStore = null;
        lblFormTitle.setText("New Store");
        btnSave.setText("Create Store");
        btnCancelEdit.setVisible(false); btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);     btnDelete.setManaged(false);
        txtName.clear(); txtAddress.clear(); txtCity.clear(); txtPostal.clear();
        cmbType.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableStores.getSelectionModel().clearSelection();
    }

    private void enterEditMode(StoreDto store) {
        editingStore = store;
        lblFormTitle.setText("Edit Store");
        btnSave.setText("Save Changes");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(store.storeName);
        cmbType.setValue(store.storeType);
        txtAddress.setText(store.address   != null ? store.address   : "");
        txtCity.setText(store.city         != null ? store.city      : "");
        txtPostal.setText(store.postalCode != null ? store.postalCode : "");
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadStores() {
        tableStores.setItems(FXCollections.observableArrayList(api.getStores()));
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @FXML
    private void handleSave() {
        String name    = txtName.getText().trim();
        String type    = cmbType.getValue();
        String address = txtAddress.getText().trim();
        String city    = txtCity.getText().trim();
        String postal  = txtPostal.getText().trim();

        if (name.isEmpty()) { showStatus("Store name is required.", true); return; }

        try {
            if (editingStore == null) {
                api.createStore(name, type,
                        address.isEmpty() ? null : address,
                        city.isEmpty()    ? null : city,
                        postal.isEmpty()  ? null : postal);
                showStatus("Store created successfully.", false);
            } else {
                api.updateStore(editingStore.id, name, type,
                        address.isEmpty() ? null : address,
                        city.isEmpty()    ? null : city,
                        postal.isEmpty()  ? null : postal);
                showStatus("Store updated successfully.", false);
            }
            enterCreateMode();
            loadStores();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleToggleActive() {
        StoreDto selected = tableStores.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Please select a store.", true); return; }
        try {
            api.toggleStoreActive(selected.id);
            showStatus("Store status updated.", false);
            loadStores();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (editingStore == null) { showStatus("Please select a store.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete store \"" + editingStore.storeName + "\"?\n"
                + "This will also remove all associated products, categories and kiosks.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm deletion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                showStatus("Deletion is not supported in this version.", true);
            }
        });
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }
    @FXML private void handleRefresh()    { enterCreateMode(); loadStores(); }
}
