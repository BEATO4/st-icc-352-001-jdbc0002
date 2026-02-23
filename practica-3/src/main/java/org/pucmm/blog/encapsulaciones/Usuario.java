package org.pucmm.blog.encapsulaciones;

public class Usuario {

    private String username;
    private String nombre;
    private String password;
    private boolean administrator;
    private boolean autor;

    public Usuario(String username, String nombre, String password, boolean administrator, boolean autor) {
        this.username = username;
        this.nombre = nombre;
        this.password = password;
        this.administrator = administrator;
        this.autor = autor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public boolean isAutor() {
        return autor;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public void setAutor(boolean autor) {
        this.autor = autor;
    }
}
