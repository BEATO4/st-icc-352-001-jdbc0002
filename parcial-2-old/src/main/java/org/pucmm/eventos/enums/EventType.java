package org.pucmm.eventos.enums;

/**
 * Tipos de eventos académicos soportados por el sistema.
 */
public enum EventType {

    CHARLA,
    TALLER,
    SEMINARIO,
    CONGRESO,
    OTRO;

    /** Etiqueta legible para la interfaz. */
    public String getLabel() {
        return switch (this) {
            case CHARLA    -> "Charla";
            case TALLER    -> "Taller";
            case SEMINARIO -> "Seminario";
            case CONGRESO  -> "Congreso";
            case OTRO      -> "Otro";
        };
    }

    /** Icono Bootstrap Icons sugerido para cada tipo. */
    public String getIcon() {
        return switch (this) {
            case CHARLA    -> "bi-mic-fill";
            case TALLER    -> "bi-tools";
            case SEMINARIO -> "bi-journal-text";
            case CONGRESO  -> "bi-people-fill";
            case OTRO      -> "bi-calendar-event";
        };
    }
}