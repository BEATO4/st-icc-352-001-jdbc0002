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

            if (request.getName().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("El nombre del formulario es requerido").asException());
                return;
            }

            if (request.getSector().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("El sector es requerido").asException());
                return;
            }

            if (request.getEducationalLevel().isBlank()) {
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

    @Override
    public void actualizarFormulario(ActualizarFormularioRequest request, StreamObserver<ActualizarFormularioResponse> responseObserver) {
        try {
            logger.info("Actualizar formulario: {}", request.getId());

            var formOpt = surveyFormService.getFormById(request.getId());
            if (formOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Formulario no encontrado").asException());
                return;
            }

            SurveyForm form = formOpt.get();

            if (!request.getName().isBlank())
                form.setName(request.getName());
            if (!request.getSector().isBlank())
                form.setSector(request.getSector());
            if (!request.getEducationalLevel().isBlank())
                form.setEducationalLevel(request.getEducationalLevel());
            if (request.getLatitude() != 0)
                form.setLatitude(request.getLatitude());
            if (request.getLongitude() != 0)
                form.setLongitude(request.getLongitude());
            if (!request.getPhotoBase64().isBlank())
                form.setPhotoBase64(request.getPhotoBase64());

            boolean updated = surveyFormService.updateForm(form);

            if (updated) {
                ActualizarFormularioResponse response = ActualizarFormularioResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Formulario actualizado exitosamente")
                        .setFormulario(mapToFormularioMessage(form))
                        .build();
                responseObserver.onNext(response);
            } else {
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error al actualizar el formulario").asException());
                return;
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error al actualizar formulario", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al actualizar formulario: " + e.getMessage()).asException());
        }
    }

    @Override
    public void eliminarFormulario(EliminarFormularioRequest request, StreamObserver<EliminarFormularioResponse> responseObserver) {
        try {
            logger.info("Eliminar formulario: {}", request.getId());

            var formOpt = surveyFormService.getFormById(request.getId());
            if (formOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Formulario no encontrado").asException());
                return;
            }

            boolean deleted = surveyFormService.deleteForm(request.getId());

            if (deleted) {
                EliminarFormularioResponse response = EliminarFormularioResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Formulario eliminado exitosamente")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error al eliminar el formulario").asException());
            }

        } catch (Exception e) {
            logger.error("Error al eliminar formulario", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al eliminar formulario: " + e.getMessage()).asException());
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
