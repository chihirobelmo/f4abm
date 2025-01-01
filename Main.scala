import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.opengl._
import org.lwjgl.system.MemoryUtil

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
  val window = GLFW.glfwCreateWindow(800, 600, "Scala LWJGL Window", MemoryUtil.NULL, MemoryUtil.NULL)
  if (window == MemoryUtil.NULL) {
    throw new RuntimeException("ウィンドウの作成に失敗しました")
  }

  // コンテキストを現在のスレッドに設定
  GLFW.glfwMakeContextCurrent(window)
  GL.createCapabilities()

  // ウィンドウを表示
  GLFW.glfwShowWindow(window)

  // ループ
  while (!GLFW.glfwWindowShouldClose(window)) {
    // 背景色を設定
    GL11.glClearColor(0.1f, 0.2f, 0.3f, 0.0f)
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

    // バッファのスワップ
    GLFW.glfwSwapBuffers(window)

    // イベントの処理
    GLFW.glfwPollEvents()
  }

  // 終了処理
  GLFW.glfwDestroyWindow(window)
  GLFW.glfwTerminate()
}