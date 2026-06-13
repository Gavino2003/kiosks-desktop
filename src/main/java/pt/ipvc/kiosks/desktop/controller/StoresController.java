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

    @FXML private Label          lblFormTitle;
    @FXML private TextField      txtName;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField      txtAddress;
    @FXML private TextField      txtCity;
    @FXML private TextField      txtPostal;
    @FXML private Button         btnSave;
    @FXML private Button         btnDelete;
    @FXML private Button         btnCancelEdit;
    @FXML private Label          lblStatus;

    @Autowired private CoreApiClient api;

    private StoreDto editingStore = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().storeName));
        colType.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().storeType));
        colCity.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().city != null ? c.getValue().city : ""));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Ativa" : "Inativa"));

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
        lblFormTitle.setText("Editar Loja");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtName.setText(store.storeName);
        cmbType.setValue(store.storeType);
        txtAddress.setText(store.address  != null ? store.address  : "");
        txtCity.setText(store.city        != null ? store.city      : "");
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

        if (name.isEmpty()) { showStatus("O nome da loja é obrigatório.", true); return; }

        try {
            if (editingStore == null) {
                api.createStore(name, type, address.isEmpty() ? null : address,
                        city.isEmpty() ? null : city, postal.isEmpty() ? null : postal);
                showStatus("Loja criada com sucesso.", false);
            } else {
                api.updateStore(editingStore.id, name, type,
                        address.isEmpty() ? null : address,
                        city.isEmpty()    ? null : city,
                        postal.isEmpty()  ? null : postal);
                showStatus("Loja atualizada com sucesso.", false);
            }
            enterCreateMode();
            loadStores();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleToggleActive() {
        StoreDto selected = tableStores.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione uma loja.", true); return; }
        try {
            api.toggleStoreActive(selected.id);
            showStatus("Estado alterado.", false);
            loadStores();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (editingStore == null) { showStatus("Selecione uma loja.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar a loja \"" + editingStore.storeName + "\" definitivamente?\n" +
                "Isto irá apagar também todos os produtos, categorias e quiosques associados.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar eliminação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                showStatus("Eliminação via API não suportada nesta versão.", true);
            }
        });
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }
    @FXML private void handleRefresh()    { enterCreateMode(); loadStores(); }
}
