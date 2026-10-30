package libreriaarec.dao;

import java.util.List;
import libreriaarec.modelo.Venta;

/** Acceso a las ventas emitidas (patrón DAO). */
public interface VentaDAO {

    void guardar(Venta venta);

    List<Venta> listar();

    /** Siguiente número de boleta (B001-000001, B001-000002, ...). */
    String siguienteNumero();
}
