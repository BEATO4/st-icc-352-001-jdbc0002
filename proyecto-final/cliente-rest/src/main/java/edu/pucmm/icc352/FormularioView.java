package edu.pucmm.icc352;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class FormularioView {
    private static final Logger logger = LoggerFactory.getLogger(FormularioView.class);

    private final RestClient restClient;
    private final String username;
    private final String userId;
    private TableView<FormularioRow> tableView;
    private TextField usuarioIdField;
    private Label statusLabel;
    private String selectedPhotoBase64;

    public FormularioView(RestClient restClient, String username, String userId) {
        this.restClient = restClient;
        this.username = username;
        this.userId = userId;
    }

    public Scene createScene() {
        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(15));
        mainVBox.setStyle("-fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("Gestor de Encuestas - REST");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        HBox usuarioBox = new HBox(10);
        usuarioBox.setPadding(new Insets(10));
        usuarioBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #fff;");

        Label usuarioLabel = new Label("ID de Usuario:");
        usuarioIdField = new TextField(userId);
        usuarioIdField.setPrefWidth(200);

        Button refrescarButton = new Button("Refrescar");
        refrescarButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 15px;");

        Button verTodosButton = new Button("Ver Todos");
        verTodosButton.setStyle(
                "-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #2196F3; -fx-text-fill: white;");

        Button crearButton = new Button("Crear Formulario");
        crearButton.setStyle(
                "-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        usuarioBox.getChildren().addAll(usuarioLabel, usuarioIdField, refrescarButton, verTodosButton, crearButton);

        tableView = createTable();
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #fff;");

        statusLabel = new Label("Listo - Usuario: " + username);
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mainVBox.getChildren().addAll(
                titleLabel,
                usuarioBox,
                new Separator(),
                scrollPane,
                statusLabel);

        refrescarButton.setOnAction(e -> refrescarFormularios());
        verTodosButton.setOnAction(e -> listarTodosFormularios());
        crearButton.setOnAction(e -> abrirDialogoCrearFormulario());

        cargarFormularios();

        return new Scene(mainVBox, 900, 700);
    }

    private TableView<FormularioRow> createTable() {
        TableView<FormularioRow> table = new TableView<>();
        table.setStyle("-fx-font-size: 11px;");

        TableColumn<FormularioRow, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        nombreCol.setPrefWidth(100);

        TableColumn<FormularioRow, String> sectorCol = new TableColumn<>("Sector");
        sectorCol.setCellValueFactory(new PropertyValueFactory<>("sector"));
        sectorCol.setPrefWidth(80);

        TableColumn<FormularioRow, String> nivelCol = new TableColumn<>("Nivel Educativo");
        nivelCol.setCellValueFactory(new PropertyValueFactory<>("nivelEducativo"));
        nivelCol.setPrefWidth(110);

        TableColumn<FormularioRow, String> fotoCol = new TableColumn<>("Foto");
        fotoCol.setCellValueFactory(new PropertyValueFactory<>("tieneFoto"));
        fotoCol.setPrefWidth(60);

        TableColumn<FormularioRow, String> fechaCol = new TableColumn<>("Fecha");
        fechaCol.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));
        fechaCol.setPrefWidth(100);

        TableColumn<FormularioRow, Void> accionesCol = new TableColumn<>("Acciones");
        accionesCol.setPrefWidth(120);
        accionesCol.setCellFactory(col -> new TableCell<FormularioRow, Void>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Eliminar");
            private final HBox hbox = new HBox(5);

            {
                editBtn.setStyle(
                        "-fx-font-size: 10px; -fx-padding: 5px 10px; -fx-background-color: #2196F3; -fx-text-fill: white;");
                deleteBtn.setStyle(
                        "-fx-font-size: 10px; -fx-padding: 5px 10px; -fx-background-color: #f44336; -fx-text-fill: white;");
                hbox.setStyle("-fx-alignment: center;");
                hbox.getChildren().addAll(editBtn, deleteBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    FormularioRow row = getTableRow().getItem();
                    editBtn.setOnAction(e -> abrirDialogoEditarFormulario(row));
                    deleteBtn.setOnAction(e -> confirmarEliminarFormulario(row));
                    setGraphic(hbox);
                }
            }
        });

        table.getColumns().addAll(nombreCol, sectorCol, nivelCol, fotoCol, fechaCol, accionesCol);

        return table;
    }

    private void cargarFormularios() {
        statusLabel.setText("Cargando formularios...");

        new Thread(() -> {
            try {
                List<FormularioDTO> formularios = restClient.listarFormularios(userId);

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    for (FormularioDTO f : formularios) {
                        tableView.getItems().add(new FormularioRow(
                                f.getId(),
                                f.getName(),
                                f.getSector(),
                                f.getEducationalLevel(),
                                f.getPhotoBase64() != null && !f.getPhotoBase64().isEmpty() ? "Si" : "No",
                                formatDateSafe(f.getCreatedAt()),
                                f.getLatitude(),
                                f.getLongitude(),
                                f.getPhotoBase64()));
                    }
                    statusLabel.setText("OK - Se cargaron " + formularios.size() + " formularios");
                });
            } catch (RuntimeException ex) {
                if (isSessionExpired(ex)) {
                    handleSessionExpired();
                } else {
                    Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                }
            }
        }).start();
    }

    private void refrescarFormularios() {
        String usuarioId = usuarioIdField.getText().trim();
        if (usuarioId.isEmpty()) {
            statusLabel.setText("ERROR - Ingrese un ID de usuario");
            return;
        }

        statusLabel.setText("Cargando formularios para: " + usuarioId);

        new Thread(() -> {
            try {
                List<FormularioDTO> formularios = restClient.listarFormularios(usuarioId);

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    for (FormularioDTO f : formularios) {
                        tableView.getItems().add(new FormularioRow(
                                f.getId(),
                                f.getName(),
                                f.getSector(),
                                f.getEducationalLevel(),
                                f.getPhotoBase64() != null && !f.getPhotoBase64().isEmpty() ? "Si" : "No",
                                formatDateSafe(f.getCreatedAt()),
                                f.getLatitude(),
                                f.getLongitude(),
                                f.getPhotoBase64()));
                    }
                    statusLabel.setText("OK - Se cargaron " + formularios.size() + " formularios");
                });
            } catch (RuntimeException ex) {
                if (isSessionExpired(ex)) {
                    handleSessionExpired();
                } else {
                    Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                }
            }
        }).start();
    }

    private void listarTodosFormularios() {
        statusLabel.setText("Cargando TODOS los formularios...");

        new Thread(() -> {
            try {
                List<FormularioDTO> formularios = restClient.listarTodos();

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    for (FormularioDTO f : formularios) {
                        tableView.getItems().add(new FormularioRow(
                                f.getId(),
                                f.getName(),
                                f.getSector(),
                                f.getEducationalLevel(),
                                f.getPhotoBase64() != null && !f.getPhotoBase64().isEmpty() ? "Si" : "No",
                                formatDateSafe(f.getCreatedAt()),
                                f.getLatitude(),
                                f.getLongitude(),
                                f.getPhotoBase64()));
                    }
                    statusLabel.setText("OK - Total: " + formularios.size() + " formularios");
                });
            } catch (RuntimeException ex) {
                if (isSessionExpired(ex)) {
                    handleSessionExpired();
                } else {
                    Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                }
            }
        }).start();
    }

    private void abrirDialogoCrearFormulario() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Crear Nuevo Formulario");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Nombre");

        TextField sectorField = new TextField();
        sectorField.setPromptText("Sector");

        ComboBox<String> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll("BASICO", "MEDIO", "GRADO_UNIVERSITARIO", "POSTGRADO", "DOCTORADO");
        levelCombo.setPromptText("Nivel Educativo");

        TextField latField = new TextField("0");
        TextField lonField = new TextField("0");

        selectedPhotoBase64 = null;
        Label photoLabel = new Label("Sin foto");
        Button selectPhotoBtn = new Button("Seleccionar Foto");
        selectPhotoBtn.setOnAction(e -> seleccionarFoto(photoLabel));

        content.getChildren().addAll(
                new Label("Nombre:"), nameField,
                new Label("Sector:"), sectorField,
                new Label("Nivel Educativo:"), levelCombo,
                new Label("Latitud:"), latField,
                new Label("Longitud:"), lonField,
                selectPhotoBtn, photoLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String name = nameField.getText().trim();
                String sector = sectorField.getText().trim();
                String level = levelCombo.getValue();
                double lat = Double.parseDouble(latField.getText());
                double lon = Double.parseDouble(lonField.getText());

                if (name.isEmpty() || sector.isEmpty() || level == null) {
                    statusLabel.setText("ERROR - Completa todos los campos");
                    return;
                }

                statusLabel.setText("Creando formulario...");

                new Thread(() -> {
                    try {
                        RestClient.CrearFormularioResponse response = restClient.crearFormulario(
                                name, sector, level, lat, lon, selectedPhotoBase64);

                        Platform.runLater(() -> {
                            if (response.success) {
                                statusLabel.setText("OK - Formulario creado exitosamente");
                                cargarFormularios();
                            } else {
                                statusLabel.setText("ERROR - " + response.message);
                            }
                        });
                    } catch (RuntimeException ex) {
                        if (isSessionExpired(ex)) {
                            handleSessionExpired();
                        } else {
                            Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                        }
                    }
                }).start();

            } catch (Exception e) {
                statusLabel.setText("ERROR - Verifica los datos");
            }
        }
    }

    private void seleccionarFoto(Label photoLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Foto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("Todos", "*.*"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                selectedPhotoBase64 = Base64.getEncoder().encodeToString(fileContent);
                photoLabel.setText("Foto: " + file.getName());
            } catch (Exception e) {
                statusLabel.setText("ERROR - No se pudo cargar la foto");
            }
        }
    }

    private String formatDateSafe(String createdAt) {
        if (createdAt == null || createdAt.isEmpty())
            return "";
        return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
    }

    private boolean isSessionExpired(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("SESION_EXPIRADA"))
            return true;
        Throwable cause = ex.getCause();
        return cause != null && cause.getMessage() != null && cause.getMessage().contains("SESION_EXPIRADA");
    }

    private void handleSessionExpired() {
        Platform.runLater(() -> {
            statusLabel.setText("ERROR - Sesion expirada. Volviendo a login...");
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) tableView.getScene().getWindow();
                LoginView loginView = new LoginView(restClient);
                stage.setScene(loginView.createScene(stage));
                stage.setWidth(600);
                stage.setHeight(400);
                stage.setTitle("FieldForm - Cliente REST");
            } catch (Exception e) {
                statusLabel.setText("ERROR - Sesion expirada");
            }
        });
    }

    private void abrirDialogoEditarFormulario(FormularioRow row) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Formulario");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        TextField nameField = new TextField(row.getNombre());
        TextField sectorField = new TextField(row.getSector());

        ComboBox<String> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll("BASICO", "MEDIO", "GRADO_UNIVERSITARIO", "POSTGRADO", "DOCTORADO");
        levelCombo.setValue(normalizarNivelRest(row.getNivelEducativo()));

        TextField latField = new TextField(String.valueOf(row.getLatitud() != null ? row.getLatitud() : 0));
        TextField lonField = new TextField(String.valueOf(row.getLongitud() != null ? row.getLongitud() : 0));

        selectedPhotoBase64 = row.getPhotoBase64();
        Label photoLabel = new Label(row.getTieneFoto().equals("Si") ? "Foto: Si" : "Sin foto");
        Button selectPhotoBtn = new Button("Cambiar Foto");
        selectPhotoBtn.setOnAction(e -> seleccionarFoto(photoLabel));

        content.getChildren().addAll(
                new Label("Nombre:"), nameField,
                new Label("Sector:"), sectorField,
                new Label("Nivel Educativo:"), levelCombo,
                new Label("Latitud:"), latField,
                new Label("Longitud:"), lonField,
                selectPhotoBtn, photoLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String name = nameField.getText().trim();
                String sector = sectorField.getText().trim();
                String level = levelCombo.getValue();
                double lat = Double.parseDouble(latField.getText());
                double lon = Double.parseDouble(lonField.getText());

                if (name.isEmpty() || sector.isEmpty() || level == null) {
                    statusLabel.setText("ERROR - Completa todos los campos");
                    return;
                }

                statusLabel.setText("Actualizando formulario...");

                new Thread(() -> {
                    try {
                        RestClient.CrearFormularioResponse response = restClient.actualizarFormulario(
                                row.getId(), name, sector, level, lat, lon, selectedPhotoBase64);

                        Platform.runLater(() -> {
                            if (response.success) {
                                statusLabel.setText("OK - Formulario actualizado exitosamente");
                                cargarFormularios();
                            } else {
                                statusLabel.setText("ERROR - " + response.message);
                            }
                        });
                    } catch (RuntimeException ex) {
                        if (isSessionExpired(ex)) {
                            handleSessionExpired();
                        } else {
                            Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                        }
                    }
                }).start();

            } catch (Exception e) {
                statusLabel.setText("ERROR - Verifica los datos");
            }
        }
    }

    private String normalizarNivelRest(String raw) {
        if (raw == null)
            return "BASICO";
        return switch (raw.trim().toUpperCase()) {
            case "BASICO", "MEDIO", "GRADO_UNIVERSITARIO" -> raw.trim().toUpperCase();
            default -> raw.trim().toUpperCase();
        };
    }

    private void confirmarEliminarFormulario(FormularioRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminacion");
        alert.setHeaderText("Eliminar Formulario");
        alert.setContentText("Deseas eliminar el formulario: " + row.getNombre() + "?");

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Eliminando formulario...");

            new Thread(() -> {
                try {
                    boolean success = restClient.eliminarFormulario(row.getId());

                    Platform.runLater(() -> {
                        if (success) {
                            statusLabel.setText("OK - Formulario eliminado exitosamente");
                            cargarFormularios();
                        } else {
                            statusLabel.setText("ERROR - No se pudo eliminar el formulario");
                        }
                    });
                } catch (RuntimeException ex) {
                    if (isSessionExpired(ex)) {
                        handleSessionExpired();
                    } else {
                        Platform.runLater(() -> statusLabel.setText("ERROR - " + ex.getMessage()));
                    }
                }
            }).start();
        }
    }

    public static class FormularioRow {
        private String id;
        private String nombre;
        private String sector;
        private String nivelEducativo;
        private String tieneFoto;
        private String fechaCreacion;
        private Double latitud;
        private Double longitud;
        private String photoBase64;

        public FormularioRow(String id, String nombre, String sector, String nivelEducativo, String tieneFoto,
                String fechaCreacion, Double latitud, Double longitud, String photoBase64) {
            this.id = id;
            this.nombre = nombre;
            this.sector = sector;
            this.nivelEducativo = nivelEducativo;
            this.tieneFoto = tieneFoto;
            this.fechaCreacion = fechaCreacion;
            this.latitud = latitud;
            this.longitud = longitud;
            this.photoBase64 = photoBase64;
        }

        public String getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getSector() {
            return sector;
        }

        public String getNivelEducativo() {
            return nivelEducativo;
        }

        public String getTieneFoto() {
            return tieneFoto;
        }

        public String getFechaCreacion() {
            return fechaCreacion;
        }

        public Double getLatitud() {
            return latitud;
        }

        public Double getLongitud() {
            return longitud;
        }

        public String getPhotoBase64() {
            return photoBase64;
        }
    }
}
