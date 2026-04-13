package edu.pucmm.icc352;

import edu.pucmm.icc352.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);

    private final ManagedChannel channel;
    private final EncuestaServiceGrpc.EncuestaServiceBlockingStub stub;

    public GrpcClient(String host, int port) {
        logger.info("Conectando a servidor gRPC en {}:{}", host, port);

        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.stub = EncuestaServiceGrpc.newBlockingStub(channel);
        logger.info("Cliente gRPC conectado exitosamente");
    }


    public List<FormularioMessage> listarFormularios(String usuarioId) {
        try {
            logger.info("Solicitando formularios del usuario: {}", usuarioId);

            ListarRequest request = ListarRequest.newBuilder()
                    .setUsuarioId(usuarioId)
                    .build();

            ListarResponse response = stub.listarFormularios(request);

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
            ListarResponse response = stub.listarTodos(request);

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

            CrearFormularioResponse response = stub.crearFormulario(request);

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

