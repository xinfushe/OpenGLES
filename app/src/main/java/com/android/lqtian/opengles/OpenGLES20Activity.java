package com.android.lqtian.opengles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class OpenGLES20Activity extends AppCompatActivity {
    private MyGLSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);
    }

    class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;
        public MyGLSurfaceView(Context context){
            super(context);
            // Create an OpenGL ES 2.0 context
            setEGLContextClientVersion(2);
            mRenderer = new MyGLRenderer(getContext());

            // Set the Renderer for drawing on the GLSurfaceView
            setRenderer(mRenderer);

            //GLSurfaceView.RENDERMODE_WHEN_DIRTY;其含义是：仅在你的绘制数据发生变化时才在视图中进行绘制操作：
            // Render the view only when there is a change in the drawing data
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
        private float mPreviousX;
        private float mPreviousY;

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            // MotionEvent reports input details from the touch screen
            // and other input controls. In this case, you are only
            // interested in events where the touch position changed.
            float x = e.getX();
            float y = e.getY();

            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE: //滑动
                    float dx = x - mPreviousX;//x方向偏移量
                    float dy = y - mPreviousY;//y方向偏移量

                    // reverse direction of rotation above the mid-line
                    if (y > getHeight() / 2) {
                        dx = dx * -1 ;
                    }
                    // reverse direction of rotation to left of the mid-line
                    if (x < getWidth() / 2) {
                        dy = dy * -1 ;
                    }

                    mRenderer.setAngle(mRenderer.getAngle() +((dx + dy) * TOUCH_SCALE_FACTOR));//设置原角度加上偏移量
                    requestRender();
            }

            mPreviousX = x;
            mPreviousY = y;
            return true;
        }
    }
}
