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
         verTodosButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #2196F3; -fx-text-fill: white;");

         Button crearButton = new Button("[NEW] Crear Formulario");
         crearButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

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
                statusLabel
        );

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
        nombreCol.setPrefWidth(120);

        TableColumn<FormularioRow, String> sectorCol = new TableColumn<>("Sector");
        sectorCol.setCellValueFactory(new PropertyValueFactory<>("sector"));
        sectorCol.setPrefWidth(100);

        TableColumn<FormularioRow, String> nivelCol = new TableColumn<>("Nivel Educativo");
        nivelCol.setCellValueFactory(new PropertyValueFactory<>("nivelEducativo"));
        nivelCol.setPrefWidth(130);

        TableColumn<FormularioRow, String> fotoCol = new TableColumn<>("¿Foto?");
        fotoCol.setCellValueFactory(new PropertyValueFactory<>("tieneFoto"));
        fotoCol.setPrefWidth(70);

        TableColumn<FormularioRow, String> fechaCol = new TableColumn<>("Fecha Creación");
        fechaCol.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));
        fechaCol.setPrefWidth(150);

        table.getColumns().addAll(nombreCol, sectorCol, nivelCol, fotoCol, fechaCol);

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
                                fm.getName(),
                                fm.getSector(),
                                fm.getEducationalLevel(),
                                fm.getPhotoBase64().isEmpty() ? "No" : "Sí",
                                fm.getCreatedAt()
                        ));
                    }

                    actualizarStatus("[OK] Se cargaron " + formularios.size() + " formularios");
                });

            } catch (Exception ex) {
                System.out.println("[ERROR] Error: " + ex.getMessage());
                ex.printStackTrace();
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
                        System.out.println("  - " + fm.getName() + " (userId: " + fm.getUserId() + ", usuario: " + fm.getUsername() + ")");
                        tableView.getItems().add(new FormularioRow(
                                fm.getName(),
                                fm.getSector(),
                                fm.getEducationalLevel(),
                                fm.getPhotoBase64().isEmpty() ? "No" : "Sí",
                                fm.getCreatedAt()
                        ));
                    }

                    actualizarStatus("[OK] Se cargaron " + formularios.size() + " formularios de TODOS los usuarios");
                });

            } catch (Exception ex) {
                System.out.println("[ERROR] Error: " + ex.getMessage());
                ex.printStackTrace();
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
                "PRIMARIA",
                "SECUNDARIA",
                "UNIVERSIDAD",
                "POSTGRADO",
                "DOCTORADO"
        );
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

        final String[] fotoBase64 = {""};

        seleccionarImagenButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Imagen");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

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
        guardarButton.setStyle("-fx-padding: 10px 20px; -fx-font-size: 12px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

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
                botonesBox
        );

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
                            usuarioId
                    );

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

    public static class FormularioRow {
        private final String nombre;
        private final String sector;
        private final String nivelEducativo;
        private final String tieneFoto;
        private final String fechaCreacion;

        public FormularioRow(String nombre, String sector, String nivelEducativo, String tieneFoto, String fechaCreacion) {
            this.nombre = nombre;
            this.sector = sector;
            this.nivelEducativo = nivelEducativo;
            this.tieneFoto = tieneFoto;
            this.fechaCreacion = fechaCreacion;
        }

        public String getNombre() { return nombre; }
        public String getSector() { return sector; }
        public String getNivelEducativo() { return nivelEducativo; }
        public String getTieneFoto() { return tieneFoto; }
        public String getFechaCreacion() { return fechaCreacion; }
    }
}


