package org.pucmm.eventos.enums;

/**
 * Roles disponibles en el sistema.
 *
 * ADMIN       → Acceso total: gestiona usuarios, eventos y puede asignar/revocar
 *               el rol de organizador. El admin inicial no puede ser eliminado.
 *
 * ORGANIZER   → Puede crear, editar, cancelar y publicar/des-publicar eventos.
 *               También escanea QR para registrar asistencia.
 *
 * PARTICIPANT → Puede ver el listado de eventos publicados, inscribirse
 *               y cancelar su inscripción antes de la fecha del evento.
 */
public enum Role {

    ADMIN,
    ORGANIZER,
    PARTICIPANT;

    /** Devuelve true si el rol tiene permisos de gestión de eventos. */
    public boolean canManageEvents() {
        return this == ADMIN || this == ORGANIZER;
    }

    /** Devuelve true si el rol puede administrar usuarios. */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /** Etiqueta legible para mostrar en la interfaz. */
    public String getLabel() {
        return switch (this) {
            case ADMIN       -> "Administrador";
            case ORGANIZER   -> "Organizador";
            case PARTICIPANT -> "Participante";
        };
    }
}