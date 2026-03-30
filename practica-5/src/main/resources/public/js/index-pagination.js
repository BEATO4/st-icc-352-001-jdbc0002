(function () {
    const paginationRoot = document.getElementById("ajax-pagination");
    const articlesContainer = document.getElementById("articles-container");
    const pageLabel = document.getElementById("page-label");

    if (!paginationRoot || !articlesContainer || !pageLabel) {
        return;
    }

    const buildCard = (articulo) => {
        const tags = (articulo.etiquetas || []).map((tag) =>
            `<a href="/?etiqueta=${encodeURIComponent(tag)}" class="badge bg-primary text-decoration-none me-1">${tag}</a>`
        ).join("");

        return `<div class="card mb-4 shadow-sm">
            <div class="card-body">
                <h2 class="card-title">${articulo.titulo}</h2>
                <p class="card-text text-muted">${articulo.resumen || ""}</p>
                <a href="/articulo/${articulo.id}" class="btn btn-primary">Leer mas &rarr;</a>
            </div>
            <div class="card-footer text-muted">
                Posteado el ${articulo.fecha || ""} por <strong>${articulo.autor || "anonimo"}</strong>
                <span class="ms-2">${tags}</span>
            </div>
        </div>`;
    };

    const updateControls = (paginaActual, totalPaginas) => {
        paginationRoot.dataset.page = String(paginaActual);
        paginationRoot.dataset.total = String(totalPaginas);
        pageLabel.textContent = `Pagina ${paginaActual} de ${totalPaginas}`;

        const prevTarget = paginationRoot.querySelector(".js-page-link[data-page-target]");
        if (prevTarget) {
            // Controls are reconstructed after each request, so no additional updates needed.
        }

        const prevHtml = paginaActual > 1
            ? `<a href="/?pagina=${paginaActual - 1}" data-page-target="${paginaActual - 1}" class="btn btn-outline-primary js-page-link">&laquo; Anterior</a>`
            : `<span class="btn btn-outline-secondary disabled">&laquo; Anterior</span>`;

        const nextHtml = paginaActual < totalPaginas
            ? `<a href="/?pagina=${paginaActual + 1}" data-page-target="${paginaActual + 1}" class="btn btn-outline-primary js-page-link">Siguiente &raquo;</a>`
            : `<span class="btn btn-outline-secondary disabled">Siguiente &raquo;</span>`;

        paginationRoot.innerHTML = `${prevHtml}<span id="page-label" class="align-self-center">Pagina ${paginaActual} de ${totalPaginas}</span>${nextHtml}`;
    };

    const loadPage = async (page) => {
        const response = await fetch(`/api/articulos?pagina=${page}`);
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        articlesContainer.innerHTML = (data.articulos || []).map(buildCard).join("");
        updateControls(data.paginaActual, data.totalPaginas);
    };

    paginationRoot.addEventListener("click", async (event) => {
        const link = event.target.closest(".js-page-link");
        if (!link) {
            return;
        }
        event.preventDefault();
        const page = Number(link.dataset.pageTarget || "1");
        if (!Number.isNaN(page)) {
            await loadPage(page);
        }
    });
})();

