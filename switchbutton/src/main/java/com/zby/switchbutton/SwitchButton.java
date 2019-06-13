package com.zby.switchbutton;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;


/**
 * @author ZhuBingYang
 */
public class SwitchButton extends CompoundButton {
    private static final String TAG = "SwitchButton";

    private static final int DEFAULT_ANIMATION_DURATION = 250;

    private static int[] STATE_ON = new int[]{android.R.attr.state_checked, android.R.attr.state_enabled, android.R.attr.state_pressed};
    private static int[] STATE_OFF = new int[]{-android.R.attr.state_checked, android.R.attr.state_enabled, android.R.attr.state_pressed};


    private Drawable mBackgroundDrawable, mThumbDrawable;
    private Drawable mCurrentBackDrawable, mNextBackDrawable;
    private RectF mThumbRectF;
    private RectF mThumbDrawRectF;
    private CharSequence mOnText, mOffText;

    /**
     * from 0 - 1
     */
    private float mProgress;
    private int mThumbPadding;

    private long mAnimationDuration = DEFAULT_ANIMATION_DURATION;
    private ObjectAnimator mAnimator;

    private long mTouchDownTime;
    private int mTouchDownX;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SwitchButton(Context context) {
        this(context, null);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainAttrs(context, attrs);
        init();
    }

    private void obtainAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton);

        mThumbPadding = ta.getInt(R.styleable.SwitchButton_switchThumbPadding, (int) dip2px(2.5f));

        mBackgroundDrawable = ta.getDrawable(R.styleable.SwitchButton_switchBackground);
        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = new ColorDrawable(Color.parseColor("#4cd964"));
        }

        mThumbDrawable = ta.getDrawable(R.styleable.SwitchButton_switchThumbBackground);
        if (mThumbDrawable == null) {
            mThumbDrawable = new ColorDrawable(Color.WHITE);
        }

        ta.recycle();
    }

    private void init() {
        mThumbRectF = new RectF();
        mThumbDrawRectF = new RectF();
        mAnimator = ObjectAnimator.ofFloat(this, "progress", 0, 1).setDuration(mAnimationDuration);
        mAnimator.setDuration(mAnimationDuration);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownTime = System.currentTimeMillis();
                mTouchDownX = (int) event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                setProgress(getProgress() + (event.getX() - mTouchDownX) / (getWidth() - getHeight()));
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - mTouchDownTime < ViewConfiguration.getTapTimeout()) {
                    setChecked(!isChecked());
                    animateToState(isChecked());
                } else {
                    animateToState(getProgress() > .5f);
                    setChecked(getProgress() > .5f);
                }
                break;

            default:
                break;
        }

        return true;
    }

    /**
     * 开关滑动切换状态
     *
     * @param checked 开关是否打开状态
     */
    public void animateToState(boolean checked) {
        if (mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        if (checked) {
            mAnimator.setFloatValues(mProgress, 1);
        } else {
            mAnimator.setFloatValues(mProgress, 0);
        }

        mAnimator.start();
    }

    public void setProgress(@FloatRange(from = 0, to = 1) float progress) {
        if (progress > 1) {
            mProgress = 1;
        } else if (progress < 0) {
            mProgress = 0;
        } else {
            mProgress = progress;
        }

        invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBackgroundDrawable instanceof ColorDrawable) {
            mPaint.setColor(((ColorDrawable) mBackgroundDrawable).getColor());

            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), getHeight() >> 1, getHeight() >> 1, mPaint);
        } else if (mBackgroundDrawable instanceof StateListDrawable) {
            int alpha = (int) (255 * (isChecked() ? getProgress() : (1 - getProgress())));
            mCurrentBackDrawable.setAlpha(alpha);
            mCurrentBackDrawable.draw(canvas);
            alpha = 255 - alpha;
            mNextBackDrawable.setAlpha(alpha);
            mNextBackDrawable.draw(canvas);
        }

        mThumbDrawRectF.set(mThumbRectF);
        mThumbDrawRectF.offset(mProgress * (getWidth() - getHeight()), 0);
        if (mThumbDrawable instanceof ColorDrawable) {
            mPaint.setColor(((ColorDrawable) mThumbDrawable).getColor());

            canvas.drawRoundRect(mThumbDrawRectF, mThumbDrawRectF.height() / 2, mThumbDrawRectF.height() / 2, mPaint);
        } else {
            mThumbDrawable.setBounds((int) (mThumbDrawRectF.left), (int) (mThumbDrawRectF.top), (int) (mThumbDrawRectF.right), (int) (mThumbDrawRectF.bottom));
            mThumbDrawable.draw(canvas);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int[] state = isChecked() ? STATE_OFF : STATE_ON;

        if (mBackgroundDrawable instanceof StateListDrawable) {
            mBackgroundDrawable.setState(state);
            mNextBackDrawable = mBackgroundDrawable.getCurrent().mutate();

            setDrawableState(mBackgroundDrawable);
            mCurrentBackDrawable = mBackgroundDrawable.getCurrent().mutate();
        }
    }

    private void setDrawableState(Drawable drawable) {
        if (drawable != null) {
            int[] state = getDrawableState();
            drawable.setState(state);
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        resizeDrawableBounds();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resizeDrawableBounds();
    }

    private void resizeDrawableBounds() {
        mBackgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        mThumbRectF.set(mThumbPadding, mThumbPadding, getMeasuredHeight() - mThumbPadding, getMeasuredHeight() - mThumbPadding);
        mThumbDrawable.setBounds((int) (mThumbRectF.left), (int) (mThumbRectF.top), (int) (mThumbRectF.right), (int) (mThumbRectF.bottom));
    }

    public CharSequence getOnText() {
        return mOnText;
    }

    public void setOnText(CharSequence mOnText) {
        this.mOnText = mOnText;
    }

    public CharSequence getOffText() {
        return mOffText;
    }

    public void setOffText(CharSequence mOffText) {
        this.mOffText = mOffText;
    }

    public int getThumbPadding() {
        return mThumbPadding;
    }

    public void setThumbPadding(int mThumbPadding) {
        this.mThumbPadding = mThumbPadding;
    }

    public long getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(long mAnimationDuration) {
        this.mAnimationDuration = mAnimationDuration;
    }

    public float getProgress() {
        return mProgress;
    }

    /**
     * dp宽度转像素值
     */
    public static float dip2px(float dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, Resources.getSystem().getDisplayMetrics());
    }
}