package com.unev.farmjm.service;

import com.unev.farmjm.dao.CatalogoDAO;
import com.unev.farmjm.dao.MedicamentoDAO;
import com.unev.farmjm.model.Categoria;
import com.unev.farmjm.model.Lote;
import com.unev.farmjm.model.Medicamento;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.util.Fmt;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Reglas de negocio del inventario de medicamentos. */
public class MedicamentoService {

    private final MedicamentoDAO dao = new MedicamentoDAO();
    private final CatalogoDAO catalogo = new CatalogoDAO();

    public List<Medicamento> listar(Usuario actor) {
        Permisos.exigir(actor, Modulo.INVENTARIO, Accion.VER);
        try {
            return MedicamentoDAO.listar();
        } catch (SQLException e) {
            throw Errores.bd("listar medicamentos", e);
        }
    }

    public List<Categoria> categorias() {
        try {
            return catalogo.categorias();
        } catch (SQLException e) {
            throw Errores.bd("listar categorías", e);
        }
    }

    /** Crea o edita (según el id del medicamento) con su lote principal. */
    public void guardar(Usuario actor, Medicamento m, Lote lote) {
        boolean nuevo = m.getIdProducto() == 0;
        Permisos.exigir(actor, Modulo.INVENTARIO, nuevo ? Accion.CREAR : Accion.EDITAR);
        validar(m, lote);
        try {
            if (dao.existeCodigo(m.getCodigo(), m.getIdProducto())) {
                throw new ServicioException("Ya existe otro producto con el código " + m.getCodigo() + ".");
            }
            if (nuevo) dao.insertar(m, lote); else dao.actualizar(m, lote);
        } catch (SQLException e) {
            throw Errores.bd(nuevo ? "crear medicamento" : "editar medicamento", e);
        }
        Auditoria.registrar(actor, "Inventario", (nuevo ? "Creó " : "Editó ") + m.getNombre() + " (" + m.getCodigo()
                + ", " + Fmt.dinero(m.getPrecio()) + ", stock " + m.getStock() + ")");
    }

    public void eliminar(Usuario actor, Medicamento m) {
        Permisos.exigir(actor, Modulo.INVENTARIO, Accion.ELIMINAR);
        try {
            dao.desactivar(m.getIdProducto());
        } catch (SQLException e) {
            throw Errores.bd("eliminar medicamento", e);
        }
        Auditoria.registrar(actor, "Inventario", "Eliminó " + m.getNombre() + " (" + m.getCodigo() + ")");
    }

    private void validar(Medicamento m, Lote lote) {
        if (m.getCodigo() == null || m.getCodigo().isBlank()) throw new ServicioException("El código es obligatorio.");
        if (m.getNombre() == null || m.getNombre().isBlank()) throw new ServicioException("El nombre es obligatorio.");
        if (m.getPrincipioActivo() == null || m.getPrincipioActivo().isBlank()) throw new ServicioException("El principio activo es obligatorio.");
        if (m.getCategoria() == null) throw new ServicioException("Seleccione una categoría.");
        if (m.getPrecio() <= 0) throw new ServicioException("El precio debe ser mayor que cero.");
        if (m.getStock() < 0) throw new ServicioException("El stock no puede ser negativo.");
        if (m.isControlado() && !m.isRequiereReceta()) throw new ServicioException("Un medicamento controlado siempre requiere receta.");
        if (lote != null) {
            if (lote.getCodigoLote() == null || lote.getCodigoLote().isBlank()) throw new ServicioException("El código de lote es obligatorio.");
            if (lote.getFechaVencimiento() == null) throw new ServicioException("La fecha de vencimiento es obligatoria.");
            if (lote.getFechaVencimiento().isBefore(LocalDate.now().minusYears(1))) throw new ServicioException("La fecha de vencimiento no es válida.");
        }
    }
}
