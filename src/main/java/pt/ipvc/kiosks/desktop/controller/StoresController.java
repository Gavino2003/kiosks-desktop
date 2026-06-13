package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.dal.entities.Store;
import pt.ipvc.kiosks.dal.repository.StoreRepository;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class StoresController implements Initializable {

    @FXML private TableView<Store> tableStores;
    @FXML private TableColumn<Store, String> colName;
    @FXML private TableColumn<Store, String> colType;
    @FXML private TableColumn<Store, String> colCity;
    @FXML private TableColumn<Store, String> colActive;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtAddress;
    @FXML private TextField txtCity;
    @FXML private TextField txtPostal;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnCancelEdit;
    @FXML private Label lblStatus;

    @Autowired private StoreRepository storeRepository;

    private Store editingStore = null;

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStoreName()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStoreType()));
        colCity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCity() != null ? c.getValue().getCity() : ""));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getActive() ? "Ativa" : "Inativa"));

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
        lblFormTitle.setText("Nova Loja");
        btnSave.setText("Criar Loja");
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);
        btnDelete.setManaged(false);
        txtName.clear(); txtAddress.clear(); txtCity.clear(); txtPostal.clear();
        cmbType.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableStores.getSelectionModel().clearSelection();
    }

    private void enterEditMode(Store store) {
        editingStore = store;
        lblFormTitle.setText("Editar Loja");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true);
        btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);
        btnDelete.setManaged(true);
        txtName.setText(store.getStoreName());
        cmbType.setValue(store.getStoreType());
        txtAddress.setText(store.getAddress() != null ? store.getAddress() : "");
        txtCity.setText(store.getCity() != null ? store.getCity() : "");
        txtPostal.setText(store.getPostalCode() != null ? store.getPostalCode() : "");
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadStores() {
        tableStores.setItems(FXCollections.observableArrayList(storeRepository.findAll()));
    }

    @FXML
    private void handleSave() {
        String name    = txtName.getText().trim();
        String type    = cmbType.getValue();
        String address = txtAddress.getText().trim();
        String city    = txtCity.getText().trim();
        String postal  = txtPostal.getText().trim();

        if (name.isEmpty()) { showStatus("O nome da loja é obrigatório.", true); return; }

        if (editingStore == null) {
            storeRepository.save(new Store(name, type, address, city, postal));
            showStatus("Loja criada com sucesso.", false);
        } else {
            editingStore.setStoreName(name);
            editingStore.setStoreType(type);
            editingStore.setAddress(address.isEmpty() ? null : address);
            editingStore.setCity(city.isEmpty() ? null : city);
            editingStore.setPostalCode(postal.isEmpty() ? null : postal);
            storeRepository.save(editingStore);
            showStatus("Loja atualizada com sucesso.", false);
        }
        enterCreateMode();
        loadStores();
    }

    @FXML
    private void handleToggleActive() {
        Store selected = tableStores.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione uma loja.", true); return; }
        selected.setActive(!selected.getActive());
        storeRepository.save(selected);
        showStatus("Estado alterado: " + (selected.getActive() ? "Ativa" : "Inativa"), false);
        loadStores();
    }

    @FXML
    private void handleDelete() {
        if (editingStore == null) { showStatus("Selecione uma loja.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar a loja \"" + editingStore.getStoreName() + "\" definitivamente?\n" +
                "Isto irá apagar também todos os produtos, categorias e quiosques associados.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar eliminação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    storeRepository.delete(editingStore);
                    enterCreateMode();
                    loadStores();
                } catch (Exception e) {
                    showStatus("Erro ao apagar: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    private void handleCancelEdit() { enterCreateMode(); }

    @FXML
    private void handleRefresh() { enterCreateMode(); loadStores(); }
}
