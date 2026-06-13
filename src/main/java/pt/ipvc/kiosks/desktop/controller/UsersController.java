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
import pt.ipvc.kiosks.desktop.dto.UserDto;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class UsersController implements Initializable {

    @FXML private TableView<UserDto>          tableUsers;
    @FXML private TableColumn<UserDto,String> colUsername;
    @FXML private TableColumn<UserDto,String> colEmail;
    @FXML private TableColumn<UserDto,String> colRole;
    @FXML private TableColumn<UserDto,String> colStore;
    @FXML private TableColumn<UserDto,String> colActive;

    @FXML private Label            lblFormTitle;
    @FXML private TextField        txtUsername;
    @FXML private TextField        txtEmail;
    @FXML private PasswordField    txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<StoreDto> cmbStore;
    @FXML private Button           btnSave;
    @FXML private Button           btnDelete;
    @FXML private Button           btnCancelEdit;
    @FXML private Label            lblStatus;

    @Autowired private CoreApiClient api;

    private UserDto editingUser = null;
    private List<StoreDto> storeOptions = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().username));
        colEmail.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().email));
        colRole.setCellValueFactory(c     -> new SimpleStringProperty(
                c.getValue().roleName != null ? c.getValue().roleName : ""));
        colStore.setCellValueFactory(c    -> new SimpleStringProperty(
                c.getValue().storeName != null ? c.getValue().storeName : "— (global)"));
        colActive.setCellValueFactory(c   -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().active) ? "Ativo" : "Inativo"));

        cmbRole.setItems(FXCollections.observableArrayList("ADMIN", "MANAGER", "OPERATOR"));
        cmbRole.getSelectionModel().selectFirst();

        javafx.util.StringConverter<StoreDto> storeConverter = new javafx.util.StringConverter<>() {
            public String   toString(StoreDto s) { return s == null ? "— (sem loja)" : s.storeName; }
            public StoreDto fromString(String s) { return null; }
        };
        cmbStore.setConverter(storeConverter);
        storeOptions = new ArrayList<>();
        storeOptions.add(null);
        storeOptions.addAll(api.getActiveStores());
        cmbStore.setItems(FXCollections.observableArrayList(storeOptions));
        cmbStore.getSelectionModel().selectFirst();

        cmbRole.getSelectionModel().selectedItemProperty().addListener((obs, o, role) -> {
            boolean needsStore = "MANAGER".equals(role) || "OPERATOR".equals(role);
            cmbStore.setDisable(!needsStore);
            if (!needsStore) cmbStore.getSelectionModel().selectFirst();
        });

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, o, user) -> {
            if (user != null) enterEditMode(user);
        });

        enterCreateMode();
        loadUsers();
    }

    private void enterCreateMode() {
        editingUser = null;
        lblFormTitle.setText("Novo Utilizador");
        btnSave.setText("Criar Utilizador");
        btnCancelEdit.setVisible(false); btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);     btnDelete.setManaged(false);
        txtUsername.setDisable(false);
        txtUsername.clear(); txtEmail.clear(); txtPassword.clear();
        cmbRole.getSelectionModel().selectFirst();
        cmbStore.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableUsers.getSelectionModel().clearSelection();
    }

    private void enterEditMode(UserDto user) {
        editingUser = user;
        lblFormTitle.setText("Editar Utilizador");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true); btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);     btnDelete.setManaged(true);
        txtUsername.setText(user.username);
        txtUsername.setDisable(true);
        txtEmail.setText(user.email);
        txtPassword.clear();
        if (user.roleName != null) cmbRole.setValue(user.roleName);
        storeOptions.stream()
                .filter(s -> s != null && s.id.equals(user.storeId))
                .findFirst()
                .ifPresentOrElse(s -> cmbStore.setValue(s),
                        () -> cmbStore.getSelectionModel().selectFirst());
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadUsers() {
        tableUsers.setItems(FXCollections.observableArrayList(api.getUsers()));
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @FXML
    private void handleSave() {
        if (editingUser == null) handleCreate();
        else handleUpdate();
    }

    private void handleCreate() {
        String   username = txtUsername.getText().trim();
        String   email    = txtEmail.getText().trim();
        String   password = txtPassword.getText();
        String   role     = cmbRole.getValue();
        StoreDto store    = cmbStore.getValue();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Preencha todos os campos obrigatórios.", true); return;
        }
        if (("MANAGER".equals(role) || "OPERATOR".equals(role)) && store == null) {
            showStatus("Manager e Operator precisam de uma loja atribuída.", true); return;
        }
        try {
            api.createUser(username, password, email, role, store != null ? store.id : null);
            showStatus("Utilizador criado com sucesso.", false);
            enterCreateMode();
            loadUsers();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    private void handleUpdate() {
        // A API de auth não expõe endpoint de update de utilizador nesta versão;
        // apenas toggle active. Mostra mensagem informativa.
        showStatus("Para editar utilizadores use a API diretamente.", false);
    }

    @FXML
    private void handleToggleActive() {
        UserDto selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione um utilizador.", true); return; }
        try {
            api.toggleUserActive(selected.id);
            showStatus("Estado alterado.", false);
            loadUsers();
        } catch (Exception e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        showStatus("Eliminação de utilizadores não suportada via API nesta versão.", true);
    }

    @FXML private void handleCancelEdit() { enterCreateMode(); }
    @FXML private void handleRefresh()    { enterCreateMode(); loadUsers(); }
}
