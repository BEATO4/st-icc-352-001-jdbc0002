package edu.pucmm.icc352.services;

import edu.pucmm.icc352.models.*;
import edu.pucmm.icc352.repositories.*;
import edu.pucmm.icc352.utils.QRGenerator;
import java.time.LocalDateTime;
import java.util.List;

public class RegistrationService {

    private final RegistrationRepository regRepo = new RegistrationRepository();
    private final AttendanceRepository   attRepo = new AttendanceRepository();
    private final EventRepository        evtRepo = new EventRepository();

    public Registration register(User user, Long eventId) {
        Event event = evtRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado."));
        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new IllegalStateException("El evento no esta disponible para inscripcion.");
        if (event.getDateTime().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("El evento ya paso.");
        if (regRepo.findByUserAndEvent(user.getId(), eventId).isPresent())
            throw new IllegalStateException("Ya estas inscrito en este evento.");
        long inscritos = evtRepo.countRegistrations(eventId);
        if (inscritos >= event.getMaxCapacity())
            throw new IllegalStateException("El evento esta lleno.");
        return regRepo.save(new Registration(user, event));
    }

    public void cancel(Long userId, Long eventId) {
        Registration reg = regRepo.findByUserAndEvent(userId, eventId)
                .orElseThrow(() -> new IllegalArgumentException("No estas inscrito en este evento."));
        if (reg.getEvent().getDateTime().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("No puedes cancelar despues de la fecha del evento.");
        regRepo.delete(reg);
    }

    public Attendance markAttendance(String qrToken) {
        Registration reg = regRepo.findByToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("QR invalido o no encontrado."));
        if (attRepo.existsByRegistration(reg.getId()))
            throw new IllegalStateException("Asistencia ya registrada para este QR.");
        return attRepo.save(new Attendance(reg));
    }

    public byte[] generateQRImage(String qrToken) {
        try {
            return QRGenerator.generate(qrToken, 300, 300);
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR: " + e.getMessage());
        }
    }

    public List<Registration> findByEvent(Long eventId) { return regRepo.findByEvent(eventId); }
    public List<Registration> findByUser(Long userId)   { return regRepo.findByUser(userId); }
}