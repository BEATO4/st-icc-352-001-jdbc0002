package edu.pucmm.icc352;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Pantalla de conexión inicial a gRPC
 */
public class MainApp extends Application {

    private GrpcClient grpcClient;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cliente gRPC - Encuestas");

        Label titleLabel = new Label("Conectar a Servidor gRPC");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField("beat04.me");
        hostField.setPrefWidth(300);

        Label portLabel = new Label("Puerto:");
        TextField portField = new TextField("9090");
        portField.setPrefWidth(300);
        CheckBox tlsCheckBox = new CheckBox("Usar TLS (recomendado para 443)");

        Button connectButton = new Button("Conectar");
        connectButton.setPrefWidth(150);
        connectButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        // Layout
        VBox formVBox = new VBox(10);
        formVBox.setPadding(new Insets(20));
        formVBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");

        HBox hostBox = new HBox(10);
        hostBox.getChildren().addAll(hostLabel, hostField);

        HBox portBox = new HBox(10);
        portBox.getChildren().addAll(portLabel, portField);

        formVBox.getChildren().addAll(titleLabel, hostBox, portBox, tlsCheckBox, connectButton, statusLabel);

        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(30));
        mainVBox.setStyle("-fx-background-color: #f5f5f5;");
        mainVBox.getChildren().add(formVBox);

        Scene scene = new Scene(mainVBox, 500, 300);

        connectButton.setOnAction(e -> {
            try {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                boolean useTls = tlsCheckBox.isSelected();

                if (host.isEmpty()) {
                    statusLabel.setText("[ERROR] Host no puede estar vacío");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }

                statusLabel.setText("Conectando...");
                statusLabel.setStyle("-fx-text-fill: #ff9800;");

                grpcClient = new GrpcClient(host, port, useTls);

                if (grpcClient.isConnected()) {
                    statusLabel.setText("[OK] Conectado exitosamente");
                    statusLabel.setStyle("-fx-text-fill: green;");

                    openFormularioView(primaryStage);
                }

            } catch (NumberFormatException ex) {
                statusLabel.setText("[ERROR] Puerto debe ser un número válido");
                statusLabel.setStyle("-fx-text-fill: red;");
            } catch (Exception ex) {
                statusLabel.setText("[ERROR] Error: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openFormularioView(Stage primaryStage) {
        FormularioView formularioView = new FormularioView(grpcClient);
        Scene scene = formularioView.createScene();
        primaryStage.setScene(scene);
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        primaryStage.setTitle("Gestor de Encuestas - gRPC");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

