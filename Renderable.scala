package com.example.render

import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import org.joml.Matrix4f
import org.joml.Vector3f

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
}

trait Renderable2D extends Renderable {
    def srt(scale: Float, rot: Float, tran: Vector3f): Renderable2D
}

abstract class Primitive() extends Renderable2D {

    var vaoId_ = 0
    var vboId_ = 0
    var vertexShader_ = 0
    var fragmentShader_ = 0
    var shaderProgram_ = 0

    // 頂点シェーダーコード
    private val vertexShaderSource_ =
    """
    #version 330 core
    layout(location = 0) in vec3 aPos;
    uniform mat4 srtMatrix;
    void main() {
        gl_Position = srtMatrix * vec4(aPos, 1.0);
    }
    """

    // フラグメントシェーダーコード
    private val fragmentShaderSource_ =
    """
    #version 330 core
    out vec4 FragColor;
    void main() {
        FragColor = vec4(1.0, 0.5, 0.2, 1.0);
    }
    """

    private val srtMatrix = new Matrix4f()

    def init(): Unit = {
        this.vaoId_ = GL30.glGenVertexArrays()
        this.vboId_ = GL15.glGenBuffers()

        // シェーダープログラムの作成
        this.vertexShader_ = compileShader(this.vertexShaderSource_, GL20.GL_VERTEX_SHADER)
        this.fragmentShader_ = compileShader(this.fragmentShaderSource_, GL20.GL_FRAGMENT_SHADER)

        this.shaderProgram_ = GL20.glCreateProgram()
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

    override def preRender(): Renderable = {
        GL20.glAttachShader(shaderProgram_, vertexShader_)
        GL20.glAttachShader(shaderProgram_, fragmentShader_)
        GL20.glLinkProgram(shaderProgram_)

        GL20.glUseProgram(shaderProgram_)

        // Pass the matrix to the shader
        val srtMatrixLocation = GL20.glGetUniformLocation(shaderProgram_, "srtMatrix")
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        srtMatrix.get(matrixBuffer)
        GL20.glUniformMatrix4fv(srtMatrixLocation, false, matrixBuffer)
        MemoryUtil.memFree(matrixBuffer)

        this
    }

    override def end(): Renderable = {
        GL15.glDeleteBuffers(vboId_)
        GL30.glDeleteVertexArrays(vaoId_)

        this
    }

    override def srt(scale: Float, rot: Float, tran: Vector3f): Renderable2D = {
        val scaleMatrix = new Matrix4f().scaling(scale, scale, scale)
        val rotationMatrix = new Matrix4f().rotateZ(Math.toRadians(rot).toFloat)
        val translationMatrix = new Matrix4f().translation(tran)
        srtMatrix.identity().mul(translationMatrix).mul(rotationMatrix).mul(scaleMatrix)

        this
    }
}

class Triangle() extends Primitive() {
    init()

    // 頂点データ
    val vertices: Array[Float] = Array(
        -0.5f, -0.5f, 0.0f, // 左下
        0.5f, -0.5f, 0.0f,  // 右下
        0.0f,  0.5f, 0.0f   // 上
    )

    GL30.glBindVertexArray(this.vaoId_)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId_)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

    // 頂点属性の設定
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0)
    GL20.glEnableVertexAttribArray(0)

    // バインド解除
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL30.glBindVertexArray(0)

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3)
        GL30.glBindVertexArray(0)

        this
    }
}

class AirTrack() extends Primitive() {
    init()

    // 頂点データ
    val vertices: Array[Float] = Array(
        -0.5f, -0.5f, +0.0f, // Line start point
        +0.5f, -0.5f, +0.0f, // Line end point
        +0.5f, +0.5f, +0.0f, // Another line start point
        -0.5f, +0.5f, +0.0f  // Another line end point
    )

    GL30.glBindVertexArray(this.vaoId_)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId_)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

    // 頂点属性の設定
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0)
    GL20.glEnableVertexAttribArray(0)

    // バインド解除
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL30.glBindVertexArray(0)

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_LINES, 0, vertices.length / 3)
        GL30.glBindVertexArray(0)

        this
    }
}