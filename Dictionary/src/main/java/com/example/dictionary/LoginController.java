package com.example.dictionary; // Use your package

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button signUpButton;
    @FXML private Label messageLabel;
    @FXML private ImageView logoImageView; // Added fx:id for logo

    @FXML
    public void initialize() {
        // Optional: Load logo programmatically if needed or handle missing image
        try {
            // Try loading logo - replace "logo.png" with your actual logo file name
            // Make sure logo.png is in the same resource folder as this controller/FXML
            InputStream logoStream = getClass().getResourceAsStream("logo.png");
            if (logoStream != null) {
                logoImageView.setImage(new Image(logoStream));
            } else {
                System.err.println("Logo image not found in resources.");
                // Optionally hide the ImageView if logo is not found
                logoImageView.setVisible(false);
                logoImageView.setManaged(false); // Don't reserve space for it
            }
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }

        // Clear initial message
        messageLabel.setText("");
    }


    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim(); // Trim input
        String password = passwordField.getText(); // Password shouldn't be trimmed usually

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and Password cannot be empty.", true); // isError = true
            return;
        }

        if (AccountManager.validateLogin(username, password)) {
            showMessage("Login Successful!", false); // isError = false
            switchToMainScene();
        } else {
            showMessage("Invalid username or password.", true);
        }
    }

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and Password required for Sign Up.", true);
            return;
        }

        if (AccountManager.signUp(username, password)) {
            showMessage("Sign Up successful! You can now Sign In.", false);
            passwordField.clear(); // Clear password after successful signup
            usernameField.clear(); // Optional: clear username too
            usernameField.requestFocus(); // Set focus back to username
        } else {
            // More specific error message might be needed depending on AccountManager logic
            showMessage("Username already exists or is invalid.", true);
        }
    }

    // Updated showMessage to use CSS styling potentially
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        // Remove old styles first to avoid conflicts
        messageLabel.getStyleClass().removeAll("success-message", "error-message");

        if (isError) {
            // You can define .error-message in CSS (e.g., -fx-text-fill: red;)
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setTextFill(Color.RED); // Fallback if CSS class not defined/working
        } else {
            // You can define .success-message in CSS (e.g., -fx-text-fill: green;)
            messageLabel.getStyleClass().add("success-message");
            messageLabel.setTextFill(Color.GREEN); // Fallback
        }
    }

    private void switchToMainScene() {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
            Parent root = loader.load();
            Scene mainScene = new Scene(root);
            // Optional: Add main scene CSS if needed
            // mainScene.getStylesheets().add(getClass().getResource("main-styles.css").toExternalForm());
            currentStage.setScene(mainScene);
            currentStage.setTitle("Main Application"); // Update title
            currentStage.centerOnScreen();
            currentStage.setResizable(true); // Allow resizing main window
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Error loading main screen.", true);
        }
    }
}