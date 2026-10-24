package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Cliente;
import com.unev.farmjm.service.ClienteService;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/** Modal para crear o editar clientes. */
public class ClienteDialog extends FormModal {

    private final ClienteService servicio;
    private final Cliente original;
    private final TextField txtNombre = new TextField();
    private final TextField txtDoc = new TextField();
    private final TextField txtTel = new TextField();
    private final TextField txtEmail = new TextField();

    public ClienteDialog(Window dueno, ClienteService servicio, Cliente editar, Runnable alGuardar) {
        super(dueno, editar == null ? "Nuevo cliente" : "Editar cliente", "Los campos marcados con * son obligatorios.", alGuardar);
        this.servicio = servicio;
        this.original = editar;
        for (TextField t : new TextField[]{txtNombre, txtDoc, txtTel, txtEmail}) t.getStyleClass().add("input");
        txtDoc.setPromptText("001-0000000-0");
        txtTel.setPromptText("809-555-0000");
        campo("NOMBRE *", txtNombre);
        campo("CÉDULA / RNC", txtDoc);
        campo("TELÉFONO", txtTel);
        campo("CORREO ELECTRÓNICO", txtEmail);
        if (editar != null) {
            txtNombre.setText(editar.getNombre());
            txtDoc.setText(editar.getCedulaRnc());
            txtTel.setText(editar.getTelefono());
            txtEmail.setText(editar.getEmail());
        }
    }

    @Override
    protected String validar() {
        return txtNombre.getText().isBlank() ? "El nombre del cliente es obligatorio." : null;
    }

    @Override
    protected void guardar() {
        Cliente c = new Cliente(original == null ? 0 : original.getIdCliente(), txtNombre.getText().trim(),
                txtDoc.getText().trim(), txtTel.getText().trim(), txtEmail.getText().trim());
        servicio.guardar(Estado.usuario, c);
    }
}
