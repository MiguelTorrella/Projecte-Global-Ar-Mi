import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

private const val LOCAL_PORT = 9000
private const val TELLO_IP = "192.168.10.1"
private const val TELLO_PORT = 8889
private const val RECEIVE_BUFFER_SIZE = 1518

fun main() {
    val telloAddress = InetAddress.getByName(TELLO_IP)
    val socket = DatagramSocket(LOCAL_PORT)

    // AtomicBoolean permite compartir el estado entre hilos de forma segura.
    val running = AtomicBoolean(true)

    val receiverThread = Thread {
        val buffer = ByteArray(RECEIVE_BUFFER_SIZE)

        while (running.get()) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val response = String(
                    packet.data,
                    packet.offset,
                    packet.length,
                    Charsets.UTF_8
                )
                println(response)
            } catch (e: SocketException) {
                // Al cerrar el socket, receive() se desbloquea.
                if (running.get()) {
                    System.err.println("Error de socket: ${e.message}")
                }
                break
            } catch (e: Exception) {
                if (running.get()) {
                    System.err.println("Error al recibir datos: ${e.message}")
                }
            }
        }

        println("\nExit...\n")
    }

    receiverThread.isDaemon = true
    receiverThread.start()

    println("\r\nTello Kotlin Demo.\r\n")
    println("Tello: command takeoff land flip forward back left right")
    println("       up down cw ccw speed speed?\r\n")
    println("end -- quit demo.\r\n")

    try {
        while (true) {
            val message = readLine() ?: break

            if (message.isBlank()) {
                break
            }

            if (message.contains("end", ignoreCase = true)) {
                println("...")
                break
            }

            val data = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                data,
                data.size,
                telloAddress,
                TELLO_PORT
            )
            socket.send(packet)
        }
    } catch (e: Exception) {
        System.err.println("Error al enviar el comando: ${e.message}")
    } finally {
        running.set(false)
        socket.close()
        receiverThread.join(1000)
    }
}
