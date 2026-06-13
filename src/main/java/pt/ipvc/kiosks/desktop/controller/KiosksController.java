package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.dal.entities.Kiosk;
import pt.ipvc.kiosks.dal.entities.KioskStatus;
import pt.ipvc.kiosks.dal.entities.Store;
import pt.ipvc.kiosks.dal.repository.KioskRepository;
import pt.ipvc.kiosks.dal.repository.StoreRepository;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class KiosksController implements Initializable {

    @FXML private TableView<Kiosk> tableKiosks;
    @FXML private TableColumn<Kiosk, String> colName;
    @FXML private TableColumn<Kiosk, String> colSerial;
    @FXML private TableColumn<Kiosk, String> colModel;
    @FXML private TableColumn<Kiosk, String> colStatus;
    @FXML private TableColumn<Kiosk, String> colStore;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtSerial;
    @FXML private TextField txtModel;
    @FXML private ComboBox<Store> cmbStore;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnCancelEdit;
    @FXML private Label lblStatus;

    @Autowired private KioskRepository kioskRepository;
    @Autowired private StoreRepository storeRepository;

    private Kiosk editingKiosk = null;

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKioskName()));
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSerialNumber() != null ? c.getValue().getSerialNumber() : ""));
        colModel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModel() != null ? c.getValue().getModel() : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStore.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStore() != null ? c.getValue().getStore().getStoreName() : ""));

        List<Store> stores = storeRepository.findByActiveTrue();
        cmbStore.setItems(FXCollections.observableArrayList(stores));
        cmbStore.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Store s) { return s == null ? "" : s.getStoreName(); }
            public Store fromString(String s) { return null; }
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
        lblFormTitle.setText("Novo Quiosque");
        btnSave.setText("Criar Quiosque");
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);
        btnDelete.setManaged(false);
        txtName.clear(); txtSerial.clear(); txtModel.clear();
        cmbStatus.getSelectionModel().selectFirst();
        List<Store> stores = storeRepository.findByActiveTrue();
        if (!stores.isEmpty()) cmbStore.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableKiosks.getSelectionModel().clearSelection();
    }

    private void enterEditMode(Kiosk kiosk) {
        editingKiosk = kiosk;
        lblFormTitle.setText("Editar Quiosque");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true);
        btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);
        btnDelete.setManaged(true);
        txtName.setText(kiosk.getKioskName());
        txtSerial.setText(kiosk.getSerialNumber() != null ? kiosk.getSerialNumber() : "");
        txtModel.setText(kiosk.getModel() != null ? kiosk.getModel() : "");
        cmbStatus.setValue(kiosk.getStatus().name());
        if (kiosk.getStore() != null) cmbStore.setValue(kiosk.getStore());
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadKiosks() {
        tableKiosks.setItems(FXCollections.observableArrayList(kioskRepository.findAll()));
    }

    @FXML
    private void handleSave() {
        String name   = txtName.getText().trim();
        String serial = txtSerial.getText().trim();
        String model  = txtModel.getText().trim();
        Store store   = cmbStore.getValue();
        String status = cmbStatus.getValue();

        if (name.isEmpty() || store == null) {
            showStatus("Nome e loja são obrigatórios.", true);
            return;
        }

        if (editingKiosk == null) {
            Kiosk kiosk = new Kiosk(name, serial.isEmpty() ? null : serial,
                    model.isEmpty() ? null : model, store);
            kioskRepository.save(kiosk);
            showStatus("Quiosque criado com sucesso.", false);
        } else {
            editingKiosk.setKioskName(name);
            editingKiosk.setSerialNumber(serial.isEmpty() ? null : serial);
            editingKiosk.setModel(model.isEmpty() ? null : model);
            editingKiosk.setStore(store);
            editingKiosk.setStatus(KioskStatus.valueOf(status));
            kioskRepository.save(editingKiosk);
            showStatus("Quiosque atualizado com sucesso.", false);
        }
        enterCreateMode();
        loadKiosks();
    }

    @FXML
    private void handleDelete() {
        if (editingKiosk == null) { showStatus("Selecione um quiosque.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar o quiosque \"" + editingKiosk.getKioskName() + "\" definitivamente?\n" +
                "Isto irá apagar também todas as encomendas e sessões associadas.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar eliminação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    kioskRepository.delete(editingKiosk);
                    enterCreateMode();
                    loadKiosks();
                } catch (Exception e) {
                    showStatus("Erro ao apagar: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    private void handleCancelEdit() { enterCreateMode(); }

    @FXML
    private void handleRefresh() { enterCreateMode(); loadKiosks(); }
}
