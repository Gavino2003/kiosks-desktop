package pt.ipvc.kiosks.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.bll.services.AuthService;
import pt.ipvc.kiosks.dal.entities.User;
import pt.ipvc.kiosks.desktop.app.MainApp;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @Autowired
    private AuthService authService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usernameField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() && password.isEmpty()) {
            showError("Preencha o username e a password.");
            return;
        }
        if (username.isEmpty()) {
            showError("Preencha o username.");
            return;
        }
        if (password.isEmpty()) {
            showError("Preencha a password.");
            return;
        }

        User user = authService.login(username, password);
        if (user == null) {
            showError("Credenciais inválidas.");
            passwordField.requestFocus();
            return;
        }

        SessionManager.getInstance().setCurrentUser(user);
        try {
            MainApp.showDashboard();
        } catch (Exception e) {
            showError("Erro ao abrir dashboard: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}
