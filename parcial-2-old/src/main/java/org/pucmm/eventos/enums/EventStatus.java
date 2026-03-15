package org.pucmm.eventos.enums;

/**
 * Estados del ciclo de vida de un evento.
 *
 * DRAFT      → Recién creado, solo visible para su organizador y el admin.
 *              No aparece en el listado público de participantes.
 *
 * PUBLISHED  → Visible para todos los participantes; se puede inscribir.
 *
 * CANCELLED  → Cancelado por el organizador o el admin.
 *              Las inscripciones activas deberían notificarse (futuro).
 */
public enum EventStatus {

    DRAFT,
    PUBLISHED,
    CANCELLED;

    /** Etiqueta legible para la interfaz. */
    public String getLabel() {
        return switch (this) {
            case DRAFT     -> "Borrador";
            case PUBLISHED -> "Publicado";
            case CANCELLED -> "Cancelado";
        };
    }

    /** Clase CSS de Bootstrap para la badge de estado. */
    public String getBadgeClass() {
        return switch (this) {
            case DRAFT     -> "bg-secondary";
            case PUBLISHED -> "bg-success";
            case CANCELLED -> "bg-danger";
        };
    }

    /** ¿Se puede inscribir un participante en este estado? */
    public boolean allowsRegistration() {
        return this == PUBLISHED;
    }
}