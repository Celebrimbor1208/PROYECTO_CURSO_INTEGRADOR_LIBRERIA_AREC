# Librerías AREC — Sistema de gestión (APF3)

Aplicación de escritorio en Java Swing con arquitectura **MVC + DAO**.

## Arquitectura
| Capa | Paquete | Responsabilidad |
|---|---|---|
| Vista | `libreriaarec`, `.venta`, `.inventario`, `.usuarios` | Solo pantallas; delegan en el controlador |
| Controlador | `libreriaarec.controlador` | Reglas de negocio y validaciones (sin Swing) |
| Modelo | `libreriaarec.modelo` | Libro, Empleado, Venta, Cargos |
| DAO | `libreriaarec.dao` (+ `.memoria`) | Acceso a datos tras interfaces; hoy en memoria, mañana JDBC |

`VentanaPrincipal` es la raíz de composición: crea los DAO y los inyecta en los controladores.
Diagramas en `docs/diagramas/` y Javadoc en `docs/javadoc/index.html`.

## SOLID aplicado
- **S**: cada controlador tiene un solo módulo; `BoletaTexto` solo da formato; `PasswordHasher` solo cifra.
- **O/L**: se agrega otro DAO (p. ej. JDBC) sin modificar controladores.
- **I**: interfaces DAO pequeñas por entidad.
- **D**: los controladores dependen de interfaces DAO y de `Clock`, no de clases concretas.

## Seguridad
- Contraseñas con PBKDF2-HMAC-SHA256 (120 000 iteraciones, sal aleatoria); nunca en texto plano.
- Bloqueo de 1 minuto tras 3 intentos fallidos; la contraseña se borra de memoria.
- Módulo de Usuarios solo para Administrador; DNI enmascarado en los logs.
- Contraseña temporal aleatoria al registrar trabajadores (se muestra una sola vez).

## Librerías
Guava (Preconditions, ImmutableList, Strings) · Apache Commons Lang3 (StringUtils) ·
SLF4J + Logback (logs en consola y `logs/libreriaarec.log`) · JUnit 4 + Hamcrest (pruebas).

## Instalación en NetBeans
1. Borra del proyecto: `venta/Catalogo.java`, `venta/Venta.java`, `usuarios/Empleado.java`, `usuarios/Personal.java`.
2. Copia las carpetas de `src/` (reemplazar `VentanaPrincipal`, `PanelLogin`, `PanelMenu`, `PanelVenta`, `PanelInventario`, `PanelUsuarios`) y `logback.xml` en la raíz de `src`.
3. Descarga y agrega en **Libraries → Add JAR**: guava, failureaccess, commons-lang3, slf4j-api, logback-classic, logback-core.
4. Copia `test/` y agrega en **Test Libraries**: junit 4.13.2 y hamcrest-core 1.3.
5. Clic derecho al proyecto → **Test** para correr las 36 pruebas.

## Credenciales de demostración
| DNI | Contraseña | Cargo |
|---|---|---|
| 70000003 | Admin2026 | Administrador |
| 70000001 | Venta2026 | Vendedor |
| 70000002 | Caja2026 | Cajero |

## Limitaciones conocidas
Datos en memoria (se pierden al cerrar); sin pantalla de cambio de contraseña; módulo Crecimiento pendiente.
