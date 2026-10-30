package libreriaarec.controlador;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que se puede adelantar a mano: permite probar el bloqueo de 1 minuto sin esperar. */
class RelojFalso extends Clock {
    private Instant ahora;

    RelojFalso(Instant inicio) { this.ahora = inicio; }

    void avanzar(Duration d) { ahora = ahora.plus(d); }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return ahora; }
}
