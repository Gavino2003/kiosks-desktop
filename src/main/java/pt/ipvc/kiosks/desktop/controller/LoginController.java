package pt.ipvc.kiosks.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import pt.ipvc.kiosks.desktop.app.MainApp;
import pt.ipvc.kiosks.desktop.client.CoreApiClient;
import pt.ipvc.kiosks.desktop.dto.UserDto;
import pt.ipvc.kiosks.desktop.util.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class LoginController implements Initializable {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @Autowired private CoreApiClient api;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usernameField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() && password.isEmpty()) { showError("Please enter your username and password."); return; }
        if (username.isEmpty()) { showError("Please enter your username."); return; }
        if (password.isEmpty()) { showError("Please enter your password."); return; }

        UserDto user = api.login(username, password);
        if (user == null) {
            showError("Invalid credentials. Please try again.");
            passwordField.requestFocus();
            return;
        }

        SessionManager.getInstance().setCurrentUser(user);
        try {
            MainApp.showDashboard();
        } catch (Exception e) {
            showError("Failed to open dashboard: " + e.getMessage());
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
