package libreriaarec.dao;

import java.util.List;
import java.util.Optional;
import libreriaarec.modelo.Empleado;

/** Acceso a los datos de los trabajadores registrados (patrón DAO). */
public interface EmpleadoDAO {

    Optional<Empleado> buscarPorDni(String dni);

    List<Empleado> listar();

    /** Inserta un trabajador nuevo. */
    void guardar(Empleado empleado);

    /**
     * Actualiza un trabajador ya guardado.
     * @param dniOriginal el DNI con el que estaba guardado (por si se corrigió el DNI)
     */
    void actualizar(String dniOriginal, Empleado empleado);
}
