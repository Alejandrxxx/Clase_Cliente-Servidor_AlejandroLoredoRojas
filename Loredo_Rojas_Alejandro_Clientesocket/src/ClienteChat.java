import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Cliente de mensajería privada por consola.
 * - Se conecta a un servidor por sockets (IP y puerto configurables).
 * - Envía el nombre de usuario como primera línea.
 * - Envía mensajes con el formato "@destinatario mensaje".
 * - Un hilo receptor imprime los mensajes entrantes en cualquier momento.
 * - Termina con "/salir" cerrando socket y flujos.
 */
public class ClienteChat {

    // Valores por defecto de conexión
    private static final String HOST_POR_DEFECTO = "localhost";
    private static final int PUERTO_POR_DEFECTO = 5000;

    // Bandera compartida entre hilos: indica si el cliente sigue activo
    private static volatile boolean activo = true;

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);

        // 1. Pedir IP y puerto (Enter = valor por defecto)
        System.out.print("IP del servidor [" + HOST_POR_DEFECTO + "]: ");
        String host = teclado.nextLine().trim();
        if (host.isEmpty()) {
            host = HOST_POR_DEFECTO;
        }

        int puerto = PUERTO_POR_DEFECTO;
        System.out.print("Puerto [" + PUERTO_POR_DEFECTO + "]: ");
        String textoPuerto = teclado.nextLine().trim();
        if (!textoPuerto.isEmpty()) {
            try {
                puerto = Integer.parseInt(textoPuerto);
            } catch (NumberFormatException e) {
                System.out.println("Puerto invalido, se usara " + PUERTO_POR_DEFECTO);
            }
        }

        // 2. Pedir nombre de usuario
        String nombre = "";
        while (nombre.isEmpty()) {
            System.out.print("Nombre de usuario: ");
            nombre = teclado.nextLine().trim();
        }

        Socket socket = null;
        BufferedReader entrada = null;
        PrintWriter salida = null;

        try {
            // 3. Conectar con el servidor
            System.out.println("Conectando a " + host + ":" + puerto + "...");
            socket = new Socket(host, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // 4. Primera linea enviada: el nombre de usuario
            salida.println(nombre);

            // 5. Instrucciones de uso
            mostrarInstrucciones(nombre);

            // 6. Hilo receptor: imprime lo que llegue del servidor en cualquier momento
            final BufferedReader entradaFinal = entrada;
            final Socket socketFinal = socket;
            Thread receptor = new Thread(() -> recibirMensajes(entradaFinal, socketFinal));
            receptor.setDaemon(true); // no impide que el programa termine
            receptor.start();

            // 7. Bucle principal: leer del teclado y enviar al servidor
            while (activo) {
                if (!teclado.hasNextLine()) {
                    break; // fin de la entrada (Ctrl+D / Ctrl+Z)
                }
                String linea = teclado.nextLine().trim();

                if (!activo) {
                    break; // la conexion se perdio mientras se escribia
                }
                if (linea.isEmpty()) {
                    continue;
                }
                if (linea.equalsIgnoreCase("/salir")) {
                    salida.println("/salir"); // avisa al servidor
                    System.out.println("Saliendo del chat...");
                    activo = false;
                    break;
                }
                if (!linea.startsWith("@") || !linea.contains(" ")) {
                    System.out.println("Formato invalido. Usa: @destinatario mensaje");
                    continue;
                }

                salida.println(linea);
                // PrintWriter no lanza excepciones: se revisa si hubo error de escritura
                if (salida.checkError()) {
                    System.out.println("Error: se perdio la conexion con el servidor.");
                    activo = false;
                    break;
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Error: no se encontro el host '" + host + "'.");
        } catch (ConnectException e) {
            System.out.println("Error: el servidor no esta disponible en " + host + ":" + puerto + ".");
        } catch (IOException e) {
            System.out.println("Error de conexion: " + e.getMessage());
        } finally {
            // 8. Cierre correcto de flujos y socket
            activo = false;
            try {
                if (entrada != null) entrada.close();
            } catch (IOException e) { /* ya cerrado */ }
            if (salida != null) salida.close();
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { /* ya cerrado */ }
            teclado.close();
            System.out.println("Conexion cerrada.");
        }
    }

    /** Imprime las instrucciones de uso al iniciar. */
    private static void mostrarInstrucciones(String nombre) {
        System.out.println();
        System.out.println("=== Conectado como " + nombre + " ===");
        System.out.println("Instrucciones:");
        System.out.println("  @destinatario mensaje  -> enviar un mensaje privado (ej: @ana hola)");
        System.out.println("  /salir                 -> cerrar la conexion y salir");
        System.out.println("===================================");
    }

    /**
     * Se ejecuta en un hilo aparte. Lee lineas del servidor y las imprime.
     * Si readLine() devuelve null, el servidor cerro la conexion.
     */
    private static void recibirMensajes(BufferedReader entrada, Socket socket) {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                System.out.println(mensaje);
            }
            // Llego null: el servidor cerro la conexion
            if (activo) {
                System.out.println("Se perdio la conexion con el servidor. Presiona Enter para salir.");
            }
        } catch (IOException e) {
            // Si activo es false, el cierre fue provocado por el propio cliente
            if (activo) {
                System.out.println("Se perdio la conexion con el servidor. Presiona Enter para salir.");
            }
        } finally {
            activo = false;
        }
    }
}