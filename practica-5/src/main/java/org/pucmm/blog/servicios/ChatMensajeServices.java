package org.pucmm.blog.servicios;

import jakarta.persistence.EntityManager;
import org.pucmm.blog.encapsulaciones.ChatMensaje;

import java.util.List;

public class ChatMensajeServices extends GestionDb<ChatMensaje> {

	private static ChatMensajeServices instancia;

	private ChatMensajeServices() {
		super(ChatMensaje.class);
	}

	public static ChatMensajeServices getInstancia() {
		if (instancia == null) {
			instancia = new ChatMensajeServices();
		}
		return instancia;
	}

	public List<ChatMensaje> listarPorSesion(long sesionId) {
		EntityManager em = getEntityManager();
		try {
			return em.createQuery("select m from ChatMensaje m where m.sesion.id = :sesionId order by m.fecha asc", ChatMensaje.class)
					.setParameter("sesionId", sesionId)
					.getResultList();
		} finally {
			em.close();
		}
	}
}

