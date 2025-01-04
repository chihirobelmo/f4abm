import com.example.render.{Renderable, Triangle, AirTrack, FontRenderer, Camera}
import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import org.joml.Vector3f
import org.joml.Vector2f

object Main extends App {

    // 初期化
    if (!GLFW.glfwInit()) {
        throw new IllegalStateException("GLFWの初期化に失敗しました")
    }

    // ウィンドウの設定
    GLFW.glfwDefaultWindowHints()
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)

    // ウィンドウ作成
    val window = GLFW.glfwCreateWindow(800, 600, "Scala LWJGL Input Example", MemoryUtil.NULL, MemoryUtil.NULL)
    if (window == MemoryUtil.NULL) {
        throw new RuntimeException("ウィンドウの作成に失敗しました")
    }

    // コンテキストを現在のスレッドに設定
    GLFW.glfwMakeContextCurrent(window)
    GL.createCapabilities()

    // ウィンドウを表示
    GLFW.glfwShowWindow(window)

    // Create and start TrttClient
    val trttClient = new TrttClient()
    trttClient.connect("127.0.0.1", 10308)
    Future {
        trttClient.run()
    }

    val camera = new Camera()
    var shouldMoveCamera = false
    var prevCursor: Vector2f = new Vector2f(0.0f, 0.0f)

    runGame()

    GLFW.glfwDestroyWindow(window)
    GLFW.glfwTerminate()

    def runGame(): Unit = {
        val primitive = new Triangle()

        // ループ
        while (!GLFW.glfwWindowShouldClose(window)) {

            GL11.glClearColor(0.1f, 0.2f, 0.3f, 0.0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

            GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) => {
                action match {
                    case GLFW.GLFW_PRESS => {
                        println(s"キー $key が押されました")
                        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                            GLFW.glfwSetWindowShouldClose(window, true)
                        }
                    }
                    case GLFW.GLFW_RELEASE => println(s"キー $key が離されました")
                    case _ =>
                }
            })
            GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) => {
                action match {
                    case GLFW.GLFW_PRESS => {
                        shouldMoveCamera = true
                    }
                    case GLFW.GLFW_RELEASE => {
                        shouldMoveCamera = false
                    }
                    case _ =>
                }
            })
            GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) => {
                if (shouldMoveCamera) {
                    camera.translate(prevCursor.x - xpos.toFloat, prevCursor.y - ypos.toFloat)
                }
                prevCursor = new Vector2f(xpos.toFloat, ypos.toFloat)
            })

            primitive
            .srt(1.0f, 90.0f, new Vector3f(0.5f, 0.5f, 0.0f))
            .camera(camera)
            .preRender()
            .render()

            // 文字列を描画
            new FontRenderer(0f, 0f, "HELLO WORLD\n1234567890 abcdef", 800, 600)
            .srt(2.0f,0.0f,new Vector3f(-1.0f, 0.0f, 0.0f))
            .camera(camera)
            .preRender()
            .render()

            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }

        // 終了処理
        primitive.end()
    }
}