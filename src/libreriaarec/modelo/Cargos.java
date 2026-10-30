package libreriaarec.modelo;

import com.google.common.collect.ImmutableList;

/** Cargos que puede tener un trabajador de la librería. */
public final class Cargos {

    public static final String ADMINISTRADOR = "Administrador";

    /** Lista inmutable de cargos (Google Guava). */
    public static final ImmutableList<String> TODOS = ImmutableList.of(
            "Reponedor", "Vendedor", "Coordinador", "Auxiliar de almacén",
            "Cajero", "Encargado de sección", ADMINISTRADOR);

    private Cargos() { }
}
