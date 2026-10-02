import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Servidor de mensajería privada por consola.
 * - Acepta varios clientes al mismo tiempo (un hilo por cliente).
 * - Cada cliente se registra con un nombre de usuario único.
 * - Reenvía los mensajes "@destinatario mensaje" solo al destinatario.
 * - Detecta desconexiones ("/salir" o cierre inesperado) y sigue atendiendo a los demás.
 *
 * Protocolo:
 *   1) Primera línea del cliente: su nombre de usuario.
 *   2) Mensaje privado: "@destinatario mensaje".
 *   3) Salir: "/salir".
 */
public class ServidorChat {

    // Puerto en el que escucha el servidor
    private static final int PUERTO = 5000;

    // Lista de usuarios conectados: nombre -> flujo de salida hacia ese cliente.
    // Está sincronizada porque varios hilos la modifican a la vez.
    private static final Map<String, PrintWriter> usuarios =
            Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor escuchando en el puerto " + PUERTO + "...");

            // Bucle principal: acepta clientes indefinidamente
            while (true) {
                // accept() es bloqueante: espera hasta que un cliente se conecte
                Socket cliente = servidor.accept();
                System.out.println("Nueva conexion desde " + cliente.getInetAddress());

                // Un hilo propio por cliente, para atender a varios simultáneamente
                Thread hilo = new Thread(() -> atenderCliente(cliente));
                hilo.start();
            }
        } catch (IOException e) {
            System.out.println("No se pudo iniciar el servidor en el puerto "
                    + PUERTO + ": " + e.getMessage());
        }
    }

    /**
     * Atiende a un cliente durante toda su sesión: registro, mensajes y desconexión.
     * Se ejecuta en un hilo independiente por cada cliente.
     */
    private static void atenderCliente(Socket cliente) {
        String nombreUsuario = null;
        boolean registrado = false;

        try {
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream()));
            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

            // 1. La primera línea enviada por el cliente es su nombre de usuario
            nombreUsuario = entrada.readLine();

            // Validar nombre: no nulo, no vacío y sin espacios
            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()
                    || nombreUsuario.trim().contains(" ")) {
                salida.println("ERROR: nombre invalido (no puede estar vacio ni tener espacios). Cerrando conexion.");
                nombreUsuario = null;
                return; // el finally cierra el socket
            }
            nombreUsuario = nombreUsuario.trim();

            // 2. Registrar el nombre; si ya existe, se rechaza.
            // synchronized evita que dos clientes tomen el mismo nombre a la vez.
            synchronized (usuarios) {
                if (usuarios.containsKey(nombreUsuario)) {
                    salida.println("ERROR: el nombre '" + nombreUsuario
                            + "' ya esta en uso. Cerrando conexion.");
                    nombreUsuario = null; // no se registró, no hay que quitarlo después
                    return;
                }
                usuarios.put(nombreUsuario, salida);
                registrado = true;
            }

            System.out.println(nombreUsuario + " se conecto. Usuarios conectados: "
                    + usuarios.keySet());
            salida.println("Bienvenido, " + nombreUsuario
                    + ". Usa el formato @usuario mensaje para escribir a alguien.");

            // 3. Leer mensajes hasta que el cliente salga o se desconecte
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.trim().equalsIgnoreCase("/salir")) {
                    break; // el cliente pidió salir
                }
                procesarMensaje(nombreUsuario, linea, salida);
            }
            // Si readLine() devuelve null, el cliente cerró la conexión sin avisar

        } catch (IOException e) {
            // Cierre inesperado del cliente (por ejemplo, cerró la terminal)
            System.out.println("Conexion perdida con "
                    + (nombreUsuario != null ? nombreUsuario : "un cliente")
                    + ": " + e.getMessage());
        } finally {
            // 4. Limpieza: retirar al usuario de la lista y cerrar el socket.
            // Los demás clientes siguen siendo atendidos por sus propios hilos.
            if (registrado && nombreUsuario != null) {
                usuarios.remove(nombreUsuario);
                System.out.println(nombreUsuario + " se desconecto. Usuarios conectados: "
                        + usuarios.keySet());
            }
            try {
                cliente.close();
            } catch (IOException e) {
                // ya estaba cerrado
            }
        }
    }

    /**
     * Valida el formato "@destinatario mensaje" y reenvía el mensaje
     * únicamente al destinatario, anteponiendo el nombre del remitente.
     */
    private static void procesarMensaje(String remitente, String linea,
                                        PrintWriter salidaRemitente) {
        // Debe empezar con @
        if (!linea.startsWith("@")) {
            salidaRemitente.println("Formato invalido. Usa: @usuario mensaje");
            return;
        }

        // Debe haber un espacio que separe destinatario y mensaje
        int espacio = linea.indexOf(' ');
        if (espacio == -1) {
            salidaRemitente.println("Formato invalido. Usa: @usuario mensaje");
            return;
        }

        String destinatario = linea.substring(1, espacio);
        String mensaje = linea.substring(espacio + 1).trim();

        if (destinatario.isEmpty() || mensaje.isEmpty()) {
            salidaRemitente.println("Formato invalido. Usa: @usuario mensaje");
            return;
        }

        // Buscar al destinatario en la lista de conectados
        PrintWriter salidaDestino = usuarios.get(destinatario);
        if (salidaDestino == null) {
            // Informar al remitente que el destinatario no existe o no está conectado
            salidaRemitente.println("El usuario '" + destinatario
                    + "' no existe o no esta conectado.");
            return;
        }

        // Enviar solo al destinatario, anteponiendo el nombre del remitente.
        // synchronized evita que dos hilos escriban al mismo tiempo en el mismo flujo.
        synchronized (salidaDestino) {
            salidaDestino.println(remitente + " (privado): " + mensaje);
        }
        System.out.println(remitente + " -> " + destinatario + ": " + mensaje);
    }
}