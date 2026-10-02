# Servidor de mensajería privada con sockets

Servidor por consola en Java (solo `java.io`, `java.net` y `java.util`).
Atiende a varios clientes al mismo tiempo, cada uno en su propio hilo.

## Requisitos
- JDK 11 o superior (`java -version` y `javac -version` para verificar)

## Puerto utilizado
**5000** (definido en la constante `PUERTO` de `ServidorChat.java`)

## Compilar
```
javac ServidorChat.java
```

## Ejecutar
```
java ServidorChat
```
Debe aparecer: `Servidor escuchando en el puerto 5000...`

Después, conecta los clientes (por ejemplo `java ClienteChat`) usando la IP del equipo donde corre el servidor y el puerto 5000.

## Protocolo
| Mensaje | Significado |
|---|---|
| Primera línea del cliente | Nombre de usuario (único, sin espacios) |
| `@destinatario mensaje` | Mensaje privado, se reenvía solo al destinatario |
| `/salir` | El cliente se desconecta |

## Comportamiento
- Si el nombre ya está en uso o es inválido, se rechaza la conexión.
- Si el destinatario no existe o no está conectado, se avisa al remitente.
- Si un cliente se desconecta (con `/salir` o cerrando la terminal), se retira de la lista y los demás siguen funcionando.

## Pruebas
Las capturas con 3 clientes (mensaje privado entregado, mensaje a usuario inexistente y desconexión de un cliente) están en la carpeta `capturas/`.
