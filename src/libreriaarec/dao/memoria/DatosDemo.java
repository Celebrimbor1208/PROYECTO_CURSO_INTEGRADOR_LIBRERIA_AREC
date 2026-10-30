package libreriaarec.dao.memoria;

import java.time.LocalDate;
import libreriaarec.dao.EmpleadoDAO;
import libreriaarec.dao.LibroDAO;
import libreriaarec.modelo.Cargos;
import libreriaarec.modelo.Empleado;
import libreriaarec.modelo.Libro;
import libreriaarec.util.PasswordHasher;

/**
 * Datos de ejemplo para poder probar el sistema (DNI y correos ficticios).
 * Se cargan al iniciar y desaparecen cuando se conecte la base de datos real.
 *
 * Usuarios de prueba (código de usuario = DNI):
 * <ul>
 *   <li>70000003 / Admin2026 - Administrador</li>
 *   <li>70000001 / Venta2026 - Vendedor</li>
 *   <li>70000002 / Caja2026 - Cajero</li>
 * </ul>
 * Las contraseñas se guardan con hash, no en texto plano.
 */
public final class DatosDemo {

    private DatosDemo() { }

    public static void cargar(LibroDAO libros, EmpleadoDAO empleados) {
        libro(libros, "Irene Vallejo", "El infinito en un junco", "Ensayo", "Siruela", 24.90, 15);
        libro(libros, "Virginia Woolf", "Una habitación propia", "Ensayo", "Cátedra", 19.50, 4);
        libro(libros, "Gabriel García Márquez", "Cien años de soledad", "Novela", "Penguin Random House", 22.00, 20);
        libro(libros, "Rosa Montero", "La ridícula idea de no volver a verte", "Novela", "Lumen", 28.00, 3);
        libro(libros, "Roberto Bolaño", "Los detectives salvajes", "Novela", "Anagrama", 18.90, 12);
        libro(libros, "Juan Rulfo", "Pedro Páramo", "Novela", "Alfaguara", 15.50, 25);

        empleado(empleados, "Lucía", "Fernández Rojas", "70000001", "987654321", "lucia@ejemplo.com",
                "Vendedor", 26, "Venta2026");
        empleado(empleados, "Carlos", "Mendoza Quispe", "70000002", "912345678", "carlos@ejemplo.com",
                "Cajero", 3, "Caja2026");
        empleado(empleados, "Marta", "Salazar Vega", "70000003", "955443322", "marta@ejemplo.com",
                Cargos.ADMINISTRADOR, 40, "Admin2026");
    }

    private static void libro(LibroDAO dao, String autor, String titulo, String genero,
                              String editorial, double precio, int stock) {
        dao.guardar(new Libro(dao.siguienteCodigo(), titulo, autor, genero, editorial,
                LocalDate.now(), precio, stock));
    }

    private static void empleado(EmpleadoDAO dao, String nombres, String apellidos, String dni,
                                 String celular, String correo, String cargo, int mesesAtras, String clave) {
        Empleado e = new Empleado(nombres, apellidos, dni, celular, correo, cargo,
                LocalDate.now().minusMonths(mesesAtras));
        e.setRegistrado(true);
        e.setClaveHash(PasswordHasher.hash(clave.toCharArray()));
        dao.guardar(e);
    }
}
