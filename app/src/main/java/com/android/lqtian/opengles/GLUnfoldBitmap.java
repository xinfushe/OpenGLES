package com.android.lqtian.opengles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * 参考：
 * http://blog.csdn.net/nupt123456789/article/details/40375731
 * http://blog.piasy.com/2016/06/07/Open-gl-es-android-2-part-1/
 * Created by Administrator on 2016/9/23.
 */

public class GLUnfoldBitmap {
    //程式（Program）：一个OpenGL ES对象，包含了你希望用来绘制一个或更多图形所要用到的着色器。
    private final int mProgram;

    //顶点着色器（Vertex Shader）：用来渲染形状顶点的OpenGL ES代码。
    private final String vertexShaderCode =
            // This matrix member variable (uMVPMatrix) provides a hook to manipulate the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 a_texCoord;" +//外部传入
                    "varying vec2 v_texCoord;" +//传到片段着色器中
                    "varying vec4 Position;" +//传到片段着色器中
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order for the matrix multiplication product to be correct.uMVPMatrix左乘才是对的
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  v_texCoord = a_texCoord;" +
                    "	Position = vPosition;" +
                    "}";
    // Use to access and set the view transformation 顶点着色器中的mMVPMatrix
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mTexCoordHandle;

    //片段着色器（Fragment Shader）：使用颜色或纹理渲染形状表面的OpenGL ES代码。
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "varying vec4 Position;" +//从上方获取坐标
                    "void main() {" +
                    "float PI= 3.14159265359;" +
                    "float F0 = 210.0 * PI /180.0;" +
                    "float f = 0.5/2.0/sin(F0/4.0);" +
                    "float R = 2.0* f * sin(PI/4.0);" +
                    "float xp = Position.x;" +
                    "float yp = Position.y;" +
                    "float a=sqrt(xp*xp+yp*yp);" +
                    "float fai=atan(yp,xp);" +
                    "float cita=2.0*asin(a/2.0/f);" +
                    "float x1=R*sin(cita)*cos(fai);" +
                    "float y1=R*sin(cita)*sin(fai);" +
                    "float z1=R*cos(cita);" +
                    "float rotGamma=0.0;" +
                    "rotGamma=-PI*rotGamma/180.0;" +
                    "float x1pp=z1*sin(rotGamma)+x1*cos(rotGamma);" +
                    "float y1pp=y1;" +
                    "float z1pp=z1*cos(rotGamma)-x1*sin(rotGamma);" +
                    "float rotSita=0.0;" +
                    "rotSita=-PI*rotSita/180.0;" +
                    "float x1p=x1pp;" +
                    "float y1p=y1pp*cos(rotSita)-z1pp*sin(rotSita);" +
                    "float z1p=y1pp*sin(rotSita)+z1pp*cos(rotSita);" +
                    "float citaP=acos(z1p/R);" +
                    "float faiP=atan(y1p/x1p);" +
                    "float rotFai=0.0;" +
                    "faiP+=PI* rotFai/180.0;" +
                    "if(faiP<0.0)" +
                    "faiP+=PI*2.0;" +
                    "float u=R*faiP/PI;" +
//                    "float u=0.5;" +
//                    "float v=0.5;" +
                    "float v=R*citaP/PI;" +
                    "vec2 test;"+
                    "test=vec2(u,v);"+
                    "  gl_FragColor =texture2D( s_texture, test );" +
//                    "  gl_FragColor =texture2D( s_texture, v_texCoord );" +
                    "}";
    private int mTexSamplerHandle;

    private FloatBuffer vertexBuffer; // buffer holding the vertexes
    private float vertexes[] = { // in counterclockwise order:
            1.0f, 1.0f, 0.0f, //  top right
            -1.0f, 1.0f, 0.0f, //  top left
            -1.0f, -1.0f, 0.0f, //  bottom left
            1.0f, -1.0f, 0.0f //  bottom right
    };
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private ShortBuffer drawListBuffer;
    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertexes

    //指定截取纹理的哪一部分绘制到图形上：
    private FloatBuffer textureBuffer; // buffer holding the texture coordinates
    private float texture[] = {
            // Mapping coordinates for the vertexes
            1.0f, 0.0f, // bottom right (V3)
            0.0f, 0.0f, // bottom left (V1)
            0.0f, 1.0f, // top left (V2)
            1.0f, 1.0f // top right (V4)

    };


    public GLUnfoldBitmap() {
        // initialize vertexBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertexes.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vertexes);
        vertexBuffer.position(0);

        //textureBuffer
        byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(// (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


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

    /** The texture pointer */
    private int[] textures = new int[1];
    public void loadGLTexture(GL10 unused, Context context) {
//        我们需要先通过 glGenTextures 创建纹理，再通过 glActiveTexture 激活指定编号的纹理，再通过 glBindTexture 将新建的纹理和编号绑定起来。我们可以对图片纹理设置一系列参数，例如裁剪策略、缩放策略，这部分更详细的介绍，建议看看《OpenGL ES 2 for Android A Quick - Start Guide (2013)》这本书，里面有很详细的讲解。最后，我们通过 texImage2D 把图片数据拷贝到纹理中。

        // loading texture
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.plane_image);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // ...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        // create GL_LINEAR filtered texture
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);
        // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public void draw(float[] mvpMatrix) {
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride,vertexBuffer);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,textureBuffer);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(mTexSamplerHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);

    }

}
