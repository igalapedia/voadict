package com.ljb.voadict;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

public class DictSeekBar extends View {

    public final String[] mLables = {"a","b","c","d","e","f","g","h","i","j","k","l",
            "m","n","o","p","q","r","s","t","u","v","w","y","z"}; 
    private int mLableColor = Color.BLACK;
    private int mThumbColor = Color.RED;
    private int mMax = mLables.length-1; //a-z+#
    private int mProgress = 0;
    Paint mLablePaint = null;
    Paint mThumbPaint = null;
    Rect mLabelBounds = null; 
    RectF mThumbRect = new RectF();
    private static final boolean DEBUG = false;
    private static void logd(String log) {
        if (DEBUG) {
            Log.d("DictSeekBar", log);
        }
    }
    
   public interface OnSeekBarChangeListener {
        
        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         * 
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range 0..max where max
         *        was set by {@link ProgressBar#setMax(int)}. (The default value for max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(DictSeekBar seekBar, int progress, boolean fromUser);
    
        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar. 
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(DictSeekBar seekBar);
        
        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar. 
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(DictSeekBar seekBar);
    }
    private OnSeekBarChangeListener mOnSeekBarChangeListener;  
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mOnSeekBarChangeListener = listener;
    }
    
    void onStartTrackingTouch() {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }
    
    void onStopTrackingTouch() {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }
    private void init(){
        logd("init");
        mLableColor = getResources().getColor(R.color.seeklable);
        mThumbColor = getResources().getColor(R.color.seekthumb);
        mLablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLablePaint.setTextAlign(Paint.Align.CENTER);
        mLablePaint.setColor(mLableColor);
        mLablePaint.setTextSize(20);
        mThumbPaint = new Paint();
        mThumbPaint.setColor(mThumbColor);
        mThumbPaint.setStyle(Style.STROKE);
        mThumbPaint.setStrokeWidth(2);
        mLabelBounds = new Rect(); 
        mLablePaint.getTextBounds(new char[]{'a'}, 0, 1, mLabelBounds);
        mLabelBounds.offset(0, -mLabelBounds.top);
        logd("LabelBounds = "+mLabelBounds.toString());
    }
    
    public DictSeekBar(Context context) {
        super(context);
        logd("DictSeekBar(Context context)");
        init();
    }

    public DictSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        logd("DictSeekBar(Context context, AttributeSet attrs, int defStyle)");
        init();
    }

    public DictSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        logd("DictSeekBar(Context context, AttributeSet attrs)");
        init();
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(mPaddingLeft, mPaddingTop);
        
        float height = getHeight();
        float avilable = height - mPaddingTop - mPaddingBottom;
        float oneHeight =  avilable/(mMax+1);
        float y = mPaddingTop;
        float toppad = (oneHeight - mLabelBounds.height())/2;
        //因为是居中画的，所以要x是字中点
        int x = getWidth()/2;
        for (int i = 0; i < mMax+1; i++) {
            //mThumbRect.set(0,i*oneHeight,getWidth(),(i+1)*oneHeight);
            //canvas.drawRect(mThumbRect, mThumbPaint);
            //y是左下标
            y = i*oneHeight + toppad+mLabelBounds.bottom;
            canvas.drawText(mLables[i], x, y, mLablePaint);
            
            if (i == mProgress) {
                mThumbRect.set(0,i*oneHeight,getWidth(),(i+1)*oneHeight);
                canvas.drawRoundRect(mThumbRect, 10, 10, mThumbPaint);
            }
        }
        
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                break;
                
            case MotionEvent.ACTION_MOVE:
                trackTouchEvent(event);
                attemptClaimDrag();
                break;
                
            case MotionEvent.ACTION_UP:
                trackTouchEvent(event);
                onStopTrackingTouch();
                setPressed(false);
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;
                
            case MotionEvent.ACTION_CANCEL:
                onStopTrackingTouch();
                setPressed(false);
                invalidate(); // see above explanation
                break;
        }
        return true;
    }
    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(true);
        }
    }
   
    public void setProgress(int progress){
        onProgressRefresh(progress,false);
    }
    
    void onProgressRefresh(int progress, boolean fromUser) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > mMax) {
            progress = mMax;
        }

        if (progress != mProgress) {
            mProgress = progress;
            logd(String.format("process set to %d",mProgress));
            invalidate();
            if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener.onProgressChanged(this, mProgress, fromUser);
            }
        }
    }
    
    private void trackTouchEvent(MotionEvent event) {
        final int height = getHeight();
        final int available = height - mPaddingTop - mPaddingBottom;
        float y = event.getY();
        float progress = 0;
        if (y < mPaddingTop) {
            progress = 0;
        } else if (y > height - mPaddingBottom) {
            progress = mMax;
        } else {
            progress = (float)(y - mPaddingTop)*mMax / (float)available;
        }
        
        onProgressRefresh(Math.round(progress),true);
    }
}