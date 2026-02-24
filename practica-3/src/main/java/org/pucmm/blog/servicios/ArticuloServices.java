package org.pucmm.blog.servicios;

import org.pucmm.blog.encapsulaciones.Articulo;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ArticuloServices extends GestionDb<Articulo> {

    private static ArticuloServices instancia;

    private ArticuloServices() {
        super(Articulo.class);
    }

    public static ArticuloServices getInstancia() {
        if (instancia == null) {
            instancia = new ArticuloServices();
        }
        return instancia;
    }

    public List<Articulo> buscarArticulosOrdenadosPorFecha() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select a from Articulo a order by a.fecha desc", Articulo.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Articulo> buscarArticulosPaginados(int pagina, int cantidadPorPagina) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select a from Articulo a order by a.fecha desc", Articulo.class)
                    .setFirstResult((pagina - 1) * cantidadPorPagina) // Desde qué registro iniciar
                    .setMaxResults(cantidadPorPagina)                 // Cuántos registros traer
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long contarArticulos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("select count(a) from Articulo a", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Articulo> buscarPorEtiqueta(String nombreEtiqueta) {
        EntityManager em = getEntityManager();
        try {
            // Hacemos un JOIN con la lista de etiquetas para filtrar
            return em.createQuery(
                            "select a from Articulo a join a.listaEtiquetas e where e.etiqueta = :etiqueta order by a.fecha desc",
                            Articulo.class)
                    .setParameter("etiqueta", nombreEtiqueta)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}