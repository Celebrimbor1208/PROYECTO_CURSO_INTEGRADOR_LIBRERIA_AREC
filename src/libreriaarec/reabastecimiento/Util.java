package libreriaarec.reabastecimiento;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

/** Funciones de ayuda compartidas (fechas y montos). */
public class Util {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static JSpinner crearSpinnerFecha(LocalDate inicial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(aDate(inicial), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    public static LocalDate aLocalDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static double leerMonto(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
    }

    public static String soles(double valor) {
        return String.format("%.2f S/.", valor);
    }
}
