package libreriaarec.dao.memoria;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import libreriaarec.dao.EmpleadoDAO;
import libreriaarec.modelo.Empleado;

/** Implementación de {@link EmpleadoDAO} que guarda los trabajadores en memoria. */
public class EmpleadoDAOMemoria implements EmpleadoDAO {

    private final List<Empleado> empleados = new ArrayList<>();

    @Override
    public Optional<Empleado> buscarPorDni(String dni) {
        for (Empleado e : empleados) {
            if (e.getDni().equals(dni)) return Optional.of(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Empleado> listar() {
        return ImmutableList.copyOf(empleados);
    }

    @Override
    public void guardar(Empleado empleado) {
        empleados.add(empleado);
    }

    @Override
    public void actualizar(String dniOriginal, Empleado empleado) {
        for (int i = 0; i < empleados.size(); i++) {
            if (empleados.get(i).getDni().equals(dniOriginal)) {
                empleados.set(i, empleado);
                return;
            }
        }
        throw new IllegalArgumentException("No existe un trabajador con el DNI indicado");
    }
}
