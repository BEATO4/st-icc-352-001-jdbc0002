package edu.pucmm.icc352.backend.grpc;

import edu.pucmm.icc352.grpc.*;
import edu.pucmm.icc352.models.SurveyForm;
import edu.pucmm.icc352.backend.services.SurveyFormService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación del servicio gRPC EncuestaService
 */
public class EncuestaServiceImpl extends EncuestaServiceGrpc.EncuestaServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(EncuestaServiceImpl.class);
    private final SurveyFormService surveyFormService;

    public EncuestaServiceImpl(SurveyFormService surveyFormService) {
        this.surveyFormService = surveyFormService;
    }

    public EncuestaServiceImpl() {
        this(new SurveyFormService());
    }

    @Override
    public void listarFormularios(ListarRequest request, StreamObserver<ListarResponse> responseObserver) {
        try {
            String usuarioId = request.getUsuarioId();
            logger.info("Listar formularios para usuario: {}", usuarioId);

            List<SurveyForm> forms = surveyFormService.getFormsByUserId(usuarioId);

            ListarResponse.Builder responseBuilder = ListarResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Formularios obtenidos exitosamente");

            for (SurveyForm form : forms) {
                FormularioMessage mensaje = mapToFormularioMessage(form);
                responseBuilder.addFormularios(mensaje);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error al listar formularios", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al listar formularios: " + e.getMessage())
                            .asException()
            );
        }
    }

    @Override
    public void crearFormulario(CrearFormularioRequest request, StreamObserver<CrearFormularioResponse> responseObserver) {
        try {
            logger.info("Crear formulario para usuario: {}", request.getUsername());

            if (request.getName() == null || request.getName().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("El nombre del formulario es requerido").asException());
                return;
            }

            if (request.getSector() == null || request.getSector().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("El sector es requerido").asException());
                return;
            }

            if (request.getEducationalLevel() == null || request.getEducationalLevel().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("El nivel educativo es requerido").asException());
                return;
            }

            SurveyForm form = new SurveyForm(
                    request.getName(),
                    request.getSector(),
                    request.getEducationalLevel(),
                    request.getLatitude() != 0 ? request.getLatitude() : null,
                    request.getLongitude() != 0 ? request.getLongitude() : null,
                    request.getPhotoBase64().isBlank() ? null : request.getPhotoBase64(),
                    request.getUserId(),
                    request.getUsername()
            );

            SurveyForm created = surveyFormService.createForm(form);

            CrearFormularioResponse response = CrearFormularioResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Formulario creado exitosamente")
                    .setId(created.getIdAsString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación al crear formulario", e);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            logger.error("Error al crear formulario", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al crear formulario: " + e.getMessage()).asException());
        }
    }

    @Override
    public void listarTodos(ListarTodosRequest request, StreamObserver<ListarResponse> responseObserver) {
        try {
            logger.info("Listar TODOS los formularios");

            List<SurveyForm> forms = surveyFormService.getAllForms();

            ListarResponse.Builder responseBuilder = ListarResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todos los formularios obtenidos exitosamente");

            for (SurveyForm form : forms) {
                FormularioMessage mensaje = mapToFormularioMessage(form);
                responseBuilder.addFormularios(mensaje);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error al listar todos los formularios", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al listar formularios: " + e.getMessage())
                            .asException()
            );
        }
    }

    private FormularioMessage mapToFormularioMessage(SurveyForm form) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        FormularioMessage.Builder builder = FormularioMessage.newBuilder()
                .setId(form.getIdAsString() != null ? form.getIdAsString() : "")
                .setName(form.getName() != null ? form.getName() : "")
                .setSector(form.getSector() != null ? form.getSector() : "")
                .setEducationalLevel(form.getEducationalLevel() != null ? form.getEducationalLevel() : "")
                .setLatitude(form.getLatitude() != null ? form.getLatitude() : 0.0)
                .setLongitude(form.getLongitude() != null ? form.getLongitude() : 0.0)
                .setPhotoBase64(form.getPhotoBase64() != null ? form.getPhotoBase64() : "")
                .setUserId(form.getUserId() != null ? form.getUserId() : "")
                .setUsername(form.getUsername() != null ? form.getUsername() : "")
                .setSynced(form.isSynced());

        if (form.getCreatedAt() != null) {
            builder.setCreatedAt(form.getCreatedAt().format(formatter));
        }
        if (form.getUpdatedAt() != null) {
            builder.setUpdatedAt(form.getUpdatedAt().format(formatter));
        }

        return builder.build();
    }
}
