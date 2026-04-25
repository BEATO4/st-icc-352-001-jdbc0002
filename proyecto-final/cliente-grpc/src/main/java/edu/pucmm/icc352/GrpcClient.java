package edu.pucmm.icc352;

import edu.pucmm.icc352.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);

    private final String host;
    private final int port;
    private volatile boolean useTls;

    private ManagedChannel channel;
    private EncuestaServiceGrpc.EncuestaServiceBlockingStub stub;

    public GrpcClient(String host, int port) {
        this(host, port, false);
    }

    public GrpcClient(String host, int port, boolean useTls) {
        this.host = host;
        this.port = port;
        this.useTls = useTls;
        initializeChannel();
    }

    private synchronized void initializeChannel() {
        logger.info("Conectando a servidor gRPC en {}:{} (tls={})", host, port, useTls);
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        if (!useTls) {
            channelBuilder.usePlaintext();
        }
        this.channel = channelBuilder.build();
        this.stub = EncuestaServiceGrpc.newBlockingStub(channel);
        logger.info("Cliente gRPC listo");
    }

    private synchronized void reconnectWithMode(boolean newUseTls) {
        if (this.useTls == newUseTls) {
            return;
        }
        logger.warn("Reintentando conexión gRPC automáticamente con tls={}", newUseTls);
        this.useTls = newUseTls;
        if (channel != null) {
            channel.shutdownNow();
        }
        initializeChannel();
    }

    private boolean isTlsMismatch(Exception e) {
        if (!(e instanceof StatusRuntimeException statusEx)) {
            return false;
        }
        if (statusEx.getStatus().getCode() != Status.Code.UNAVAILABLE) {
            return false;
        }
        String message = String.valueOf(statusEx.getMessage()).toLowerCase();
        return message.contains("not an ssl/tls record")
                || message.contains("first received frame was not settings")
                || message.contains("ssl")
                || message.contains("tls");
    }

    private <T> T executeWithAutoTlsFallback(Callable<T> call) throws Exception {
        try {
            return call.call();
        } catch (Exception firstError) {
            if (!isTlsMismatch(firstError)) {
                throw firstError;
            }
            boolean fallbackMode = !useTls;
            reconnectWithMode(fallbackMode);
            return call.call();
        }
    }


    public List<FormularioMessage> listarFormularios(String usuarioId) {
        try {
            logger.info("Solicitando formularios del usuario: {}", usuarioId);

            ListarRequest request = ListarRequest.newBuilder()
                    .setUsuarioId(usuarioId)
                    .build();

            ListarResponse response = executeWithAutoTlsFallback(() -> stub.listarFormularios(request));

            if (response.getSuccess()) {
                logger.info("Se obtuvieron {} formularios", response.getFormulariosCount());
                return response.getFormulariosList();
            } else {
                logger.warn("Error en listarFormularios: {}", response.getMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            logger.error("Error al listar formularios", e);
            throw new RuntimeException("Error al conectar con el servidor: " + e.getMessage(), e);
        }
    }


    public List<FormularioMessage> listarTodos() {
        try {
            logger.info("Solicitando TODOS los formularios");

            ListarTodosRequest request = ListarTodosRequest.newBuilder().build();
            ListarResponse response = executeWithAutoTlsFallback(() -> stub.listarTodos(request));

            if (response.getSuccess()) {
                logger.info("Se obtuvieron {} formularios", response.getFormulariosCount());
                return response.getFormulariosList();
            } else {
                logger.warn("Error en listarTodos: {}", response.getMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            logger.error("Error al listar todos los formularios", e);
            throw new RuntimeException("Error al conectar con el servidor: " + e.getMessage(), e);
        }
    }


    public CrearFormularioResponse crearFormulario(
            String name,
            String sector,
            String educationalLevel,
            double latitude,
            double longitude,
            String photoBase64,
            String userId,
            String username) {

        try {
            logger.info("Creando formulario: {}", name);

            CrearFormularioRequest request = CrearFormularioRequest.newBuilder()
                    .setName(name)
                    .setSector(sector)
                    .setEducationalLevel(educationalLevel)
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setPhotoBase64(photoBase64 != null ? photoBase64 : "")
                    .setUserId(userId)
                    .setUsername(username)
                    .build();

            CrearFormularioResponse response = executeWithAutoTlsFallback(() -> stub.crearFormulario(request));

            if (response.getSuccess()) {
                logger.info("Formulario creado exitosamente con ID: {}", response.getId());
            } else {
                logger.warn("Error al crear formulario: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("Error al crear formulario", e);
            throw new RuntimeException("Error al crear formulario: " + e.getMessage(), e);
        }
    }

    public ActualizarFormularioResponse actualizarFormulario(
            String id,
            String name,
            String sector,
            String educationalLevel,
            double latitude,
            double longitude,
            String photoBase64) {

        try {
            logger.info("Actualizando formulario: {}", id);

            ActualizarFormularioRequest request = ActualizarFormularioRequest.newBuilder()
                    .setId(id)
                    .setName(name)
                    .setSector(sector)
                    .setEducationalLevel(educationalLevel)
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setPhotoBase64(photoBase64 != null ? photoBase64 : "")
                    .build();

            ActualizarFormularioResponse response = executeWithAutoTlsFallback(() -> stub.actualizarFormulario(request));

            if (response.getSuccess()) {
                logger.info("Formulario actualizado exitosamente");
            } else {
                logger.warn("Error al actualizar formulario: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("Error al actualizar formulario", e);
            throw new RuntimeException("Error al actualizar formulario: " + e.getMessage(), e);
        }
    }

    public EliminarFormularioResponse eliminarFormulario(String id) {
        try {
            logger.info("Eliminando formulario: {}", id);

            EliminarFormularioRequest request = EliminarFormularioRequest.newBuilder()
                    .setId(id)
                    .build();

            EliminarFormularioResponse response = executeWithAutoTlsFallback(() -> stub.eliminarFormulario(request));

            if (response.getSuccess()) {
                logger.info("Formulario eliminado exitosamente");
            } else {
                logger.warn("Error al eliminar formulario: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("Error al eliminar formulario", e);
            throw new RuntimeException("Error al eliminar formulario: " + e.getMessage(), e);
        }
    }

    public void cerrar() {
        try {
            logger.info("Cerrando conexión gRPC...");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("Conexión gRPC cerrada");
        } catch (InterruptedException e) {
            logger.error("Error al cerrar conexión gRPC", e);
            channel.shutdownNow();
        }
    }

    /**
     * Verificar si el canal está conectado
     */
    public boolean isConnected() {
        return channel != null && !channel.isShutdown();
    }
}
