import akka.actor.ActorSystem
import java.net.{Socket, InetSocketAddress}
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap

object ThreadState extends Enumeration {
    type ThreadState = Value
    val CONNECTING, CONNECTED, FAILED, DISCONNECTED, TERMINATED = Value
}

class TrttClient {
    implicit val system: ActorSystem = ActorSystem("PeriodicTaskSystem")
    @volatile private var connecting: Boolean = false
    @volatile private var connected: Boolean = false
    @volatile private var quit: Boolean = false
    private var clientsocket: Option[Socket] = None
    private var server: Option[(String, Int)] = None
    private val numRetries: Int = 5
    private var retries: Int = 0
    private val idMap = HashMap[String, Map[String, String]]()
    private var timestamp: String = ""

    startPeriodicTask(2.seconds)

    private def startPeriodicTask(interval: FiniteDuration): Unit = {
        system.scheduler.scheduleWithFixedDelay(0.seconds, interval) { () =>
            {
                idMap.foreach { case (id, attributes) =>
                    print(s"ID: $id")
                    attributes.foreach { case (key, value) => print(s", $key: $value") }
                    println()
                }
            }
        }
    }

    private def setStatus(state: ThreadState.ThreadState, message: String): Unit = {
        println(s"Status: $state, Message: $message")
    }

    private def performHandshake(buf: BufferedReader, username: String): Boolean = {
        val handshake = s"XtraLib.Stream.0\nTacview.RealTimeTelemetry.0\nClient $username\n\u0000"
        val out = new PrintWriter(clientsocket.get.getOutputStream, true)
        out.print(handshake)
        out.flush()

        val response = buf.readLine()
        if (response != null && response.startsWith("XtraLib.Stream.0") 
                             && buf.readLine.startsWith("Tacview.RealTimeTelemetry.0")) {
            connected = true
            true
        } else {
            println(s"Handshake request: $handshake")
            println(s"Handshake response: $response")
            false
        }
    }

    private def processData(buf: BufferedReader): Unit = {
        try {
            var data: String = null
            while (connected && { data = buf.readLine(); data != null }) {
                // Process each line of data here
                val lines = data.split("\u000A")
                lines.foreach { line => parseLine(line) }
                //println(s"Received: $data")
            }
        } catch {
            case e: Exception =>
                setStatus(ThreadState.FAILED, s"Error processing data: ${e.getMessage}")
                disconnect()
        }
    }

    def parseLine(line: String): Unit = {
        
        if (line.startsWith("#")) {
            // time stamp line: #123456.789
            timestamp = line.stripPrefix("#").toString
        } else {
            // ID and attributes line: 1,attr1=val1,attr2=val2
            val parts = line.split(",")

            if (parts.length <= 1) {
                println(s"LINE: $line")
                return
            }
            
            val id = parts(0)

            val attributes = parts.tail.map { attr =>
                val Array(key, value) = attr.split("=")
                key -> value
            }.toMap

            idMap += (id -> (attributes + ("TimeStamp" -> timestamp)))
        }
    }

    def connect(server: String, port: Int): Boolean = {
        if (connected) return false
        clientsocket = Some(new Socket())
        this.server = Some((server, port))
        connecting = true
        true
    }

    def disconnect(): Unit = {
        clientsocket.foreach(_.close())
        clientsocket = None
        connected = false
        connecting = false
    }

    def stop(): Unit = {
        quit = true
        disconnect()
    }

    def run(): Unit = {
        while (!quit) {
        if (connecting) {
            setStatus(ThreadState.CONNECTING, "Trying connection")

            (clientsocket, server) match {
            case (Some(socket), Some((host, port))) if retries < numRetries || numRetries == 0 =>
                try {
                socket.connect(new InetSocketAddress(host, port))
                connecting = false
                connected = true
                } catch {
                case _: java.net.ConnectException =>
                    retries += 1
                    setStatus(ThreadState.CONNECTING, s"Connection refused, retrying in 10 seconds $retries/$numRetries")
                    Thread.sleep(10000)
                }
            case _ =>
                setStatus(ThreadState.FAILED, "Failed to connect to server")
                connecting = false
                connected = false
            }
        }

        if (connected && clientsocket.isDefined) {
            val buf = new BufferedReader(new InputStreamReader(clientsocket.get.getInputStream))

            if (!performHandshake(buf, "F4ABM")) {
                setStatus(ThreadState.FAILED, "Tacview Handshake failed")
                disconnect()
                return
            }

            setStatus(ThreadState.CONNECTED, "")
            // Post RADAR_SERVER_CONNECTED event here
            processData(buf) // blocking call
            setStatus(ThreadState.DISCONNECTED, "Disconnected from server")
            // Post RADAR_SERVER_DISCONNECTED event here
        }

        Thread.sleep(1000)
        }
    }
}