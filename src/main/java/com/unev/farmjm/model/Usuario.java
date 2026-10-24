package com.unev.farmjm.model;

/** Clase del modelo: Usuario. */
public class Usuario {

    // Atributos privados (encapsulamiento)
    private int idUsuario;
    private String nombre;
    private String apellido;
    private String licencia;
    private String username;
    private String clave;            // hash BCrypt (nunca texto plano)
    private String email;
    private String totpSecret;        // clave secreta 2FA (Base32), guardada en la BD
    private boolean totpActivo;
    private int intentosFallidos;
    private java.time.LocalDateTime bloqueadoHasta;
    private Rol rol;
    private boolean activo;

    // Constructor vacío
    public Usuario() { }

    // Constructor con parámetros
    public Usuario(int idUsuario, String nombre, String apellido, String licencia, String username, String clave, Rol rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.licencia = licencia;
        this.username = username;
        this.clave = clave;
        this.rol = rol;
        this.activo = activo;
    }

    // Getters y setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getLicencia() { return licencia; }
    public void setLicencia(String licencia) { this.licencia = licencia; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public boolean isTotpActivo() { return totpActivo; }
    public void setTotpActivo(boolean totpActivo) { this.totpActivo = totpActivo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public java.time.LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(java.time.LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    // Métodos auxiliares
    public boolean estaBloqueado() { return bloqueadoHasta != null && bloqueadoHasta.isAfter(java.time.LocalDateTime.now()); }
    public String getNombreCompleto() { return nombre + " " + apellido; }

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }
}
