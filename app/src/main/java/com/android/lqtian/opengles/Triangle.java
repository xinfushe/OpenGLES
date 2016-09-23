package com.android.lqtian.opengles;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Administrator on 2016/9/13.
 */

public class Triangle {

    //顶点着色器（Vertex Shader）：用来渲染形状顶点的OpenGL ES代码。
    private final String vertexShaderCode =
        // This matrix member variable (uMVPMatrix) provides a hook to manipulate the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order for the matrix multiplication product to be correct.uMVPMatrix左乘才是对的
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}";
    // Use to access and set the view transformation 顶点着色器中的mMVPMatrix
    private int mMVPMatrixHandle;
    private int mPositionHandle;

    //片段着色器（Fragment Shader）：使用颜色或纹理渲染形状表面的OpenGL ES代码。
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";
    private int mColorHandle;

    // number of coordinates per vertex in this array 每个顶点的坐标个数
    static final int COORDS_PER_VERTEX = 3;
    //顶点数据
    private FloatBuffer vertexBuffer;
    static float triangleCoords[] = {   // in counterclockwise order:      x y z三维坐标
//            0.0f,  0.622008459f, 0.0f, // top
//            -0.5f, -0.311004243f, 0.0f, // bottom left
//            0.5f, -0.311004243f, 0.0f  // bottom right
            0.0f,  0.5f, 0.0f, // top
            -0.5f, -0.0f, 0.0f, // bottom left
            0.5f, -0.0f, 0.0f  // bottom right
    };

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.63671875f, 0.16953125f, 0.12265625f, 1.0f };

    //程式（Program）：一个OpenGL ES对象，包含了你希望用来绘制一个或更多图形所要用到的着色器。
    private final int mProgram;
    public Triangle() {
        // initialize vertex byte buffer for shape coordinates  可以用流水线写法
        // 初始化顶点缓冲区bb 大小为顶点长度*4 byte
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);// (number of coordinate values * 4 bytes per float)
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();
        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);
        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);
        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);
    }


    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    //唯一接口
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);
        // get handle to vertex shader's vPosition member   vPosition成员变量
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);//vertexStride步幅 vertexBuffer为顶点

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        // Set color for drawing the triangle  设置color
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the triangle 这里才是画
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }


}
