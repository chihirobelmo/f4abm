import com.example.render.{Renderable, Triangle, AirTrack, FontRenderer, Camera}
import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.HashMap
import org.joml.{Vector2f, Vector3f, Vector4f, Matrix4f}

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
    var prevTimeMs = GLFW.glfwGetTime()

    runGame()

    GLFW.glfwDestroyWindow(window)
    GLFW.glfwTerminate()

    def runGame(): Unit = {
        val primitive = new Triangle()

        // ループ
        while (!GLFW.glfwWindowShouldClose(window)) {

            val frameTimeMs = GLFW.glfwGetTime() - prevTimeMs

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
                        val worldPos = screenToWorld(prevCursor, window, camera)
                        println(s"クリック位置: $worldPos")
                    }
                    case GLFW.GLFW_RELEASE => {
                        shouldMoveCamera = false
                    }
                    case _ =>
                }
            })
            GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) => {
                if (shouldMoveCamera) {
                    camera.translate((prevCursor.x - xpos.toFloat) * 10.0f * frameTimeMs.toFloat, +(prevCursor.y - ypos.toFloat) * 10.0f * frameTimeMs.toFloat)
                }
                prevCursor = new Vector2f(xpos.toFloat, ypos.toFloat)
            })
            GLFW.glfwSetScrollCallback(window, (window, xoffset, yoffset) => {
                camera.zoom(yoffset.toFloat)
            })
            GLFW.glfwSetWindowSizeCallback(window, (window, width, height) => {
                GL11.glViewport(0, 0, width, height)
                camera.setProjectionMatrix(45.0f, width.toFloat / height.toFloat, 0.1f, 100.0f).update()
            })

            primitive
            .srt(1.0f, 90.0f, new Vector3f(0.5f, 0.5f, 0.0f))
            .camera(camera)
            .preRender()
            .render()

            val width = Array(0)
            val height = Array(0)
            GLFW.glfwGetWindowSize(window, width, height)

            new FontRenderer(0f, 0f, "HELLO WORLD\n1234567890 abcdef", width(0), height(0))
            .srt(2.0f,0.0f,new Vector3f(-1.0f, 0.0f, 0.0f))
            .camera(camera)
            .preRender()
            .render()

            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()

            prevTimeMs = GLFW.glfwGetTime()
        }

        // 終了処理
        primitive.end()
    }

    def screenToWorld(screenPos: Vector2f, window: Long, camera: Camera): Vector3f = {
        val width = Array(0)
        val height = Array(0)
        GLFW.glfwGetWindowSize(window, width, height)

        val normalizedX = (2.0f * screenPos.x) / width(0) - 1.0f
        val normalizedY = 1.0f - (2.0f * screenPos.y) / height(0)

        val clipCoords = new Vector4f(normalizedX, normalizedY, -1.0f, 1.0f)

        val invProjMatrix = new Matrix4f(camera.getProjectionMatrix()).invert()
        val eyeCoords = invProjMatrix.transform(clipCoords)
        eyeCoords.z = 0.0f
        eyeCoords.w = 0.0f

        val invViewMatrix = new Matrix4f(camera.getViewMatrix()).invert()
        val worldCoords = invViewMatrix.transform(eyeCoords)

        new Vector3f(worldCoords.x, worldCoords.y, worldCoords.z)
    }
}