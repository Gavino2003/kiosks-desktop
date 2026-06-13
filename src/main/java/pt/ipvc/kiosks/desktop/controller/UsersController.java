package pt.ipvc.kiosks.desktop.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.bll.services.AuthService;
import pt.ipvc.kiosks.dal.entities.Role;
import pt.ipvc.kiosks.dal.entities.Store;
import pt.ipvc.kiosks.dal.entities.User;
import pt.ipvc.kiosks.dal.repository.RoleRepository;
import pt.ipvc.kiosks.dal.repository.StoreRepository;
import pt.ipvc.kiosks.dal.repository.UserRepository;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class UsersController implements Initializable {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStore;
    @FXML private TableColumn<User, String> colActive;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<Store> cmbStore;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnCancelEdit;
    @FXML private Label lblStatus;

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private RoleRepository roleRepository;

    private User editingUser = null;

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("error", "success");
        lblStatus.getStyleClass().add(error ? "error" : "success");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRole() != null ? c.getValue().getRole().getRoleName() : ""));
        colStore.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStore() != null ? c.getValue().getStore().getStoreName() : "— (global)"));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getActive() ? "Ativo" : "Inativo"));

        cmbRole.setItems(FXCollections.observableArrayList("ADMIN", "MANAGER", "OPERATOR"));
        cmbRole.getSelectionModel().selectFirst();

        javafx.util.StringConverter<Store> storeConverter = new javafx.util.StringConverter<>() {
            public String toString(Store s) { return s == null ? "— (sem loja)" : s.getStoreName(); }
            public Store fromString(String s) { return null; }
        };
        cmbStore.setConverter(storeConverter);
        List<Store> storeOptions = new ArrayList<>();
        storeOptions.add(null);
        storeOptions.addAll(storeRepository.findByActiveTrue());
        cmbStore.setItems(FXCollections.observableArrayList(storeOptions));
        cmbStore.getSelectionModel().selectFirst();

        cmbRole.getSelectionModel().selectedItemProperty().addListener((obs, o, role) -> {
            boolean needsStore = "MANAGER".equals(role) || "OPERATOR".equals(role);
            cmbStore.setDisable(!needsStore);
            if (!needsStore) cmbStore.getSelectionModel().selectFirst();
        });

        // ao clicar numa linha entra em modo editar
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
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
        btnDelete.setVisible(false);
        btnDelete.setManaged(false);
        txtUsername.setDisable(false);
        txtUsername.clear();
        txtEmail.clear();
        txtPassword.clear();
        cmbRole.getSelectionModel().selectFirst();
        cmbStore.getSelectionModel().selectFirst();
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
        tableUsers.getSelectionModel().clearSelection();
    }

    private void enterEditMode(User user) {
        editingUser = user;
        lblFormTitle.setText("Editar Utilizador");
        btnSave.setText("Guardar Alterações");
        btnCancelEdit.setVisible(true);
        btnCancelEdit.setManaged(true);
        btnDelete.setVisible(true);
        btnDelete.setManaged(true);
        txtUsername.setText(user.getUsername());
        txtUsername.setDisable(true);
        txtEmail.setText(user.getEmail());
        txtPassword.clear();
        if (user.getRole() != null) cmbRole.setValue(user.getRole().getRoleName());
        cmbStore.setValue(user.getStore());
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("error", "success");
    }

    private void loadUsers() {
        tableUsers.setItems(FXCollections.observableArrayList(userRepository.findAll()));
    }

    @FXML
    private void handleSave() {
        if (editingUser == null) handleCreate();
        else handleUpdate();
    }

    private void handleCreate() {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String role     = cmbRole.getValue();
        Store store     = cmbStore.getValue();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Preencha todos os campos obrigatórios.", true);
            return;
        }
        if (("MANAGER".equals(role) || "OPERATOR".equals(role)) && store == null) {
            showStatus("Manager e Operator precisam de uma loja atribuída.", true);
            return;
        }
        try {
            authService.createUser(username, password, email, role,
                    store != null ? store.getIdStore() : null);
            showStatus("Utilizador criado com sucesso.", false);
            enterCreateMode();
            loadUsers();
        } catch (IllegalArgumentException e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    private void handleUpdate() {
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String roleName = cmbRole.getValue();
        Store store     = cmbStore.getValue();

        if (email.isEmpty()) { showStatus("Email obrigatório.", true); return; }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { showStatus("Email inválido.", true); return; }
        if (("MANAGER".equals(roleName) || "OPERATOR".equals(roleName)) && store == null) {
            showStatus("Manager e Operator precisam de uma loja atribuída.", true);
            return;
        }

        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getIdUser().equals(editingUser.getIdUser())) {
                throw new IllegalStateException("Email já está em uso por outro utilizador.");
            }
        });

        try {
            if (!password.isEmpty()) {
                if (password.length() < 8) { showStatus("Password deve ter pelo menos 8 caracteres.", true); return; }
                editingUser.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            }
            editingUser.setEmail(email);
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role não encontrada."));
            editingUser.setRole(role);
            editingUser.setStore(store);
            userRepository.save(editingUser);
            showStatus("Utilizador atualizado com sucesso.", false);
            enterCreateMode();
            loadUsers();
        } catch (IllegalStateException | IllegalArgumentException e) {
            showStatus("Erro: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleToggleActive() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Selecione um utilizador.", true); return; }
        selected.setActive(!selected.getActive());
        userRepository.save(selected);
        showStatus("Estado alterado: " + (selected.getActive() ? "Ativo" : "Inativo"), false);
        loadUsers();
    }

    @FXML
    private void handleDelete() {
        if (editingUser == null) { showStatus("Selecione um utilizador.", true); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar o utilizador \"" + editingUser.getUsername() + "\" definitivamente?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar eliminação");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                userRepository.delete(editingUser);
                enterCreateMode();
                loadUsers();
            }
        });
    }

    @FXML
    private void handleCancelEdit() {
        enterCreateMode();
    }

    @FXML
    private void handleRefresh() {
        enterCreateMode();
        loadUsers();
    }
}
