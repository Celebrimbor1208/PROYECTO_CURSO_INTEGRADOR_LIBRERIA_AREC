package libreriaarec.dao.memoria;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import libreriaarec.dao.VentaDAO;
import libreriaarec.modelo.Venta;

/** Implementación de {@link VentaDAO} que guarda las ventas en memoria. */
public class VentaDAOMemoria implements VentaDAO {

    private final List<Venta> ventas = new ArrayList<>();

    @Override
    public void guardar(Venta venta) {
        ventas.add(venta);
    }

    @Override
    public List<Venta> listar() {
        return ImmutableList.copyOf(ventas);
    }

    @Override
    public String siguienteNumero() {
        return String.format("B001-%06d", ventas.size() + 1);
    }
}
