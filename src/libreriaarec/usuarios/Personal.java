package libreriaarec.usuarios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Personal registrado ("base de datos" en memoria).
 * Más adelante se reemplazará por la tabla de usuarios de la base de datos real.
 */
public class Personal {

    public static final String ADMINISTRADOR = "Administrador";

    /** Meses de antigüedad necesarios para poder ascender a administrador. Cámbialo según tu política. */
    public static final int MESES_PARA_ADMIN = 12;

    public static final String[] CARGOS = {
        "Reponedor", "Vendedor", "Coordinador", "Auxiliar de almacén",
        "Cajero", "Encargado de sección", ADMINISTRADOR};

    private static final List<Empleado> LISTA = new ArrayList<>();

    static {
        // Datos de ejemplo para poder probar (DNI ficticios). Bórralos cuando uses la base de datos real.
        ejemplo("Lucía", "Fernández Rojas", "70000001", "987654321", "lucia@ejemplo.com", "Vendedor", 26);
        ejemplo("Carlos", "Mendoza Quispe", "70000002", "912345678", "carlos@ejemplo.com", "Cajero", 3);
        ejemplo("Marta", "Salazar Vega", "70000003", "955443322", "marta@ejemplo.com", ADMINISTRADOR, 40);
    }

    private static void ejemplo(String nombres, String apellidos, String dni, String celular,
                                String correo, String cargo, int mesesAtras) {
        Empleado e = new Empleado(nombres, apellidos, dni, celular, correo, cargo,
                LocalDate.now().minusMonths(mesesAtras));
        e.registrado = true;
        LISTA.add(e);
    }

    public static List<Empleado> todos() {
        return LISTA;
    }

    public static Empleado buscarPorDni(String dni) {
        for (Empleado e : LISTA) {
            if (e.dni.equals(dni)) return e;
        }
        return null;
    }

    /** Envía un trabajador a la "base de datos". */
    public static void registrar(Empleado e) {
        e.registrado = true;
        LISTA.add(e);
    }
}
