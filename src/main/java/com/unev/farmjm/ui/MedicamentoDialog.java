package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Categoria;
import com.unev.farmjm.model.Lote;
import com.unev.farmjm.model.Medicamento;
import com.unev.farmjm.service.MedicamentoService;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.util.List;

/** Modal para crear o editar un medicamento (con su lote principal). */
public class MedicamentoDialog extends FormModal {

    private final MedicamentoService servicio;
    private final Medicamento original;
    private final TextField txtCodigo = new TextField();
    private final TextField txtNombre = new TextField();
    private final TextField txtActivo = new TextField();
    private final TextField txtPresentacion = new TextField();
    private final ComboBox<Categoria> cmbCategoria = new ComboBox<>();
    private final TextField txtPrecio = new TextField();
    private final TextField txtStock = new TextField();
    private final CheckBox chkReceta = new CheckBox("Requiere receta");
    private final CheckBox chkControlado = new CheckBox("Controlado");
    private final TextField txtLote = new TextField();
    private final DatePicker dpVence = new DatePicker();

    private double precio;
    private int stock;

    public MedicamentoDialog(Window dueno, MedicamentoService servicio, List<Categoria> categorias,
                             Medicamento editar, Runnable alGuardar) {
        super(dueno, editar == null ? "Nuevo medicamento" : "Editar medicamento",
                "Los campos marcados con * son obligatorios.", alGuardar);
        this.servicio = servicio;
        this.original = editar;
        for (TextField t : new TextField[]{txtCodigo, txtNombre, txtActivo, txtPresentacion, txtPrecio, txtStock, txtLote}) {
            t.getStyleClass().add("input");
        }
        cmbCategoria.getStyleClass().add("combo-clean");
        cmbCategoria.setMaxWidth(Double.MAX_VALUE);
        cmbCategoria.getItems().setAll(categorias);
        dpVence.setMaxWidth(Double.MAX_VALUE);
        txtLote.setPromptText("Ej.: L-2027-05");

        campo("CÓDIGO DE BARRAS *", txtCodigo);
        campo("NOMBRE COMERCIAL *", txtNombre);
        campo("PRINCIPIO ACTIVO *", txtActivo);
        campo("PRESENTACIÓN", txtPresentacion);
        campo("CATEGORÍA *", cmbCategoria);
        campo("PRECIO (RD$) *", txtPrecio);
        campo("STOCK (UNIDADES) *", txtStock);
        fila(chkReceta, chkControlado);
        campo("LOTE *", txtLote);
        campo("FECHA DE VENCIMIENTO *", dpVence);

        if (editar != null) {
            txtCodigo.setText(editar.getCodigo());
            txtNombre.setText(editar.getNombre());
            txtActivo.setText(editar.getPrincipioActivo());
            txtPresentacion.setText(editar.getPresentacion());
            categorias.stream().filter(c -> c.getIdCategoria() == editar.getCategoria().getIdCategoria())
                    .findFirst().ifPresent(cmbCategoria::setValue);
            txtPrecio.setText(String.valueOf(editar.getPrecio()));
            txtStock.setText(String.valueOf(editar.getStock()));
            chkReceta.setSelected(editar.isRequiereReceta());
            chkControlado.setSelected(editar.isControlado());
            Lote l = editar.getLoteProximo();
            if (l != null) {
                txtLote.setText(l.getCodigoLote());
                dpVence.setValue(l.getFechaVencimiento());
            }
        }
    }

    @Override
    protected String validar() {
        if (txtCodigo.getText().isBlank()) return "El código es obligatorio.";
        if (txtNombre.getText().isBlank()) return "El nombre es obligatorio.";
        if (txtActivo.getText().isBlank()) return "El principio activo es obligatorio.";
        if (cmbCategoria.getValue() == null) return "Seleccione una categoría.";
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return "El precio debe ser un número (por ejemplo 250.00).";
        }
        if (precio <= 0) return "El precio debe ser mayor que cero.";
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
        } catch (NumberFormatException e) {
            return "El stock debe ser un número entero.";
        }
        if (stock < 0) return "El stock no puede ser negativo.";
        if (chkControlado.isSelected() && !chkReceta.isSelected()) return "Un medicamento controlado requiere receta.";
        if (txtLote.getText().isBlank()) return "Escriba el código del lote.";
        if (dpVence.getValue() == null) return "Seleccione la fecha de vencimiento.";
        return null;
    }

    @Override
    protected void guardar() {
        Medicamento m = new Medicamento();
        m.setIdProducto(original == null ? 0 : original.getIdProducto());
        m.setCodigo(txtCodigo.getText().trim());
        m.setNombre(txtNombre.getText().trim());
        m.setPrincipioActivo(txtActivo.getText().trim());
        m.setPresentacion(txtPresentacion.getText().trim());
        m.setCategoria(cmbCategoria.getValue());
        m.setPrecio(precio);
        m.setStock(stock);
        m.setRequiereReceta(chkReceta.isSelected());
        m.setControlado(chkControlado.isSelected());
        int idLote = original != null && original.getLoteProximo() != null ? original.getLoteProximo().getIdLote() : 0;
        Lote lote = new Lote(idLote, txtLote.getText().trim(), m, dpVence.getValue(), stock);
        servicio.guardar(Estado.usuario, m, lote);
    }
}
