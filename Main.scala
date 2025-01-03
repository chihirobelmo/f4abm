import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

    runGame()

    GLFW.glfwDestroyWindow(window)
    GLFW.glfwTerminate()

    def runGame(): Unit = {
        val primitive = new Triangle()

        // ループ
        while (!GLFW.glfwWindowShouldClose(window)) {
            gameLoop(primitive)
        }

        // 終了処理
        primitive.end()
    }

    def gameLoop(primitive: Renderable): Unit = {
        
        GL11.glClearColor(0.1f, 0.2f, 0.3f, 0.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

        primitive.render()

        GLFW.glfwSwapBuffers(window)
        GLFW.glfwPollEvents()
    }
}