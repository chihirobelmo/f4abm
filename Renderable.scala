package com.example.render

import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryStack
import org.joml.Matrix4f
import org.joml.Vector3f

class Camera() {
    
    private val viewMatrix = new Matrix4f()
    private val projectionMatrix = new Matrix4f()

    var position: Vector3f = new Vector3f(0.0f, 0.0f, 10.0f)
    var target: Vector3f = new Vector3f(0.0f, 0.0f, 0.0f)
    var up: Vector3f = new Vector3f(0.0f, 1.0f, 0.0f)
    var fov: Float = 45.0f
    var aspectRatio: Float = 800.0f / 600.0f
    var near: Float = 0.1f
    var far: Float = 100.0f

    update()

    def update() {
        viewMatrix.identity()
        viewMatrix.lookAt(position, target, up)

        projectionMatrix.identity()
        projectionMatrix.perspective(fov, aspectRatio, near, far)
    }

    def translate(x: Float, y: Float): Camera = {
        position.add(x, y, 0)
        target.add(x, y, 0)
        update()
        this
    }

    def zoom(z: Float): Camera = {
        position.add(0, 0, z)
        update()
        this
    }

    def getViewMatrix(): Matrix4f = {
        viewMatrix
    }

    def getProjectionMatrix(): Matrix4f = {
        projectionMatrix
    }
}

object Util {
    def createSRT2D(scale: Vector3f, rotation: Int, translation: Vector3f): Matrix4f = {
        val matrix = new Matrix4f()
        matrix.identity()
        matrix.scale(scale)
        matrix.rotateZ(Math.toRadians(45).toFloat)
        matrix.translate(translation)
        matrix
    }
}

trait Renderable {
    def preRender(): Renderable
    def render(): Renderable
    def end(): Renderable
    def srt(scale: Float, rot: Float, tran: Vector3f): Renderable
    def camera(camera: Camera): Renderable
}

object PrimitiveShader {
    
    val vertexShaderSource =
    """
    #version 330 core
    layout(location = 0) in vec3 aPos;
    layout(location = 1) in vec3 aColor;
    out vec3 vertexColor;
    uniform mat4 srtMatrix;
    uniform mat4 viewMatrix;
    uniform mat4 projMatrix;
    void main() {
        gl_Position = projMatrix * viewMatrix * srtMatrix * vec4(aPos, 1.0);
        vertexColor = aColor;
    }
    """

    val fragmentShaderSource =
    """
    #version 330 core
    in vec3 vertexColor;
    out vec4 FragColor;
    void main() {
        FragColor = vec4(vertexColor.r, vertexColor.g, vertexColor.b, 1.0);
    }
    """

    val vertexShader = compileShader(vertexShaderSource, GL20.GL_VERTEX_SHADER)
    val fragmentShader = compileShader(fragmentShaderSource, GL20.GL_FRAGMENT_SHADER)

    val shaderProgram = GL20.glCreateProgram()

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

    def use(vaoId: Int, vboId: Int, srtMatrix: Matrix4f, camera: Camera, vertices: Array[Float]) {

        GL30.glBindVertexArray(vaoId)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0)
        GL20.glEnableVertexAttribArray(0)

        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4)
        GL20.glEnableVertexAttribArray(1)

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)

        GL20.glAttachShader(shaderProgram, vertexShader)
        GL20.glAttachShader(shaderProgram, fragmentShader)
        GL20.glLinkProgram(shaderProgram)

        GL20.glUseProgram(shaderProgram)


        val srtMatrixLocation = GL20.glGetUniformLocation(shaderProgram, "srtMatrix")
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        srtMatrix.get(matrixBuffer)
        GL20.glUniformMatrix4fv(srtMatrixLocation, false, matrixBuffer)
        MemoryUtil.memFree(matrixBuffer)

        val viewMatrixLocation = GL20.glGetUniformLocation(shaderProgram, "viewMatrix")
        val viewMatrixBuffer = MemoryUtil.memAllocFloat(16)
        camera.getViewMatrix().get(viewMatrixBuffer)
        GL20.glUniformMatrix4fv(viewMatrixLocation, false, viewMatrixBuffer)
        MemoryUtil.memFree(viewMatrixBuffer)

        val projMatrixLocation = GL20.glGetUniformLocation(shaderProgram, "projMatrix")
        val projMatrixBuffer = MemoryUtil.memAllocFloat(16)
        camera.getProjectionMatrix().get(projMatrixBuffer)
        GL20.glUniformMatrix4fv(projMatrixLocation, false, projMatrixBuffer)
        MemoryUtil.memFree(projMatrixBuffer)
    }
}

abstract class Primitive() extends Renderable {

    var vaoId_ = GL30.glGenVertexArrays()
    var vboId_ = GL15.glGenBuffers()

    val srtMatrix = new Matrix4f()

    var camera = new Camera()

    val vertices: Array[Float] = Array(
        -0.5f, -0.5f, +0.0f, 1.0f, 0.0f, 1.0f, // 左下 白
        +0.5f, -0.5f, +0.0f, 1.0f, 0.0f, 1.0f, // 右下 白
        +0.0f, +0.5f, +0.0f, 1.0f, 0.0f, 1.0f  // 上 白
    )

    override def preRender(): Renderable = {

        PrimitiveShader.use(this.vaoId_, this.vboId_, this.srtMatrix, this.camera, this.vertices)

        this
    }

    override def end(): Renderable = {
        GL15.glDeleteBuffers(vboId_)
        GL30.glDeleteVertexArrays(vaoId_)

        this
    }

    override def srt(scale: Float, rot: Float, tran: Vector3f): Renderable = {
        val scaleMatrix = new Matrix4f().scaling(scale, scale, scale)
        val rotationMatrix = new Matrix4f().rotateZ(Math.toRadians(rot).toFloat)
        val translationMatrix = new Matrix4f().translation(tran)
        srtMatrix.identity().mul(translationMatrix).mul(rotationMatrix).mul(scaleMatrix)

        this
    }

    override def camera(camera: Camera): Renderable = {
        this.camera = camera
        this
    }
}

class Triangle() extends Primitive() {

    // 頂点データ
    override val vertices: Array[Float] = Array(
        -0.5f, -0.5f, +0.0f, 1.0f, 1.0f, 1.0f, // 左下 白
        +0.5f, -0.5f, +0.0f, 1.0f, 1.0f, 1.0f, // 右下 白
        +0.0f, +0.5f, +0.0f, 1.0f, 1.0f, 1.0f  // 上 白
    )

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices.length / 6)
        GL30.glBindVertexArray(0)

        this
    }
}

class AirTrack() extends Primitive() {

    // 頂点データ
    override val vertices: Array[Float] = Array(
        -0.5f, -0.5f, +0.0f, // Line start point
        +0.5f, -0.5f, +0.0f, // Line end point
        +0.5f, +0.5f, +0.0f, // Another line start point
        -0.5f, +0.5f, +0.0f  // Another line end point
    )

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_LINES, 0, vertices.length / 3)
        GL30.glBindVertexArray(0)

        this
    }
}

/*

  0 1 2 3 4 5 6 7
0 0 0 1 1 1 1 0 0  // 0x3C
1 0 1 0 0 0 0 1 0  // 0x42
2 1 0 0 0 0 0 0 1  // 0x81
3 1 0 0 0 0 0 0 1  // 0x81
4 1 1 1 1 1 1 1 1  // 0xFF
5 1 0 0 0 0 0 0 1  // 0x81
6 1 0 0 0 0 0 0 1  // 0x81
7 1 0 0 0 0 0 0 1  // 0x81

val A: Array[Int] = Array(
  0x3C, // 00111100
  0x42, // 01000010
  0x81, // 10000001
  0x81, // 10000001
  0xFF, // 11111111
  0x81, // 10000001
  0x81, // 10000001
  0x81  // 10000001
)

*/
object FontData {
    val widthPx = 8
    val heightPx = 8

    // 簡単なビットマップフォントデータ（例として8x8の固定幅フォント）
    val fontData: Array[Array[Int]] = Array(
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // ' ' (space)
        Array(0x18, 0x3C, 0x3C, 0x18, 0x18, 0x00, 0x18, 0x00), // '!'
        Array(0x6C, 0x6C, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00), // '"'
        Array(0x6C, 0x6C, 0xFE, 0x6C, 0xFE, 0x6C, 0x6C, 0x00), // '#'
        Array(0x18, 0x3E, 0x58, 0x3C, 0x1A, 0x7C, 0x18, 0x00), // '$'
        Array(0x00, 0x66, 0x6C, 0x18, 0x30, 0x66, 0x46, 0x00), // '%'
        Array(0x1C, 0x36, 0x1C, 0x6E, 0x3B, 0x33, 0x6E, 0x00), // '&'
        Array(0x18, 0x18, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00), // '''
        Array(0x0C, 0x18, 0x30, 0x30, 0x30, 0x18, 0x0C, 0x00), // '('
        Array(0x30, 0x18, 0x0C, 0x0C, 0x0C, 0x18, 0x30, 0x00), // ')'
        Array(0x00, 0x66, 0x3C, 0xFF, 0x3C, 0x66, 0x00, 0x00), // '*'
        Array(0x00, 0x18, 0x18, 0x7E, 0x18, 0x18, 0x00, 0x00), // '+'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x30), // ','
        Array(0x00, 0x00, 0x00, 0x7E, 0x00, 0x00, 0x00, 0x00), // '-'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x00), // '.'
        Array(0x06, 0x0C, 0x18, 0x30, 0x60, 0xC0, 0x80, 0x00), // '/'
        Array(0x3C, 0x66, 0x6E, 0x76, 0x66, 0x66, 0x3C, 0x00), // '0'
        Array(0x18, 0x38, 0x18, 0x18, 0x18, 0x18, 0x7E, 0x00), // '1'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x30, 0x60, 0x7E, 0x00), // '2'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x06, 0x66, 0x3C, 0x00), // '3'
        Array(0x0C, 0x1C, 0x3C, 0x6C, 0xFE, 0x0C, 0x0C, 0x00), // '4'
        Array(0x7E, 0x60, 0x7C, 0x06, 0x06, 0x66, 0x3C, 0x00), // '5'
        Array(0x3C, 0x66, 0x60, 0x7C, 0x66, 0x66, 0x3C, 0x00), // '6'
        Array(0x7E, 0x66, 0x0C, 0x18, 0x30, 0x30, 0x30, 0x00), // '7'
        Array(0x3C, 0x66, 0x66, 0x3C, 0x66, 0x66, 0x3C, 0x00), // '8'
        Array(0x3C, 0x66, 0x66, 0x3E, 0x06, 0x66, 0x3C, 0x00), // '9'
        Array(0x00, 0x18, 0x18, 0x00, 0x00, 0x18, 0x18, 0x00), // ':'
        Array(0x00, 0x18, 0x18, 0x00, 0x00, 0x18, 0x18, 0x30), // ';'
        Array(0x0E, 0x1C, 0x38, 0x70, 0x38, 0x1C, 0x0E, 0x00), // '<'
        Array(0x00, 0x00, 0x7E, 0x00, 0x00, 0x7E, 0x00, 0x00), // '='
        Array(0x70, 0x38, 0x1C, 0x0E, 0x1C, 0x38, 0x70, 0x00), // '>'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x18, 0x00, 0x18, 0x00), // '?'
        Array(0x3C, 0x66, 0x6E, 0x6E, 0x60, 0x60, 0x3E, 0x00), // '@'
        Array(0x18, 0x3C, 0x66, 0x66, 0x7E, 0x66, 0x66, 0x00), // 'A'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x66, 0x66, 0x7C, 0x00), // 'B'
        Array(0x3C, 0x66, 0x60, 0x60, 0x60, 0x66, 0x3C, 0x00), // 'C'
        Array(0x7C, 0x66, 0x66, 0x66, 0x66, 0x66, 0x7C, 0x00), // 'D'
        Array(0x7E, 0x60, 0x60, 0x7C, 0x60, 0x60, 0x7E, 0x00), // 'E'
        Array(0x7E, 0x60, 0x60, 0x7C, 0x60, 0x60, 0x60, 0x00), // 'F'
        Array(0x3C, 0x66, 0x60, 0x6E, 0x66, 0x66, 0x3C, 0x00), // 'G'
        Array(0x66, 0x66, 0x66, 0x7E, 0x66, 0x66, 0x66, 0x00), // 'H'
        Array(0x3C, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'I'
        Array(0x1E, 0x0C, 0x0C, 0x0C, 0x0C, 0x6C, 0x38, 0x00), // 'J'
        Array(0x66, 0x6C, 0x78, 0x70, 0x78, 0x6C, 0x66, 0x00), // 'K'
        Array(0x60, 0x60, 0x60, 0x60, 0x60, 0x60, 0x7E, 0x00), // 'L'
        Array(0x63, 0x77, 0x7F, 0x7F, 0x6B, 0x63, 0x63, 0x00), // 'M'
        Array(0x66, 0x76, 0x7E, 0x7E, 0x6E, 0x66, 0x66, 0x00), // 'N'
        Array(0x3C, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x00), // 'O'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x60, 0x60, 0x60, 0x00), // 'P'
        Array(0x3C, 0x66, 0x66, 0x66, 0x66, 0x6E, 0x3C, 0x06), // 'Q'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x78, 0x6C, 0x66, 0x00), // 'R'
        Array(0x3C, 0x66, 0x30, 0x1C, 0x06, 0x66, 0x3C, 0x00), // 'S'
        Array(0x7E, 0x7E, 0x5A, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'T'
        Array(0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x00), // 'U'
        Array(0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x18, 0x00), // 'V'
        Array(0x63, 0x63, 0x63, 0x6B, 0x7F, 0x77, 0x63, 0x00), // 'W'
        Array(0x66, 0x66, 0x3C, 0x18, 0x3C, 0x66, 0x66, 0x00), // 'X'
        Array(0x66, 0x66, 0x66, 0x3C, 0x18, 0x18, 0x3C, 0x00), // 'Y'
        Array(0x7F, 0x63, 0x31, 0x18, 0x0C, 0x46, 0x7F, 0x00), // 'Z'
        Array(0x3C, 0x66, 0x06, 0x1E, 0x06, 0x66, 0x3C, 0x00), // '['
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // '\'
        Array(0x3C, 0x66, 0x60, 0x60, 0x60, 0x66, 0x3C, 0x00), // ']'
        Array(0x18, 0x3C, 0x66, 0x66, 0x66, 0x66, 0x66, 0x00), // '^'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // '_'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // '`'
        Array(0x00, 0x00, 0x3C, 0x06, 0x3E, 0x66, 0x3B, 0x00), // 'a'
        Array(0x60, 0x60, 0x7C, 0x66, 0x66, 0x66, 0x7C, 0x00), // 'b'
        Array(0x00, 0x00, 0x3C, 0x66, 0x60, 0x66, 0x3C, 0x00), // 'c'
        Array(0x06, 0x06, 0x3E, 0x66, 0x66, 0x66, 0x3E, 0x00), // 'd'
        Array(0x00, 0x00, 0x3C, 0x66, 0x7E, 0x60, 0x3C, 0x00), // 'e'
        Array(0x0E, 0x18, 0x3E, 0x18, 0x18, 0x18, 0x18, 0x00), // 'f'
        Array(0x00, 0x00, 0x3E, 0x66, 0x66, 0x3E, 0x06, 0x7C), // 'g'
        Array(0x60, 0x60, 0x7C, 0x66, 0x66, 0x66, 0x66, 0x00), // 'h'
        Array(0x18, 0x00, 0x38, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'i'
        Array(0x06, 0x00, 0x06, 0x06, 0x06, 0x06, 0x06, 0x3C), // 'j'
        Array(0x60, 0x60, 0x66, 0x6C, 0x78, 0x6C, 0x66, 0x00), // 'k'
        Array(0x38, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'l'
        Array(0x00, 0x00, 0x66, 0x7F, 0x7F, 0x6B, 0x63, 0x00), // 'm'
        Array(0x00, 0x00, 0x7C, 0x66, 0x66, 0x66, 0x66, 0x00), // 'n'
        Array(0x00, 0x00, 0x3C, 0x66, 0x66, 0x66, 0x3C, 0x00), // 'o'
        Array(0x00, 0x00, 0x7C, 0x66, 0x66, 0x7C, 0x60, 0x60), // 'p'
        Array(0x00, 0x00, 0x3E, 0x66, 0x66, 0x3E, 0x06, 0x06), // 'q'
        Array(0x00, 0x00, 0x6E, 0x70, 0x60, 0x60, 0x60, 0x00), // 'r'
        Array(0x00, 0x00, 0x3E, 0x60, 0x3C, 0x06, 0x7C, 0x00), // 's'
        Array(0x18, 0x18, 0x3E, 0x18, 0x18, 0x18, 0x0E, 0x00), // 't'
        Array(0x00, 0x00, 0x66, 0x66, 0x66, 0x66, 0x3E, 0x00), // 'u'
        Array(0x00, 0x00, 0x66, 0x66, 0x66, 0x3C, 0x18, 0x00), // 'v'
        Array(0x00, 0x00, 0x63, 0x6B, 0x7F, 0x7F, 0x36, 0x00), // 'w'
        Array(0x00, 0x00, 0x66, 0x3C, 0x18, 0x3C, 0x66, 0x00), // 'x'
        Array(0x00, 0x00, 0x66, 0x66, 0x66, 0x3E, 0x06, 0x7C), // 'y'
        Array(0x00, 0x00, 0x7E, 0x0C, 0x18, 0x30, 0x7E, 0x00)  // 'z'
    )
}

class FontRenderer(x: Float, y: Float, str: String, windowWidth: Int, windowHeight: Int) extends Primitive() {

    val startX_ = x
    val startY_ = y
    val str_ = str
    val windowWidth_ = windowWidth
    val windowHeight_ = windowHeight

    private def drawChar(x: Float, y: Float, c: Char, windowWidth: Int, windowHeight: Int): Unit = {
        val index = c - ' '
        if (index < 0 || index >= FontData.fontData.length) return

        val data = FontData.fontData(index)
        val vertices = new Array[Float](FontData.widthPx * FontData.heightPx * 6 * 3 * 2) // 6 vertices per quad, 6 attributes (position and color)
        var vertexIndex = 0
        
        for (i <- 0 until FontData.heightPx) {
            for (j <- 0 until FontData.widthPx) {
                if ((data(FontData.heightPx - 1 - i) & (1 << (FontData.widthPx - 1 - j))) != 0) {
                    val xPos = (x + j) / windowWidth_ * 2 - 1
                    val yPos = (y + i) / windowHeight_ * 2 - 1
                    val color = Array(1.0f, 1.0f, 1.0f)

                    // First triangle
                    vertices(vertexIndex) = xPos
                    vertices(vertexIndex + 1) = yPos
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6

                    vertices(vertexIndex) = xPos + 2.0f / windowWidth
                    vertices(vertexIndex + 1) = yPos
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6

                    vertices(vertexIndex) = xPos
                    vertices(vertexIndex + 1) = yPos + 2.0f / windowHeight
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6

                    // Second triangle
                    vertices(vertexIndex) = xPos + 2.0f / windowWidth
                    vertices(vertexIndex + 1) = yPos
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6

                    vertices(vertexIndex) = xPos + 2.0f / windowWidth
                    vertices(vertexIndex + 1) = yPos + 2.0f / windowHeight
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6

                    vertices(vertexIndex) = xPos
                    vertices(vertexIndex + 1) = yPos + 2.0f / windowHeight
                    vertices(vertexIndex + 2) = 0.0f
                    vertices(vertexIndex + 3) = color(0)
                    vertices(vertexIndex + 4) = color(1)
                    vertices(vertexIndex + 5) = color(2)
                    vertexIndex += 6
                }
            }
        }
        val vaoId = GL30.glGenVertexArrays()
        val vboId = GL15.glGenBuffers()

        GL30.glBindVertexArray(vaoId)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 6 * 4, 0)
        GL20.glEnableVertexAttribArray(0)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4)
        GL20.glEnableVertexAttribArray(1)

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexIndex / 6)

        GL20.glDisableVertexAttribArray(0)
        GL20.glDisableVertexAttribArray(1)

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
    }

    override def render(): Renderable = {
        var x_ = startX_
        var y_ = startY_
        for (i <- str_.indices) {
            if (str_(i) == '\n') {
                x_ = startX_ - FontData.widthPx // why ?
                y_ -= FontData.heightPx
            }
            drawChar(
                windowWidth_ / 2 + x_, 
                windowHeight_ / 2 + y_, 
                str_(i), 
                windowWidth_, 
                windowHeight_)
            x_ += FontData.widthPx
        }

        this
    }
}