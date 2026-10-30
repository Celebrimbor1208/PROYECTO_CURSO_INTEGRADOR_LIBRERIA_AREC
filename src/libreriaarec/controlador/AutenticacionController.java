package libreriaarec.controlador;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import libreriaarec.dao.EmpleadoDAO;
import libreriaarec.modelo.Empleado;
import libreriaarec.util.PasswordHasher;
import libreriaarec.util.Texto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inicio de sesión con medidas de seguridad:
 * <ul>
 *   <li>La contraseña se compara contra un hash PBKDF2 (nunca contra texto plano).</li>
 *   <li>Tras {@value #MAX_INTENTOS} intentos fallidos se bloquea el acceso por un tiempo.</li>
 *   <li>El mensaje de error es el mismo si el usuario no existe o la contraseña es incorrecta.</li>
 *   <li>Los registros (logs) nunca incluyen contraseñas y ocultan parte del DNI.</li>
 * </ul>
 */
public class AutenticacionController {

    public static final int MAX_INTENTOS = 3;
    public static final Duration TIEMPO_BLOQUEO = Duration.ofMinutes(1);

    private static final Logger LOG = LoggerFactory.getLogger(AutenticacionController.class);

    /** Hash de relleno: se verifica cuando el DNI no existe, para que tarde lo mismo que un DNI real. */
    private static final String HASH_RELLENO = PasswordHasher.hash("relleno-no-valido".toCharArray());

    /** Resultado de un intento de inicio de sesión. */
    public static final class Resultado {
        public enum Estado { CORRECTO, CREDENCIALES_INVALIDAS, BLOQUEADO }

        private final Estado estado;
        private final Empleado empleado;
        private final long segundosRestantes;

        private Resultado(Estado estado, Empleado empleado, long segundosRestantes) {
            this.estado = estado;
            this.empleado = empleado;
            this.segundosRestantes = segundosRestantes;
        }

        public Estado getEstado() { return estado; }
        public Empleado getEmpleado() { return empleado; }
        public long getSegundosRestantes() { return segundosRestantes; }
        public boolean esCorrecto() { return estado == Estado.CORRECTO; }
    }

    private final EmpleadoDAO empleados;
    private final Clock reloj;
    private final Map<String, Integer> fallos = new HashMap<>();
    private final Map<String, Instant> bloqueadoHasta = new HashMap<>();

    public AutenticacionController(EmpleadoDAO empleados, Clock reloj) {
        this.empleados = checkNotNull(empleados);
        this.reloj = checkNotNull(reloj);
    }

    /**
     * Intenta iniciar sesión.
     * @param dni el código de usuario (DNI)
     * @param clave la contraseña escrita (el llamador debe borrar el arreglo después)
     */
    public Resultado iniciarSesion(String dni, char[] clave) {
        String usuario = dni == null ? "" : dni.trim();
        String id = Texto.enmascararDni(usuario);

        Instant ahora = reloj.instant();
        Instant hasta = bloqueadoHasta.get(usuario);
        if (hasta != null) {
            if (ahora.isBefore(hasta)) {
                long seg = Math.max(1, Duration.between(ahora, hasta).getSeconds());
                LOG.warn("Intento de acceso con cuenta bloqueada: {}", id);
                return new Resultado(Resultado.Estado.BLOQUEADO, null, seg);
            }
            bloqueadoHasta.remove(usuario);
            fallos.remove(usuario);
        }

        Optional<Empleado> encontrado = empleados.buscarPorDni(usuario)
                .filter(Empleado::isRegistrado);
        boolean correcto;
        if (encontrado.isPresent()) {
            correcto = PasswordHasher.verificar(clave, encontrado.get().getClaveHash());
        } else {
            PasswordHasher.verificar(clave, HASH_RELLENO); // mismo tiempo de respuesta
            correcto = false;
        }

        if (correcto) {
            fallos.remove(usuario);
            LOG.info("Inicio de sesión correcto: {}", id);
            return new Resultado(Resultado.Estado.CORRECTO, encontrado.get(), 0);
        }

        int n = fallos.merge(usuario, 1, Integer::sum);
        LOG.warn("Inicio de sesión fallido ({}/{}): {}", n, MAX_INTENTOS, id);
        if (n >= MAX_INTENTOS) {
            bloqueadoHasta.put(usuario, ahora.plus(TIEMPO_BLOQUEO));
            LOG.warn("Cuenta bloqueada temporalmente por intentos fallidos: {}", id);
            return new Resultado(Resultado.Estado.BLOQUEADO, null, TIEMPO_BLOQUEO.getSeconds());
        }
        return new Resultado(Resultado.Estado.CREDENCIALES_INVALIDAS, null, 0);
    }
}
