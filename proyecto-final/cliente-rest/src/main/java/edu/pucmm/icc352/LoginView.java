package edu.pucmm.icc352;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class LoginView {
    private RestClient restClient;

    public LoginView(RestClient restClient) {
        this.restClient = restClient;
    }

    public Scene createScene(Stage primaryStage) {
        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(40));
        mainVBox.setStyle("-fx-background-color: #f5f5f5;");
        mainVBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("FieldForm - Cliente REST");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(30));
        formBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-color: #fff; -fx-border-width: 1;");
        formBox.setMaxWidth(350);

        Label usernameLabel = new Label("Usuario:");
        usernameLabel.setStyle("-fx-font-weight: bold;");
        TextField usernameField = new TextField("admin2");
        usernameField.setPrefHeight(40);
        usernameField.setStyle("-fx-padding: 10; -fx-font-size: 12;");

        Label passwordLabel = new Label("Contraseña:");
        passwordLabel.setStyle("-fx-font-weight: bold;");
        PasswordField passwordField = new PasswordField();
        passwordField.setText("123456");
        passwordField.setPrefHeight(40);
        passwordField.setStyle("-fx-padding: 10; -fx-font-size: 12;");

        Button loginButton = new Button("Iniciar Sesión");
        loginButton.setPrefHeight(45);
        loginButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-border-radius: 5;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        formBox.getChildren().addAll(
                usernameLabel, usernameField,
                passwordLabel, passwordField,
                loginButton, statusLabel
        );

        mainVBox.getChildren().addAll(titleLabel, formBox);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Por favor ingrese usuario y contraseña");
                statusLabel.setStyle("-fx-text-fill: #f44336;");
                return;
            }

            statusLabel.setText("Autenticando...");
            statusLabel.setStyle("-fx-text-fill: #ff9800;");
            loginButton.setDisable(true);

            new Thread(() -> {
                try {
                    RestClient.LoginResponse response = restClient.login(username, password);

                    javafx.application.Platform.runLater(() -> {
                        if (response.success) {
                            statusLabel.setText("Autenticacion exitosa. Cargando...");
                            statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                            FormularioView formularioView = new FormularioView(restClient, response.username, response.userId);
                            Scene scene = formularioView.createScene();
                            primaryStage.setScene(scene);
                            primaryStage.setWidth(900);
                            primaryStage.setHeight(700);
                            primaryStage.setTitle("FieldForm - Cliente REST");
                        } else {
                            statusLabel.setText("Usuario o contraseña incorrectos");
                            statusLabel.setStyle("-fx-text-fill: #f44336;");
                            loginButton.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Error de conexion");
                        statusLabel.setStyle("-fx-text-fill: #f44336;");
                        loginButton.setDisable(false);
                    });
                }
            }).start();
        });

        return new Scene(mainVBox, 600, 400);
    }
}

