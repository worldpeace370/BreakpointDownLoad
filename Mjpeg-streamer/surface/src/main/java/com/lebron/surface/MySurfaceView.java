package com.lebron.surface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by wuxiangkun on 2016/11/6 20:00.
 * Contacts wuxiangkun@live.com
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    private CircleThread mCircleThread;

    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        mCircleThread = new CircleThread(surfaceHolder, 10);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCircleThread.isRunning = true;
        mCircleThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCircleThread.isRunning = false;
        try {
            mCircleThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class CircleThread extends Thread{
        float radius;
        Paint paint;
        boolean isRunning;
        SurfaceHolder mSurfaceHolder;
        public CircleThread(SurfaceHolder holder, float radius){
            this.mSurfaceHolder = holder;
            this.radius = radius;
            isRunning = false;
            paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
        }
        @Override
        public void run() {
            Canvas canvas = null;
            while (isRunning){
                try {
                    synchronized (mSurfaceHolder){
                        canvas = mSurfaceHolder.lockCanvas();
                        canvas.drawColor(Color.BLACK);
                        canvas.translate(300, 400);
                        canvas.drawCircle(0, 0, radius++, paint);
                        sleep(50);
                        if(radius > 100){
                            radius = 10;
                        }
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
//                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
