package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.db.chart.view.LineChartView;

/**
 * Created by pk on 5/23/2016.
 */
public class MyLineChartView extends LineChartView {
    float mFactor;
    ScaleGestureDetector mScaleGestureDetector;
    OnScaleListener mListener;


    public MyLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mFactor = 1;
        mScaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestures());
    }

    public MyLineChartView(Context context) {
        super(context);

        mFactor = 1;
        mScaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestures());
    }

    public void setOnScale(OnScaleListener listener) {
        mListener = listener;
    }

    protected void triggerOnScale() {
        if(mListener != null) {
            mListener.OnScale(mFactor);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        super.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    class MyScaleGestures implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mFactor = detector.getScaleFactor();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            triggerOnScale();
        }
    }

    public interface OnScaleListener {
        void OnScale(float factor);
    }
}


