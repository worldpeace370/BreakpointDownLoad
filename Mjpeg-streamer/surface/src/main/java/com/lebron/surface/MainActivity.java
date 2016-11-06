package com.lebron.surface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new MySurfaceView(this));
    }

    class  AnimateView extends View{
        float radius = 10;
        Paint paint;
        public AnimateView(Context context) {
            super(context);
            paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.translate(300, 400);
            canvas.drawCircle(0, 0, radius++, paint);
            if(radius > 100){
                radius = 10;
            }
            invalidate();
        }
    }
}
