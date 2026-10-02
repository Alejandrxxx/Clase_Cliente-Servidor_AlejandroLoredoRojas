# Cliente de mensajería privada con sockets

Cliente por consola en Java (solo `java.io`, `java.net` y `java.util`).

## Requisitos
- JDK 11 o superior (`java -version` y `javac -version` para verificar)
- Un servidor de chat en ejecución (por ejemplo `servidorChat`, puerto 5000)

## Compilar
```
javac ClienteChat.java
```

## Ejecutar
1. Inicia el servidor: `java servidorChat`
2. En otra terminal: `java ClienteChat`
3. El programa pide:
   - IP del servidor (Enter para `localhost`)
   - Puerto (Enter para `5000`)
   - Nombre de usuario (se envía al servidor como primera línea)
4. Abre una segunda terminal y ejecuta otro cliente con un nombre distinto.

## Uso
| Comando | Acción |
|---|---|
| `@destinatario mensaje` | Envía un mensaje privado (ej. `@ana hola, ¿ya terminaste?`) |
| `/salir` | Cierra la conexión y termina el programa |

## Pruebas
Las capturas de pantalla de dos clientes intercambiando mensajes están en la carpeta `capturas/`.
