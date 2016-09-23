package com.android.lqtian.opengles;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2016/9/13.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private Triangle mTriangle;
    private Square mSquare;
    private GLBitmap mGlBitmap;

    public volatile float mAngle;//由于渲染器代码运行在一个独立的线程中（非主UI线程），我们必须同时将该变量声明为volatile。

    public float getAngle() {
        return mAngle;
    }
    public void setAngle(float angle) {
        mAngle = angle;
    }

    public MyGLRenderer(Context context) {
        this.context = context;
    }

    //调用一次，用来配置View的OpenGL ES环境。初始化使用
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // initialize a triangle
        mTriangle = new Triangle();
        // initialize a square
        mSquare = new Square();

        mGlBitmap =new GLBitmap();
        mGlBitmap.loadGLTexture(unused, this.context);
    }
    private float[] mRotationMatrix = new float[16];
//每次重新绘制View时被调用。
    public void onDrawFrame(GL10 gl) {
        float[] scratch = new float[16];//最终的矩阵
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 相机视角（Camera View）：这个变换会基于一个虚拟相机位置改变绘图对象的坐标。
        // 注意到OpenGL ES并没有定义一个实际的相机对象，取而代之的，它提供了一些辅助方法，
        // 通过对绘图对象的变换来模拟相机视角。一个相机视角变换可能仅在建立你的GLSurfaceView时计算一次，
        // 也可能根据用户的行为或者你的应用的功能进行动态调整。

        // Set the camera position (View matrix)        相机视角矩阵mViewMatrix
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1f, 0.0f);//eyeX eyeY eyeZ 0f, 0f, -3,    centerX centerY centerZ 0 0 0     upX upY upZ 0 1 0
//        public static void setLookAtM (float[] rm, int rmOffset, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ)
//      相机的坐标  目标的位置 相机的视觉向量(指定观测点方向为“上”的向量。) 都是基于世界坐标系
        Log.i("MyGLRenderer", Arrays.toString(mViewMatrix));//4 * 4 的矩阵

        // Calculate the projection and view transformation    Model View Projection Matrix = mProjectionMatrix * mViewMatrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // Create a rotation transformation for the triangle
//        long time = SystemClock.uptimeMillis() % 4000L;
//        float angle = 0.090f * ((int) time);
        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        //画图

        mGlBitmap.draw(scratch);

//        mSquare.draw(scratch);

//        mTriangle.draw(scratch);
    }

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];//投影矩阵
    private final float[] mViewMatrix = new float[16];
//如果View的几何形态发生变化时会被调用，例如当设备的屏幕方向发生改变时。
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        //Normalized device coordinate -> window coordinate
        GLES20.glViewport(0, 0, width, height);

        //投影（Projection）：这个变换会基于显示它们的GLSurfaceView的长和宽，来调整绘图对象的坐标。
        //如果没有该计算，那么用OpenGL ES绘制的对象会由于其长宽比例和View窗口比例的不一致而发生形变。
        // 一个投影变换一般仅当OpenGL View的比例在渲染器的onSurfaceChanged()方法中建立或发生变化时才被计算。
        // 关于更多OpenGL ES投影和坐标映射的知识，可以阅读Mapping Coordinates for Drawn Objects。
        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);//视锥体  视景体的6个裁剪平面（左、右、底、顶、近和远）
    }


    //着色器包含了OpenGL Shading Language（GLSL）代码，它必须先被编译然后才能在OpenGL环境中使用。
    //要编译这些代码，需要在你的渲染器类中创建一个辅助方法：
    public static int loadShader(int type, String shaderCode){
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
