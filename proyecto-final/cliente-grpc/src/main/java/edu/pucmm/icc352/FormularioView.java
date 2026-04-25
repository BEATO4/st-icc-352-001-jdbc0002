package edu.pucmm.icc352;

import edu.pucmm.icc352.grpc.FormularioMessage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class FormularioView {
    private static final Logger logger = LoggerFactory.getLogger(FormularioView.class);

    private final GrpcClient grpcClient;
    private TableView<FormularioRow> tableView;
    private TextField usuarioIdField;
    private Label statusLabel;

    public FormularioView(GrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public Scene createScene() {
        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(15));
        mainVBox.setStyle("-fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("Gestor de Encuestas - gRPC");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        HBox usuarioBox = new HBox(10);
        usuarioBox.setPadding(new Insets(10));
        usuarioBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #fff;");

        Label usuarioLabel = new Label("ID de Usuario:");
        usuarioIdField = new TextField("user1");
        usuarioIdField.setPrefWidth(200);

        Button refrescarButton = new Button("[REFRESH] Refrescar");
        refrescarButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 15px;");

        Button verTodosButton = new Button("[VIEW] Ver Todos");
        verTodosButton.setStyle(
                "-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #2196F3; -fx-text-fill: white;");

        Button crearButton = new Button("[NEW] Crear Formulario");
        crearButton.setStyle(
                "-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        usuarioBox.getChildren().addAll(usuarioLabel, usuarioIdField, refrescarButton, verTodosButton, crearButton);

        tableView = createTable();
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #fff;");

        statusLabel = new Label("Listo");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        mainVBox.getChildren().addAll(
                titleLabel,
                usuarioBox,
                new Separator(),
                scrollPane,
                statusLabel);

        refrescarButton.setOnAction(e -> refrescarFormularios());
        verTodosButton.setOnAction(e -> listarTodosFormularios());
        crearButton.setOnAction(e -> abrirDialogoCrearFormulario());

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
        sectorCol.setPrefWidth(90);

        TableColumn<FormularioRow, String> nivelCol = new TableColumn<>("Nivel Educativo");
        nivelCol.setCellValueFactory(new PropertyValueFactory<>("nivelEducativo"));
        nivelCol.setPrefWidth(120);

        TableColumn<FormularioRow, String> fotoCol = new TableColumn<>("Foto");
        fotoCol.setCellValueFactory(new PropertyValueFactory<>("tieneFoto"));
        fotoCol.setPrefWidth(60);

        TableColumn<FormularioRow, String> fechaCol = new TableColumn<>("Fecha Creacion");
        fechaCol.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));
        fechaCol.setPrefWidth(130);

        TableColumn<FormularioRow, Void> accionesCol = new TableColumn<>("Acciones");
        accionesCol.setPrefWidth(140);
        accionesCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Eliminar");
            private final HBox box = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.setStyle(
                        "-fx-font-size:10px; -fx-padding:4px 8px; -fx-background-color:#2196F3; -fx-text-fill:white;");
                deleteBtn.setStyle(
                        "-fx-font-size:10px; -fx-padding:4px 8px; -fx-background-color:#f44336; -fx-text-fill:white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                FormularioRow row = getTableRow().getItem();
                editBtn.setOnAction(e -> abrirDialogoEditarFormulario(row));
                deleteBtn.setOnAction(e -> confirmarEliminarFormulario(row));
                setGraphic(box);
            }
        });

        table.getColumns().setAll(nombreCol, sectorCol, nivelCol, fotoCol, fechaCol, accionesCol);
        return table;
    }

    private void refrescarFormularios() {
        new Thread(() -> {
            try {
                String usuarioId = usuarioIdField.getText().trim();
                System.out.println("[SEARCH] Buscando formularios para usuarioId: [" + usuarioId + "]");

                if (usuarioId.isEmpty()) {
                    actualizarStatus("[ERROR] Ingrese un ID de usuario");
                    return;
                }

                actualizarStatus("Cargando formularios...");

                List<FormularioMessage> formularios = grpcClient.listarFormularios(usuarioId);
                System.out.println("[OK] Respuesta recibida: " + formularios.size() + " formularios");

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    for (FormularioMessage fm : formularios) {
                        System.out.println("  - " + fm.getName() + " (userId: " + fm.getUserId() + ")");
                        tableView.getItems().add(new FormularioRow(
                                fm.getId(),
                                fm.getName(),
                                fm.getSector(),
                                fm.getEducationalLevel(),
                                fm.getPhotoBase64().isEmpty() ? "No" : "Si",
                                fm.getCreatedAt(),
                                fm.getLatitude(),
                                fm.getLongitude(),
                                fm.getPhotoBase64()));
                    }

                    actualizarStatus("[OK] Se cargaron " + formularios.size() + " formularios");
                });

            } catch (Exception ex) {
                System.out.println("[ERROR] Error: " + ex.getMessage());
                actualizarStatus("[ERROR] Error: " + ex.getMessage());
                logger.error("Error al refrescar formularios", ex);
            }
        }).start();
    }

    private void listarTodosFormularios() {
        new Thread(() -> {
            try {
                System.out.println("[VIEW] Buscando TODOS los formularios...");
                actualizarStatus("Cargando todos los formularios...");

                List<FormularioMessage> formularios = grpcClient.listarTodos();
                System.out.println("[OK] Respuesta recibida: " + formularios.size() + " formularios");

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    for (FormularioMessage fm : formularios) {
                        System.out.println("  - " + fm.getName() + " (userId: " + fm.getUserId() + ", usuario: "
                                + fm.getUsername() + ")");
                        tableView.getItems().add(new FormularioRow(
                                fm.getId(),
                                fm.getName(),
                                fm.getSector(),
                                fm.getEducationalLevel(),
                                fm.getPhotoBase64().isEmpty() ? "No" : "Si",
                                fm.getCreatedAt(),
                                fm.getLatitude(),
                                fm.getLongitude(),
                                fm.getPhotoBase64()));
                    }

                    actualizarStatus("[OK] Se cargaron " + formularios.size() + " formularios de TODOS los usuarios");
                });

            } catch (Exception ex) {
                System.out.println("[ERROR] Error: " + ex.getMessage());
                actualizarStatus("[ERROR] Error: " + ex.getMessage());
                logger.error("Error al listar todos los formularios", ex);
            }
        }).start();
    }

    private void abrirDialogoCrearFormulario() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Crear Nuevo Formulario");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(20));
        vBox.setStyle("-fx-background-color: #f9f9f9;");

        Label nombreLabel = new Label("Nombre:");
        TextField nombreField = new TextField();
        nombreField.setPromptText("Ej: Encuesta COVID-19");
        nombreField.setPrefWidth(350);

        Label sectorLabel = new Label("Sector:");
        TextField sectorField = new TextField();
        sectorField.setPromptText("Ej: Villa Mella");
        sectorField.setPrefWidth(350);

        Label nivelLabel = new Label("Nivel Educativo:");
        ComboBox<String> nivelCombo = new ComboBox<>();
        nivelCombo.getItems().addAll(
                "BASICO",
                "MEDIO",
                "GRADO_UNIVERSITARIO",
                "POSTGRADO",
                "DOCTORADO");
        nivelCombo.setPrefWidth(350);

        HBox coordBox = new HBox(10);
        Label latLabel = new Label("Latitud:");
        TextField latField = new TextField("0.0");
        latField.setPrefWidth(150);
        Label longLabel = new Label("Longitud:");
        TextField longField = new TextField("0.0");
        longField.setPrefWidth(150);
        coordBox.getChildren().addAll(latLabel, latField, longLabel, longField);

        Label imagenLabel = new Label("Imagen:");
        Button seleccionarImagenButton = new Button("[FILE] Seleccionar Imagen");
        seleccionarImagenButton.setStyle("-fx-padding: 8px 15px;");

        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);

        final String[] fotoBase64 = { "" };

        seleccionarImagenButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Imagen");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));

            File file = fileChooser.showOpenDialog(dialogStage);
            if (file != null) {
                try {
                    byte[] imageBytes = Files.readAllBytes(file.toPath());
                    fotoBase64[0] = Base64.getEncoder().encodeToString(imageBytes);

                    Image image = new Image(file.toURI().toString());
                    imagePreview.setImage(image);

                    actualizarStatus("[OK] Imagen cargada: " + file.getName());
                } catch (Exception ex) {
                    actualizarStatus("Error al cargar imagen: " + ex.getMessage());
                    logger.error("Error al cargar imagen", ex);
                }
            }
        });

        Button guardarButton = new Button("Guardar");
        guardarButton.setStyle(
                "-fx-padding: 10px 20px; -fx-font-size: 12px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button cancelarButton = new Button("Cancelar");
        cancelarButton.setStyle("-fx-padding: 10px 20px; -fx-font-size: 12px;");

        HBox botonesBox = new HBox(10);
        botonesBox.getChildren().addAll(guardarButton, cancelarButton);

        vBox.getChildren().addAll(
                nombreLabel, nombreField,
                sectorLabel, sectorField,
                nivelLabel, nivelCombo,
                coordBox,
                imagenLabel, seleccionarImagenButton, imagePreview,
                botonesBox);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 450, 650);
        dialogStage.setScene(scene);

        guardarButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String nombre = nombreField.getText().trim();
                    String sector = sectorField.getText().trim();
                    String nivel = nivelCombo.getValue();
                    String usuarioId = usuarioIdField.getText().trim();

                    if (nombre.isEmpty() || sector.isEmpty() || nivel == null) {
                        actualizarStatus("Complete todos los campos requeridos");
                        return;
                    }

                    double lat = Double.parseDouble(latField.getText());
                    double lon = Double.parseDouble(longField.getText());

                    actualizarStatus("Creando formulario...");

                    var response = grpcClient.crearFormulario(
                            nombre,
                            sector,
                            nivel,
                            lat,
                            lon,
                            fotoBase64[0],
                            usuarioId,
                            usuarioId);

                    if (response.getSuccess()) {
                        actualizarStatus("Formulario creado: " + response.getId());
                        Platform.runLater(() -> {
                            dialogStage.close();
                            refrescarFormularios();
                        });
                    } else {
                        actualizarStatus("[ERROR] " + response.getMessage());
                    }

                } catch (NumberFormatException ex) {
                    actualizarStatus("[ERROR] Latitud/Longitud deben ser números");
                } catch (Exception ex) {
                    actualizarStatus("[ERROR] Error: " + ex.getMessage());
                    logger.error("Error al crear formulario", ex);
                }
            }).start();
        });

        cancelarButton.setOnAction(e -> dialogStage.close());

        dialogStage.setWidth(480);
        dialogStage.setHeight(700);
        dialogStage.show();
    }

    private void actualizarStatus(String mensaje) {
        Platform.runLater(() -> statusLabel.setText(mensaje));
    }

    private void abrirDialogoEditarFormulario(FormularioRow row) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Editar Formulario");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(20));
        vBox.setStyle("-fx-background-color: #f9f9f9;");

        TextField nombreField = new TextField(row.getNombre() != null ? row.getNombre() : "");
        nombreField.setPromptText("Nombre");

        TextField sectorField = new TextField(row.getSector() != null ? row.getSector() : "");
        sectorField.setPromptText("Sector");

        ComboBox<String> nivelCombo = new ComboBox<>();
        nivelCombo.getItems().addAll("BASICO", "MEDIO", "GRADO_UNIVERSITARIO", "POSTGRADO", "DOCTORADO");

        String nivel = normalizarNivel(row.getNivelEducativo());
        nivelCombo.setValue(nivel != null ? nivel : "PRIMARIA");

        double latVal = row.getLatitud() != null ? row.getLatitud() : 0.0;
        double lonVal = row.getLongitud() != null ? row.getLongitud() : 0.0;
        TextField latField = new TextField(String.valueOf(latVal));
        TextField lonField = new TextField(String.valueOf(lonVal));

        String[] photoBase64 = { row.getPhotoBase64() != null ? row.getPhotoBase64() : "" };
        Label photoLabel = new Label(row.getTieneFoto().equals("Si") ? "Foto: Si" : "Sin foto");
        Button selectPhotoBtn = new Button("Cambiar Foto");
        selectPhotoBtn.setOnAction(e -> seleccionarFotoDialog(photoLabel, photoBase64));

        vBox.getChildren().addAll(
                new Label("Nombre:"), nombreField,
                new Label("Sector:"), sectorField,
                new Label("Nivel Educativo:"), nivelCombo,
                new Label("Latitud:"), latField,
                new Label("Longitud:"), lonField,
                selectPhotoBtn, photoLabel);

        Button guardarButton = new Button("Guardar");
        Button cancelarButton = new Button("Cancelar");
        HBox buttonBox = new HBox(10, guardarButton, cancelarButton);
        buttonBox.setStyle("-fx-alignment: center;");
        vBox.getChildren().add(buttonBox);

        guardarButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String nombre = nombreField.getText().trim();
                    String sector = sectorField.getText().trim();
                    String nivelVal = nivelCombo.getValue();
                    double lat = Double.parseDouble(latField.getText().trim());
                    double lon = Double.parseDouble(lonField.getText().trim());

                    if (nombre.isEmpty() || sector.isEmpty() || nivelVal == null) {
                        actualizarStatus("[ERROR] Completa todos los campos");
                        return;
                    }

                    actualizarStatus("Actualizando formulario...");
                    var response = grpcClient.actualizarFormulario(row.getId(), nombre, sector, nivelVal, lat, lon,
                            photoBase64[0]);

                    if (response.getSuccess()) {
                        actualizarStatus("[OK] Formulario actualizado exitosamente");
                        refrescarFormularios();
                        Platform.runLater(dialogStage::close);
                    } else {
                        actualizarStatus("[ERROR] " + response.getMessage());
                    }
                } catch (NumberFormatException ex) {
                    actualizarStatus("[ERROR] Latitud/Longitud deben ser números");
                } catch (Exception ex) {
                    actualizarStatus("[ERROR] " + ex.getMessage());
                    logger.error("Error al actualizar", ex);
                }
            }).start();
        });

        cancelarButton.setOnAction(e -> dialogStage.close());

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);
        dialogStage.setScene(new Scene(scrollPane, 480, 600));
        dialogStage.show();
    }

    private String normalizarNivel(String raw) {
        if (raw == null)
            return null;
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
            new Thread(() -> {
                try {
                    actualizarStatus("Eliminando formulario...");
                    var response = grpcClient.eliminarFormulario(row.getId());

                    if (response.getSuccess()) {
                        actualizarStatus("[OK] Formulario eliminado exitosamente");
                        refrescarFormularios();
                    } else {
                        actualizarStatus("[ERROR] " + response.getMessage());
                    }
                } catch (Exception ex) {
                    actualizarStatus("[ERROR] " + ex.getMessage());
                    logger.error("Error al eliminar", ex);
                }
            }).start();
        }
    }

    private void seleccionarFotoDialog(Label photoLabel, String[] photoBase64) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Foto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("Todos", "*.*"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                photoBase64[0] = Base64.getEncoder().encodeToString(fileContent);
                photoLabel.setText("Foto: " + file.getName());
            } catch (Exception ex) {
                actualizarStatus("[ERROR] No se pudo cargar la foto");
            }
        }
    }

    public static class FormularioRow {
        private final String id;
        private final String nombre;
        private final String sector;
        private final String nivelEducativo;
        private final String tieneFoto;
        private final String fechaCreacion;
        private final Double latitud;
        private final Double longitud;
        private final String photoBase64;

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
