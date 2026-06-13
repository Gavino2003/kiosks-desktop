package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.KioskDto;
import pt.ipvc.kiosks.desktop.dto.StoreDto;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class KiosksController implements Initializable {

    @FXML private TableView<KioskDto>          tableKiosks;
    @FXML private TableColumn<KioskDto,String> colName;
    @FXML private TableColumn<KioskDto,String> colSerial;
    @FXML private TableColumn<KioskDto,String> colModel;
    @FXML private TableColumn<KioskDto,String> colStatus;
    @FXML private TableColumn<KioskDto,String> colStore;

    @FXML private Label              lblFormTitle;
    @FXML private TextField          txtName;
    @FXML private TextField          txtSerial;
    @FXML private TextField          txtModel;
    @FXML private ComboBox<StoreDto> cmbStore;
    @FXML private ComboBox<String>   cmbStatus;
    @FXML private Button             btnSave;
    @FXML private Button             btnDelete;
    @FXML private Button             btnCancelEdit;
    @FXML private Label              lblStatus;

    @Autowired private CoreApiClient api;

    private KioskDto        editingKiosk = null;
    private List<StoreDto>  stores;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().kioskName));
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().serialNumber != null ? c.getValue().serialNumber : ""));
        colModel.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().model != null ? c.getValue().model : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colStore.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().storeName != null ? c.getValue().storeName : ""));

        stores = api.getActiveStores();
        cmbStore.setItems(FXCollections.observableArrayList(stores));
        cmbStore.setConverter(new javafx.util.StringConverter<>() {
            public String   toString(StoreDto s) { return s == null ? "" : s.storeName; }
            public StoreDto fromString(String s) { return null; }
        });
        if (!stores.isEmpty()) cmbStore.getSelectionModel().selectFirst();

        cmbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE", "MAINTENANCE"));
        cmbStatus.getSelectionModel().selectFirst();

        tableKiosks.getSelectionModel().selectedItemProperty().addListener((obs, o, kiosk) -> {
            if (kiosk != null) enterEditMode(kiosk);
        });

        enterCreateMode();
        loadKiosks();
    }

    private void enterCreateMode() {
        editingKiosk = null;
        lblFormTitle.setText("New Kiosk");
        btnSave.setText("Create Kiosk");
        btnCancelEdit.setVisible(false); btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);     btnDelete.setManaged(false);
        txtName.clear(); txtSerial.clear(); txtModel.clear();
        cmbStatus.getSelectionModel().selectFirst();
        if (!stores.isEmpty()) cmbStore.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableKiosks.getSelectionModel().clearSelection();
    }

    private void enterEditMode(KioskDto kiosk) {
        editingKiosk = kiosk;
        lblFormTitle.setText("Edit Kiosk");
        btnSave.setText("Save Changes");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(kiosk.kioskName);
        txtSerial.setText(kiosk.serialNumber != null ? kiosk.serialNumber : "");
        txtModel.setText(kiosk.model         != null ? kiosk.model         : "");
        cmbStatus.setValue(kiosk.status);
        stores.stream().filter(s -> s.id.equals(kiosk.storeId)).findFirst()
                .ifPresent(s -> cmbStore.setValue(s));
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadKiosks() {
        tableKiosks.setItems(FXCollections.observableArrayList(api.getKiosks(null)));
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @FXML
    private void handleSave() {
        String   name   = txtName.getText().trim();
        String   serial = txtSerial.getText().trim();
        String   model  = txtModel.getText().trim();
        StoreDto store  = cmbStore.getValue();
        String   status = cmbStatus.getValue();

        if (name.isEmpty() || store == null) {
            showStatus("Name and store are required.", true);
            return;
        }

        try {
            if (editingKiosk == null) {
                api.createKiosk(name,
                        serial.isEmpty() ? null : serial,
                        model.isEmpty()  ? null : model,
                        store.id);
                showStatus("Kiosk created successfully.", false);
            } else {
                api.updateKiosk(editingKiosk.id, name,
                        serial.isEmpty() ? null : serial,
                        model.isEmpty()  ? null : model,
                        store.id, status);
                showStatus("Kiosk updated successfully.", false);
            }
            enterCreateMode();
            loadKiosks();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (editingKiosk == null) { showStatus("Please select a kiosk.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete kiosk \"" + editingKiosk.kioskName + "\"?",
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
    @FXML private void handleRefresh()    { enterCreateMode(); loadKiosks(); }
}
