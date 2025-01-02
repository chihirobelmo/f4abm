import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.opengl._
import org.lwjgl.system.MemoryUtil

class Primitive(vertices: Array[Float]) {
    
    val vaoId = createVao()
    val vboId = createVbo(vertices)

    // 頂点シェーダーコード
    val vertexShaderSource =
    """
    #version 330 core
    layout(location = 0) in vec3 aPos;
    void main() {
        gl_Position = vec4(aPos, 1.0);
    }
    """

    // フラグメントシェーダーコード
    val fragmentShaderSource =
    """
    #version 330 core
    out vec4 FragColor;
    void main() {
        FragColor = vec4(1.0, 0.5, 0.2, 1.0);
    }
    """

    // シェーダープログラムの作成
    val vertexShader = compileShader(vertexShaderSource, GL20.GL_VERTEX_SHADER)
    val fragmentShader = compileShader(fragmentShaderSource, GL20.GL_FRAGMENT_SHADER)

    val shaderProgram = GL20.glCreateProgram()

    private def createVao(): Int = {

        // VAO作成
        val vaoId = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vaoId)

        vaoId
    }

    private def createVbo(vertices: Array[Float]): Int = {

        // VBO作成
        val vboId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

        // 頂点属性の設定
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0)
        GL20.glEnableVertexAttribArray(0)

        // バインド解除
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)

        vboId
    }

    private def compileShader(source: String, shaderType: Int): Int = {
        val shader = GL20.glCreateShader(shaderType)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)

        // コンパイルエラーの確認
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(s"シェーダーのコンパイルに失敗: ${GL20.glGetShaderInfoLog(shader)}")
        }
        shader
    }

    def render() {

        GL20.glAttachShader(shaderProgram, vertexShader)
        GL20.glAttachShader(shaderProgram, fragmentShader)
        GL20.glLinkProgram(shaderProgram)

        // シェーダープログラムを使用
        GL20.glUseProgram(shaderProgram)

        // VAOをバインドして描画
        GL30.glBindVertexArray(vaoId)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3)

        GL30.glBindVertexArray(0)
    }

    def end() {
        GL15.glDeleteBuffers(vboId)
        GL30.glDeleteVertexArrays(vaoId)
    }
}

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
    val window_ = GLFW.glfwCreateWindow(800, 600, "Scala LWJGL Input Example", MemoryUtil.NULL, MemoryUtil.NULL)
    if (window_ == MemoryUtil.NULL) {
        throw new RuntimeException("ウィンドウの作成に失敗しました")
    }

    // コンテキストを現在のスレッドに設定
    GLFW.glfwMakeContextCurrent(window_)
    GL.createCapabilities()

    // ウィンドウを表示
    GLFW.glfwShowWindow(window_)

    runGame()

    GLFW.glfwDestroyWindow(window_)
    GLFW.glfwTerminate()

    def runGame() {

        // 頂点データ
        val vertices: Array[Float] = Array(
            -0.5f, -0.5f, +0.0f, // 左下
            +0.5f, -0.5f, +0.0f, // 右下
            +0.0f, +0.5f, +0.0f  // 上
        )
        
        val primitive = new Primitive(vertices)

        // ループ
        while (!GLFW.glfwWindowShouldClose(window_)) {
            gameLoop(primitive)
        }

        // 終了処理
        primitive.end()
    }

    def keyboardAndMouse() {
        GLFW.glfwSetKeyCallback(window_, (window, key, scancode, action, mods) => {
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

        GLFW.glfwSetMouseButtonCallback(window_, (window, button, action, mods) => {
            action match {
                case GLFW.GLFW_PRESS => println(s"マウスボタン $button が押されました")
                case GLFW.GLFW_RELEASE => println(s"マウスボタン $button が離されました")
                case _ =>
            }
        })

        GLFW.glfwSetCursorPosCallback(window_, (window, xpos, ypos) => {
            println(s"マウス位置が変更されました: ($xpos, $ypos)")
        })
    }

    def render(primitive: Primitive) {
        // 背景色を設定
        GL11.glClearColor(0.1f, 0.2f, 0.3f, 0.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

        primitive.render()

        // バッファのスワップ
        GLFW.glfwSwapBuffers(window_)
    }

    def gameLoop(primitive: Primitive) {

        keyboardAndMouse()
        render(primitive)

        // イベントの処理
        GLFW.glfwPollEvents()
    }
}