import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Programa principal del proyecto Storage Cloud.
 *
 * <p>Permite generar archivos de entrada, consultar su contenido,
 * calcular el almacenamiento utilizado por las cuentas y generar
 * un reporte de las cuentas que exceden su cuota contratada.
 *
 * @author Storage Cloud
 * @version 1.0
 */
public class Main {

    private static final String ARCHIVO_CUENTAS = "cuentas.csv";
    private static final String ARCHIVO_TRANSFERENCIAS = "transferencias.txt";
    private static final String ARCHIVO_SALIDA = "exceso.csv";

    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Punto de entrada del programa.
     *
     * @param args argumentos de ejecución
     */
    public static void main(String[] args) {

        boolean continuar = true;

        while (continuar) {

            mostrarMenu();

            String opcion = SCANNER.nextLine().trim();

            switch (opcion) {

                case "1" -> generarArchivos();

                case "2" -> importarYVerBase();

                case "3" -> importarYVerSalida();

                case "4" -> descargarSalida();

                case "5" -> {
                    continuar = false;
                    System.out.println("Storage Cloud finalizado.");
                }

                default -> System.out.println(
                        "Opción inválida. Seleccione una opción del 1 al 5."
                );
            }

            System.out.println();
        }
    }

    /**
     * Muestra el menú principal del sistema.
     */
    private static void mostrarMenu() {

        System.out.println("======================================");
        System.out.println("             STORAGE CLOUD");
        System.out.println("======================================");
        System.out.println("[1] Generar Archivos");
        System.out.println("[2] Importar/Ver Base");
        System.out.println("[3] Importar/Ver Salida");
        System.out.println("[4] Descargar Salida");
        System.out.println("[5] Salir");
        System.out.println("======================================");
        System.out.print("Seleccione una opción: ");
    }

    /**
     * Ejecuta la clase GenerateInfoFiles para generar los archivos
     * de entrada del sistema.
     */
    private static void generarArchivos() {

        System.out.println();
        System.out.println("Generando archivos de Storage Cloud...");

        GenerateInfoFiles.main(new String[0]);
    }

    /**
     * Muestra el contenido de los archivos de entrada.
     */
    private static void importarYVerBase() {

        System.out.println();
        System.out.println("========== BASE DE STORAGE CLOUD ==========");

        mostrarArchivo(ARCHIVO_CUENTAS);
        mostrarArchivo(ARCHIVO_TRANSFERENCIAS);
    }

    /**
     * Lee las cuentas y sus transferencias, calcula el almacenamiento
     * utilizado y genera el archivo exceso.csv.
     */
    private static void importarYVerSalida() {

        System.out.println();
        System.out.println("Procesando información de Storage Cloud...");

        try {

            Map<String, Cuenta> cuentas = cargarCuentas();

            int movimientosProcesados = procesarTransferencias(cuentas);

            int cantidadExcedidas = generarArchivoSalida(cuentas);

            System.out.println();
            System.out.println("Procesamiento finalizado correctamente.");
            System.out.println(
                    "Movimientos procesados: " + movimientosProcesados
            );
            System.out.println(
                    "Cuentas que exceden su cuota: " + cantidadExcedidas
            );
            System.out.println(
                    "Archivo generado: " + ARCHIVO_SALIDA
            );

            mostrarArchivo(ARCHIVO_SALIDA);

        } catch (IOException exception) {

            System.err.println(
                    "Error al procesar los archivos: "
                    + exception.getMessage()
            );

        } catch (IllegalArgumentException exception) {

            System.err.println(
                    "Error en los datos: " + exception.getMessage()
            );
        }
    }

    /**
     * Copia el archivo de salida a la ubicación indicada por el usuario.
     */
    private static void descargarSalida() {

        Path archivoOrigen = Path.of(ARCHIVO_SALIDA)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(archivoOrigen)) {

            System.out.println(
                    "El archivo exceso.csv todavía no existe."
            );
            System.out.println(
                    "Utilice primero la opción 3 para generar la salida."
            );

            return;
        }

        System.out.println();
        System.out.println("========== DESCARGAR SALIDA ==========");
        System.out.println(
                "Archivo disponible: " + archivoOrigen
        );
        System.out.println(
                "Escriba la ruta completa del archivo de destino."
        );
        System.out.println(
                "Ejemplo: C:\\Users\\Usuario\\Desktop\\exceso.csv"
        );
        System.out.print("Ruta de destino: ");

        String ruta = SCANNER.nextLine().trim();

        if (ruta.isBlank()) {
            System.out.println("La ruta no puede estar vacía.");
            return;
        }

        try {

            // Permite pegar una ruta rodeada de comillas.
            ruta = ruta.replace("\"", "");

            Path destino = Path.of(ruta)
                    .toAbsolutePath()
                    .normalize();

            if (archivoOrigen.equals(destino)) {

                System.out.println(
                        "El destino coincide con el archivo original."
                );

                return;
            }

            Path carpetaDestino = destino.getParent();

            if (carpetaDestino != null) {
                Files.createDirectories(carpetaDestino);
            }

            Files.copy(
                    archivoOrigen,
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println();
            System.out.println("Archivo copiado correctamente.");
            System.out.println("Ubicación: " + destino);

        } catch (IOException | IllegalArgumentException exception) {

            System.err.println(
                    "No fue posible copiar el archivo: "
                    + exception.getMessage()
            );
        }
    }

    /**
     * Carga las cuentas desde cuentas.csv.
     *
     * <p>Formato esperado: IDCuenta;Plan.
     *
     * @return mapa de cuentas indexado por su identificador
     * @throws IOException si el archivo no existe o no puede leerse
     */
    private static Map<String, Cuenta> cargarCuentas()
            throws IOException {

        Path archivo = Path.of(ARCHIVO_CUENTAS);

        if (!Files.isRegularFile(archivo)) {
            throw new IOException(
                    "No se encontró el archivo " + ARCHIVO_CUENTAS
                    + ". Genere primero los archivos de entrada."
            );
        }

        Map<String, Cuenta> cuentas = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(
                archivo,
                StandardCharsets.UTF_8
        )) {

            String linea;
            int numeroLinea = 0;

            while ((linea = reader.readLine()) != null) {

                numeroLinea++;

                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split(";", -1);

                if (datos.length != 2) {

                    System.err.println(
                            "Advertencia: formato incorrecto en cuentas.csv, "
                            + "línea " + numeroLinea
                    );

                    continue;
                }

                String idCuenta = datos[0].trim();
                String plan = datos[1].trim();

                try {

                    if (idCuenta.isBlank()) {
                        throw new IllegalArgumentException(
                                "El identificador de la cuenta está vacío."
                        );
                    }

                    int limiteGb = obtenerLimiteGb(plan);

                    if (limiteGb <= 0) {
                        throw new IllegalArgumentException(
                                "El límite debe ser mayor que cero."
                        );
                    }

                    if (cuentas.containsKey(idCuenta)) {

                        System.err.println(
                                "Advertencia: cuenta duplicada "
                                + idCuenta
                        );

                        continue;
                    }

                    Cuenta cuenta = new Cuenta(
                            idCuenta,
                            plan,
                            limiteGb
                    );

                    cuentas.put(idCuenta, cuenta);

                } catch (IllegalArgumentException exception) {

                    System.err.println(
                            "Advertencia en cuentas.csv, línea "
                            + numeroLinea + ": "
                            + exception.getMessage()
                    );
                }
            }
        }

        if (cuentas.isEmpty()) {
            throw new IOException(
                    "No se encontraron cuentas válidas en cuentas.csv."
            );
        }

        return cuentas;
    }

    /**
     * Procesa los movimientos registrados en transferencias.txt.
     *
     * <p>Las subidas aumentan el almacenamiento utilizado y las bajadas
     * lo disminuyen. El consumo nunca puede ser inferior a cero.
     *
     * @param cuentas cuentas previamente cargadas
     * @return cantidad de movimientos procesados correctamente
     * @throws IOException si el archivo no puede leerse
     */
    private static int procesarTransferencias(
            Map<String, Cuenta> cuentas
    ) throws IOException {

        Path archivo = Path.of(ARCHIVO_TRANSFERENCIAS);

        if (!Files.isRegularFile(archivo)) {
            throw new IOException(
                    "No se encontró el archivo "
                    + ARCHIVO_TRANSFERENCIAS
            );
        }

        int movimientosProcesados = 0;

        try (BufferedReader reader = Files.newBufferedReader(
                archivo,
                StandardCharsets.UTF_8
        )) {

            String linea;
            int numeroLinea = 0;

            while ((linea = reader.readLine()) != null) {

                numeroLinea++;

                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split(";", -1);

                if (datos.length != 3) {

                    System.err.println(
                            "Advertencia: transferencia incorrecta en la línea "
                            + numeroLinea
                    );

                    continue;
                }

                String idCuenta = datos[0].trim();
                String tipoMovimiento = datos[1].trim();
                String cantidadTexto = datos[2].trim();

                Cuenta cuenta = cuentas.get(idCuenta);

                if (cuenta == null) {

                    System.err.println(
                            "Advertencia: la cuenta " + idCuenta
                            + " no existe. Movimiento ignorado."
                    );

                    continue;
                }

                try {

                    int cantidadGb = convertirGb(cantidadTexto);

                    if (cantidadGb < 0) {

                        System.err.println(
                                "Advertencia: cantidad negativa en la línea "
                                + numeroLinea
                        );

                        continue;
                    }

                    if (tipoMovimiento.equalsIgnoreCase("Subida")) {

                        cuenta.subir(cantidadGb);

                    } else if (
                            tipoMovimiento.equalsIgnoreCase("Bajada")
                    ) {

                        cuenta.bajar(cantidadGb);

                    } else {

                        System.err.println(
                                "Advertencia: movimiento desconocido en la "
                                + "línea " + numeroLinea
                        );

                        continue;
                    }

                    movimientosProcesados++;

                } catch (NumberFormatException exception) {

                    System.err.println(
                            "Advertencia: cantidad no válida en la línea "
                            + numeroLinea
                    );
                }
            }
        }

        return movimientosProcesados;
    }

    /**
     * Genera exceso.csv con las cuentas cuyo almacenamiento utilizado
     * supera la capacidad del plan contratado.
     *
     * <p>Formato de cada registro:
     * IDCuenta;Plan;AlmacenamientoUtilizado;EXCEDE.
     *
     * @param cuentas cuentas procesadas
     * @return número de cuentas que exceden su cuota
     * @throws IOException si ocurre un error al escribir el reporte
     */
    private static int generarArchivoSalida(
            Map<String, Cuenta> cuentas
    ) throws IOException {

        List<Cuenta> cuentasExcedidas = new ArrayList<>();

        for (Cuenta cuenta : cuentas.values()) {

            if (cuenta.excedeCuota()) {
                cuentasExcedidas.add(cuenta);
            }
        }

        cuentasExcedidas.sort(
                Comparator.comparingInt(Cuenta::obtenerExcesoGb)
                        .reversed()
        );

        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(ARCHIVO_SALIDA),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {

            for (Cuenta cuenta : cuentasExcedidas) {

                writer.write(
                        cuenta.id()
                        + ";"
                        + cuenta.plan()
                        + ";"
                        + cuenta.almacenamientoUsado()
                        + "GB;EXCEDE"
                );

                writer.newLine();
            }
        }

        return cuentasExcedidas.size();
    }

    /**
     * Extrae el límite numérico de un plan.
     *
     * <p>Ejemplo: "Plan 50GB" produce 50.
     *
     * @param plan nombre del plan
     * @return capacidad del plan en GB
     */
    private static int obtenerLimiteGb(String plan) {

        String numero = plan.replaceAll("[^0-9]", "");

        if (numero.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar la capacidad del plan: " + plan
            );
        }

        return Integer.parseInt(numero);
    }

    /**
     * Convierte una cantidad de almacenamiento a GB.
     *
     * <p>Admite valores como "15GB" o "15".
     *
     * @param cantidad texto con la cantidad
     * @return cantidad en GB
     */
    private static int convertirGb(String cantidad) {

        String numero = cantidad
                .replaceAll("(?i)GB", "")
                .trim();

        return Integer.parseInt(numero);
    }

    /**
     * Muestra el contenido de un archivo de texto.
     *
     * @param nombreArchivo nombre del archivo que se mostrará
     */
    private static void mostrarArchivo(String nombreArchivo) {

        Path archivo = Path.of(nombreArchivo);

        System.out.println();
        System.out.println("---------- " + nombreArchivo + " ----------");

        if (!Files.isRegularFile(archivo)) {

            System.out.println("El archivo todavía no existe.");

            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(
                archivo,
                StandardCharsets.UTF_8
        )) {

            String linea;
            boolean tieneContenido = false;

            while ((linea = reader.readLine()) != null) {

                System.out.println(linea);
                tieneContenido = true;
            }

            if (!tieneContenido) {
                System.out.println("(El archivo está vacío.)");
            }

        } catch (IOException exception) {

            System.err.println(
                    "Error al leer " + nombreArchivo + ": "
                    + exception.getMessage()
            );
        }
    }

    /**
     * Representa una cuenta de almacenamiento de Storage Cloud.
     *
     * <p>La clase mantiene el identificador, el plan contratado,
     * el límite de almacenamiento y el uso actual.
     */
    private static class Cuenta {

        private final String id;
        private final String plan;
        private final int limiteGb;

        private int almacenamientoUsado;

        /**
         * Construye una cuenta de almacenamiento.
         *
         * @param id identificador de la cuenta
         * @param plan plan contratado
         * @param limiteGb capacidad máxima del plan
         */
        private Cuenta(String id, String plan, int limiteGb) {

            this.id = id;
            this.plan = plan;
            this.limiteGb = limiteGb;
            this.almacenamientoUsado = 0;
        }

        /**
         * Registra una subida de archivos.
         *
         * @param gb cantidad de GB subidos
         */
        private void subir(int gb) {

            almacenamientoUsado = Math.addExact(
                    almacenamientoUsado,
                    gb
            );
        }

        /**
         * Registra una bajada de archivos.
         *
         * <p>Si se intenta retirar más almacenamiento del utilizado,
         * el uso queda en cero.
         *
         * @param gb cantidad de GB retirados
         */
        private void bajar(int gb) {

            almacenamientoUsado = Math.max(
                    0,
                    almacenamientoUsado - gb
            );
        }

        /**
         * Determina si la cuenta supera el límite de su plan.
         *
         * @return true si excede la cuota; false en caso contrario
         */
        private boolean excedeCuota() {

            return almacenamientoUsado > limiteGb;
        }

        /**
         * Calcula cuántos GB supera la cuenta su límite.
         *
         * @return cantidad de GB excedidos
         */
        private int obtenerExcesoGb() {

            return Math.max(
                    0,
                    almacenamientoUsado - limiteGb
            );
        }

        private String id() {
            return id;
        }

        private String plan() {
            return plan;
        }

        private int almacenamientoUsado() {
            return almacenamientoUsado;
        }
    }
}
    
