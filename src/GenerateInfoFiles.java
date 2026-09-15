
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class GenerateInfoFiles {
    
    /**
 * Genera los archivos de entrada para el proyecto Storage Cloud.
 *
 * <p>En esta primera entrega, la clase se encarga de generar:
 * <ul>
 *     <li>cuentas.csv</li>
 *     <li>transferencias.txt</li>
 * </ul>
 *
 * <p>Los datos generados son pseudoaleatorios y mantienen coherencia
 * entre las cuentas y las transferencias.
 *
 * @author Storage Cloud
 * @version 1.0
 */
     private static final String ARCHIVO_CUENTAS = "cuentas.csv";
    private static final String ARCHIVO_TRANSFERENCIAS = "transferencias.txt";

    private static final int CANTIDAD_CUENTAS = 10;
    private static final int CANTIDAD_TRANSFERENCIAS = 100;

    private static final String[] PLANES = {
        "Plan 25GB",
        "Plan 50GB",
        "Plan 100GB",
        "Plan 200GB"
    };

    private static final Random RANDOM = new Random();

    /**
     * Método principal de ejecución.
     *
     * @param args argumentos de ejecución. No se utilizan.
     */
    public static void main(String[] args) {

        try {
            System.out.println("======================================");
            System.out.println("     STORAGE CLOUD - GENERADOR");
            System.out.println("======================================");
            System.out.println("Iniciando generación de archivos...");

            List<Cuenta> cuentas = createAccountsFile(CANTIDAD_CUENTAS);

            createTransfersFile(
                    CANTIDAD_TRANSFERENCIAS,
                    cuentas
            );

            System.out.println();
            System.out.println("Archivos generados correctamente.");
            System.out.println("Archivo generado: " + ARCHIVO_CUENTAS);
            System.out.println("Archivo generado: " + ARCHIVO_TRANSFERENCIAS);
            System.out.println();
            System.out.println("Proceso finalizado exitosamente.");

        } catch (IOException exception) {

            System.err.println();
            System.err.println("ERROR: No fue posible generar los archivos.");
            System.err.println("Detalle: " + exception.getMessage());

        } catch (IllegalArgumentException exception) {

            System.err.println();
            System.err.println("ERROR: Datos de configuración incorrectos.");
            System.err.println("Detalle: " + exception.getMessage());
        }
    }

    /**
     * Genera el archivo cuentas.csv.
     *
     * <p>Formato:
     * <pre>
     * IDCuenta;Plan
     * CI10;Plan 50GB
     * CI11;Plan 100GB
     * </pre>
     *
     * @param cantidadCuentas cantidad de cuentas que se crearán
     * @return lista de cuentas generadas
     * @throws IOException si ocurre un error al escribir el archivo
     */
    public static List<Cuenta> createAccountsFile(int cantidadCuentas)
            throws IOException {

        if (cantidadCuentas <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de cuentas debe ser mayor que cero."
            );
        }

        List<Cuenta> cuentas = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(ARCHIVO_CUENTAS),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {

            for (int i = 0; i < cantidadCuentas; i++) {

                String idCuenta = "CI" + (10 + i);

                String plan = PLANES[
                        RANDOM.nextInt(PLANES.length)
                ];

                Cuenta cuenta = new Cuenta(idCuenta, plan);

                cuentas.add(cuenta);

                writer.write(cuenta.toCsv());
                writer.newLine();
            }
        }

        return cuentas;
    }

    /**
     * Genera el archivo transferencias.txt.
     *
     * <p>Formato:
     * <pre>
     * IDCuenta;TipoMovimiento;Cantidad
     * CI10;Subida;15GB
     * CI10;Bajada;5GB
     * </pre>
     *
     * <p>Las cuentas utilizadas en este archivo provienen de
     * cuentas.csv, por lo que no se generan identificadores inexistentes.
     *
     * @param cantidadTransferencias cantidad de movimientos que se crearán
     * @param cuentas lista de cuentas válidas
     * @throws IOException si ocurre un error al escribir el archivo
     */
    public static void createTransfersFile(
            int cantidadTransferencias,
            List<Cuenta> cuentas
    ) throws IOException {

        if (cantidadTransferencias <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de transferencias debe ser mayor que cero."
            );
        }

        if (cuentas == null || cuentas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe existir al menos una cuenta."
            );
        }

        /*
         * Almacenamos el uso actual de cada cuenta.
         * Esto permite evitar que una cuenta tenga un almacenamiento
         * negativo por una operación de bajada.
         */
        Map<String, Integer> almacenamientoActual = new HashMap<>();

        for (Cuenta cuenta : cuentas) {
            almacenamientoActual.put(cuenta.id(), 0);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(ARCHIVO_TRANSFERENCIAS),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {

            for (int i = 0; i < cantidadTransferencias; i++) {

                Cuenta cuenta = cuentas.get(
                        RANDOM.nextInt(cuentas.size())
                );

                String tipoMovimiento;

                int almacenamiento = almacenamientoActual.get(
                        cuenta.id()
                );

                /*
                 * Si la cuenta no tiene almacenamiento utilizado,
                 * solo se puede realizar una subida.
                 */
                if (almacenamiento == 0) {

                    tipoMovimiento = "Subida";

                } else {

                    tipoMovimiento = RANDOM.nextBoolean()
                            ? "Subida"
                            : "Bajada";
                }

                int cantidadGb = RANDOM.nextInt(20) + 1;

                /*
                 * Evitamos que una bajada deje el almacenamiento
                 * por debajo de cero.
                 */
                if (tipoMovimiento.equals("Bajada")
                        && cantidadGb > almacenamiento) {

                    cantidadGb = almacenamiento;
                }

                /*
                 * Si la cantidad resultante fuera cero, convertimos
                 * la operación en una subida.
                 */
                if (cantidadGb == 0) {

                    tipoMovimiento = "Subida";
                    cantidadGb = RANDOM.nextInt(20) + 1;
                }

                if (tipoMovimiento.equals("Subida")) {

                    almacenamiento += cantidadGb;

                } else {

                    almacenamiento -= cantidadGb;
                }

                almacenamientoActual.put(
                        cuenta.id(),
                        almacenamiento
                );

                writer.write(
                        cuenta.id()
                        + ";"
                        + tipoMovimiento
                        + ";"
                        + cantidadGb
                        + "GB"
                );

                writer.newLine();
            }
        }
    }

    /**
     * Representa una cuenta de almacenamiento.
     *
     * @param id identificador de la cuenta
     * @param plan plan de almacenamiento contratado
     */
    private record Cuenta(String id, String plan) {

        /**
         * Convierte la cuenta al formato CSV.
         *
         * @return representación de la cuenta como texto CSV
         */
        private String toCsv() {
            return id + ";" + plan;
        }
    }
}
    

