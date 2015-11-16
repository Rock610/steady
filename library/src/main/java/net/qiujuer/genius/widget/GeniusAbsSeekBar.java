/*
 * Copyright (C) 2014 Qiujuer <qiujuer@live.cn>
 * WebSite http://www.qiujuer.net
 * Created 02/25/2015
 * Changed 03/01/2015
 * Version 2.0.0
 * GeniusEditText
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.qiujuer.genius.widget;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.qiujuer.genius.GeniusUI;
import net.qiujuer.genius.R;
import net.qiujuer.genius.drawable.AlmostRippleDrawable;
import net.qiujuer.genius.drawable.BalloonMarkerDrawable;
import net.qiujuer.genius.drawable.SeekBarDrawable;
import net.qiujuer.genius.widget.attribute.Attributes;
import net.qiujuer.genius.widget.attribute.SeekBarAttributes;

import java.util.Formatter;
import java.util.Locale;

/**
 * This abstract class use to SeekBar
 */
public abstract class GeniusAbsSeekBar extends View implements Attributes.AttributeChangeListener {
    /**
     * Interface to transform the current internal value of this GeniusSeekBar to anther one for the visualization.
     * <p/>
     * This will be used on the floating bubble to display a different value if needed.
     * <p/>
     * Using this in conjunction with {@link #setIndicatorFormatter(String)} you will be able to manipulate the
     * value seen by the user
     *
     * @see #setIndicatorFormatter(String)
     * @see #setNumericTransformer(GeniusAbsSeekBar.NumericTransformer)
     */
    public static abstract class NumericTransformer {
        /**
         * Return the desired value to be shown to the user.
         * This value will be formatted using the format specified by {@link #setIndicatorFormatter} before displaying it
         *
         * @param value The value to be transformed
         * @return The transformed int
         */
        public abstract int transform(int value);

        /**
         * Return the desired value to be shown to the user.
         * This value will be displayed 'as is' without further formatting.
         *
         * @param value The value to be transformed
         * @return A formatted string
         */
        public String transformToString(int value) {
            return String.valueOf(value);
        }

        /**
         * Used to indicate which transform will be used. If this method returns true,
         * {@link #transformToString(int)} will be used, otherwise {@link #transform(int)}
         * will be used
         */
        public boolean useStringTransform() {
            return false;
        }
    }


    // Default  NumericTransformer class
    private static class DefaultNumericTransformer extends NumericTransformer {

        @Override
        public int transform(int value) {
            return value;
        }
    }


    //We want to always use a formatter so the indicator numbers are "translated" to specific locales.
    private static final String DEFAULT_FORMATTER = "%d";

    private static final int PRESSED_STATE = android.R.attr.state_pressed;
    private static final int FOCUSED_STATE = android.R.attr.state_focused;
    private static final int PROGRESS_ANIMATION_DURATION = 250;
    private static final int INDICATOR_DELAY_FOR_TAPS = 150;

    private AlmostRippleDrawable mRipple;
    private SeekBarDrawable mSeekBarDrawable;

    private int mMax = 100;
    private int mMin = 0;
    private int mValue = 0;

    private int mKeyProgressIncrement = 1;
    private boolean mMirrorForRtl = false;
    private boolean mAllowTrackClick = true;
    //We use our own Formatter to avoid creating new instances on every progress change
    private Formatter mFormatter;
    private String mIndicatorFormatter;
    private NumericTransformer mNumericTransformer;
    private StringBuilder mFormatBuilder;
    private boolean mIsDragging;
    private int mDragOffset;

    private Rect mInvalidateRect = new Rect();
    private Rect mTempRect = new Rect();
    private GeniusPopupIndicator mIndicator;
    private ValueAnimator mPositionAnimator;
    private float mAnimationPosition;
    private int mAnimationTarget;
    private float mDownX;
    private float mTouchSlop;

    private SeekBarAttributes mAttributes;

    public GeniusAbsSeekBar(Context context) {
        this(context, null);
    }

    public GeniusAbsSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.DefaultSeekBarStyle);
    }

    public GeniusAbsSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setFocusable(true);
        setWillNotDraw(false);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // New
        final Resources resources = getResources();
        final ColorStateList transparent = ColorStateList.valueOf(Color.TRANSPARENT);

        mAttributes = new SeekBarAttributes(this, resources);

        mRipple = new AlmostRippleDrawable(transparent);
        mRipple.setCallback(this);

        mSeekBarDrawable = new SeekBarDrawable(transparent, transparent, transparent);
        mSeekBarDrawable.setCallback(this);

        if (!isInEditMode()) {
            mIndicator = new GeniusPopupIndicator(context);
            mIndicator.setListener(mFloaterListener);
        }

        // Set Size
        setTrackStroke(resources.getDimensionPixelSize(R.dimen.genius_seekBar_trackStroke));
        setScrubberStroke(resources.getDimensionPixelSize(R.dimen.genius_seekBar_scrubberStroke));
        setThumbRadius(resources.getDimensionPixelSize(R.dimen.genius_seekBar_thumbSize));
        setTouchRadius(resources.getDimensionPixelSize(R.dimen.genius_seekBar_touchSize));
        setTickRadius(resources.getDimensionPixelSize(R.dimen.genius_seekBar_tickSize));

        // Init
        init(attrs, defStyle);

        // End
        setNumericTransformer(new DefaultNumericTransformer());
        isRtl();

        if (attrs != null)
            setEnabled(attrs.getAttributeBooleanValue(GeniusUI.androidStyleNameSpace, "enabled", isEnabled()));
        else
            setEnabled(isEnabled());
    }

    private void init(AttributeSet attrs, int defStyle) {
        final Context context = getContext();
        final Resources resources = getResources();
        final boolean notEdit = !isInEditMode();

        ColorStateList trackColor = mAttributes.getTrackColor();
        ColorStateList thumbColor = mAttributes.getThumbColor();
        ColorStateList scrubberColor = mAttributes.getScrubberColor();
        ColorStateList rippleColor = mAttributes.getRippleColor();
        ColorStateList indicatorColor = mAttributes.getIndicatorColor();

        int textAppearanceId = R.style.DefaultBalloonMarkerTextAppearanceStyle;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GeniusSeekBar,
                    R.attr.GeniusSeekBarStyle, defStyle);

            // Getting common attributes
            int customTheme = a.getResourceId(R.styleable.GeniusSeekBar_g_theme, Attributes.DEFAULT_THEME);
            mAttributes.setTheme(customTheme, resources);

            // Values
            int max = a.getInteger(R.styleable.GeniusSeekBar_g_max, mMax);
            int min = a.getInteger(R.styleable.GeniusSeekBar_g_min, mMin);
            int value = a.getInteger(R.styleable.GeniusSeekBar_g_value, mValue);

            mMin = min;
            mMax = Math.max(min + 1, max);
            mValue = Math.max(min, Math.min(max, value));

            // Colors
            trackColor = a.getColorStateList(R.styleable.GeniusSeekBar_g_trackColor);
            thumbColor = a.getColorStateList(R.styleable.GeniusSeekBar_g_thumbColor);
            scrubberColor = a.getColorStateList(R.styleable.GeniusSeekBar_g_scrubberColor);
            rippleColor = a.getColorStateList(R.styleable.GeniusSeekBar_g_rippleColor);
            indicatorColor = a.getColorStateList(R.styleable.GeniusSeekBar_g_indicatorBackgroundColor);


            // Size
            int tickSize = a.getDimensionPixelSize(R.styleable.GeniusSeekBar_g_tickSize,
                    mSeekBarDrawable.getTickRadius());
            int thumbSize = a.getDimensionPixelSize(R.styleable.GeniusSeekBar_g_thumbSize,
                    mSeekBarDrawable.getThumbRadius());
            int touchSize = a.getDimensionPixelSize(R.styleable.GeniusSeekBar_g_touchSize,
                    mSeekBarDrawable.getTouchRadius());
            int trackStroke = a.getDimensionPixelSize(R.styleable.GeniusSeekBar_g_trackStroke,
                    mSeekBarDrawable.getTrackStroke());
            int scrubberStroke = a.getDimensionPixelSize(R.styleable.GeniusSeekBar_g_scrubberStroke,
                    mSeekBarDrawable.getScrubberStroke());

            // Set Size
            setTrackStroke(trackStroke);
            setScrubberStroke(scrubberStroke);
            setThumbRadius(thumbSize);
            setTouchRadius(touchSize);
            setTickRadius(tickSize);

            // Other
            mMirrorForRtl = a.getBoolean(R.styleable.GeniusSeekBar_g_mirrorForRtl, mMirrorForRtl);
            mAllowTrackClick = a.getBoolean(R.styleable.GeniusSeekBar_g_allowTrackClickToDrag, mAllowTrackClick);
            mIndicatorFormatter = a.getString(R.styleable.GeniusSeekBar_g_indicatorFormatter);

            textAppearanceId = a.getResourceId(R.styleable.GeniusSeekBar_g_indicatorTextAppearance, textAppearanceId);

            a.recycle();
        }

        // Init Colors
        if (rippleColor == null) {
            rippleColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{mAttributes.getColor(1)});
        } else {
            mAttributes.setRippleColor(rippleColor);
        }
        if (trackColor == null) {
            int[] colors = new int[]{mAttributes.getColor(4), mAttributes.getColor(5)};
            int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}, new int[]{-android.R.attr.state_enabled}};
            trackColor = new ColorStateList(states, colors);
        } else {
            mAttributes.setTrackColor(trackColor);
        }
        if (thumbColor == null) {
            int[] colors = new int[]{mAttributes.getColor(2), mAttributes.getColor(3)};
            int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}, new int[]{-android.R.attr.state_enabled}};
            thumbColor = new ColorStateList(states, colors);
        } else {
            mAttributes.setThumbColor(thumbColor);
        }
        if (scrubberColor == null) {
            int[] colors = new int[]{mAttributes.getColor(2), mAttributes.getColor(3)};
            int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}, new int[]{-android.R.attr.state_enabled}};
            scrubberColor = new ColorStateList(states, colors);
        } else {
            mAttributes.setScrubberColor(scrubberColor);
        }
        if (indicatorColor == null) {
            int[] colors = new int[]{mAttributes.getColor(2), mAttributes.getColor(1)};
            int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}, new int[]{android.R.attr.state_pressed}};
            indicatorColor = new ColorStateList(states, colors);
        } else {
            mAttributes.setIndicatorColor(indicatorColor);
        }

        // Set Colors
        mRipple.setColorStateList(rippleColor);
        mSeekBarDrawable.setTrackColor(trackColor);
        mSeekBarDrawable.setScrubberColor(scrubberColor);
        mSeekBarDrawable.setThumbColor(thumbColor);
        if (notEdit) {
            mIndicator.setIndicatorColor(indicatorColor);
            mIndicator.setIndicatorTextAppearance(textAppearanceId);
        }

        // Set Values
        mSeekBarDrawable.setNumSegments(mMax - mMin);
        updateKeyboardRange();
    }

    public void setTrackStroke(int trackStroke) {
        mSeekBarDrawable.setTrackStroke(trackStroke);
    }

    public void setScrubberStroke(int scrubberStroke) {
        mSeekBarDrawable.setScrubberStroke(scrubberStroke);
    }

    public void setThumbRadius(int thumbRadius) {
        mSeekBarDrawable.setThumbRadius(thumbRadius);
        if (!isInEditMode())
            mIndicator.setIndicatorClosedSize(thumbRadius * 2);
    }

    public void setTouchRadius(int touchRadius) {
        mSeekBarDrawable.setTouchRadius(touchRadius);
    }

    public void setTickRadius(int tickRadius) {
        mSeekBarDrawable.setTickRadius(tickRadius);
    }


    /**
     * Sets the current Indicator formatter string
     *
     * @param formatter Value formatter
     * @see String#format(String, Object...)
     * @see #setNumericTransformer(GeniusAbsSeekBar.NumericTransformer)
     */
    public void setIndicatorFormatter(@Nullable String formatter) {
        mIndicatorFormatter = formatter;
        updateProgressMessage(mValue);
    }

    /**
     * Sets the current {@link GeniusAbsSeekBar.NumericTransformer}
     *
     * @param transformer NumericTransformer transformer
     * @see #getNumericTransformer()
     */
    public void setNumericTransformer(@Nullable NumericTransformer transformer) {
        mNumericTransformer = transformer != null ? transformer : new DefaultNumericTransformer();
        //We need to refresh the PopupIndicator view
        if (!isInEditMode()) {
            if (mNumericTransformer.useStringTransform()) {
                mIndicator.setIndicatorSizes(mNumericTransformer.transformToString(mMax));
            } else {
                mIndicator.setIndicatorSizes(convertValueToMessage(mNumericTransformer.transform(mMax)));
            }
        }
        updateProgressMessage(mValue);
    }

    /**
     * Retrieves the current {@link GeniusAbsSeekBar.NumericTransformer}
     *
     * @return NumericTransformer
     * @see #setNumericTransformer
     */
    public NumericTransformer getNumericTransformer() {
        return mNumericTransformer;
    }

    /**
     * Sets the maximum value for this GeniusSeekBar
     * if the supplied argument is smaller than the Current MIN value,
     * the MIN value will be set to MAX-1
     * <p/>
     * <p>
     * Also if the current progress is out of the new range, it will be set to MIN
     * </p>
     *
     * @param max Progress max value
     * @see #setMin(int)
     * @see #setProgress(int)
     */
    public void setMax(int max) {
        mMax = max;
        if (mMax < mMin) {
            setMin(mMax - 1);
        }
        updateKeyboardRange();
        mSeekBarDrawable.setNumSegments(mMax - mMin);

        if (mValue < mMin || mValue > mMax) {
            setProgress(mMin);
        }
    }

    /**
     * Get the max value
     *
     * @return Progress max value
     */
    public int getMax() {
        return mMax;
    }

    /**
     * Sets the minimum value for this GeniusSeekBar
     * if the supplied argument is bigger than the Current MAX value,
     * the MAX value will be set to MIN+1
     * <p>
     * Also if the current progress is out of the new range, it will be set to MIN
     * </p>
     *
     * @param min Progress min value
     * @see #setMax(int)
     * @see #setProgress(int)
     */
    public void setMin(int min) {
        mMin = min;
        if (mMin > mMax) {
            setMax(mMin + 1);
        }
        updateKeyboardRange();
        mSeekBarDrawable.setNumSegments(mMax - mMin);

        if (mValue < mMin || mValue > mMax) {
            setProgress(mMin);
        }
    }

    /**
     * Get the min value
     *
     * @return Progress min value
     */
    public int getMin() {
        return mMin;
    }

    /**
     * Sets the current progress for this GeniusSeekBar
     * The supplied argument will be capped to the current MIN-MAX range
     *
     * @param progress Progress Value
     * @see #setMax(int)
     * @see #setMin(int)
     */
    public void setProgress(int progress) {
        setProgress(progress, false, -1);
    }

    /**
     * Get the current progress
     *
     * @return the current progress :-P
     */
    public int getProgress() {
        return mValue;
    }


    /**
     * Sets the color of the seek thumb, as well as the color of the popup indicator.
     *
     * @param startColor The color the seek thumb will be changed to
     * @param endColor   The color the popup indicator will be changed to
     */
    public void setThumbColor(int startColor, int endColor) {
        mSeekBarDrawable.setThumbColor(ColorStateList.valueOf(startColor));
        mIndicator.setColors(startColor, endColor);
    }

    /**
     * Sets the color of the SeekBar scrubber
     *
     * @param color The color the track will be changed to
     */
    public void setScrubberColor(int color) {
        mSeekBarDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int height = mSeekBarDrawable.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthSize, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            removeCallbacks(mShowIndicatorRunnable);
            if (!isInEditMode()) {
                mIndicator.dismissComplete();
            }
            updateFromDrawableState();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mSeekBarDrawable.setBounds(getPaddingLeft(),
                getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());

        //Update the thumb position after size changed
        updateThumbPosForScale(-1);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mSeekBarDrawable.draw(canvas);
        mRipple.draw(canvas);

    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int actionMasked = MotionEventCompat.getActionMasked(event);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                startDragging(event, isInScrollingContainer());
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    updateDragging(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mDownX) > mTouchSlop) {
                        startDragging(event, false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                if (mSeekBarDrawable.isHaveTick())
                    animateSetProgress();
                onStopTrackingTouch();

                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        boolean handled = false;
        boolean isAdd = false;
        if (isEnabled()) {
            int progress = getAnimatedProgress();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = true;
                    isAdd = isRtl();
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = true;
                    isAdd = !isRtl();
                    break;
            }

            if (handled) {
                if (isAdd) {
                    if (progress < mMax)
                        animateSetProgress(progress + mKeyProgressIncrement);
                } else {
                    if (progress > mMin)
                        animateSetProgress(progress - mKeyProgressIncrement);

                }
            }
        }

        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mSeekBarDrawable || who == mRipple || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateFromDrawableState();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mShowIndicatorRunnable);
        if (!isInEditMode()) {
            mIndicator.dismissComplete();
        }
    }

    @Override
    public void onThemeChange() {
        init(null, R.style.DefaultSeekBarStyle);
    }

    @Override
    public SeekBarAttributes getAttributes() {
        return mAttributes;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public boolean isRtl() {
        boolean isRtl = (ViewCompat.getLayoutDirection(this) == LAYOUT_DIRECTION_RTL) && mMirrorForRtl;
        mSeekBarDrawable.setRtl(isRtl);
        return isRtl;
    }

    private boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p != null && p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private void setProgress(int value, boolean fromUser, float scale) {
        value = Math.max(mMin, Math.min(mMax, value));
        if (isAnimationRunning()) {
            mPositionAnimator.cancel();
        }

        if (mValue != value) {
            mValue = value;
            onProgressChanged(value, fromUser);
            updateProgressMessage(value);
        }
        updateThumbPosForScale(scale);
    }

    private void updateKeyboardRange() {
        int range = mMax - mMin;
        if ((mKeyProgressIncrement == 0) || (range / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            mKeyProgressIncrement = Math.max(1, Math.round((float) range / 20));
        }
    }

    private void updateFromDrawableState() {
        int[] state = getDrawableState();
        boolean focused = false;
        boolean pressed = false;
        for (int i : state) {
            if (i == FOCUSED_STATE) {
                focused = true;
            } else if (i == PRESSED_STATE) {
                pressed = true;
            }
        }
        if (isEnabled() && (focused || pressed)) {
            //We want to add a small delay here to avoid
            //POPing in/out on simple taps
            removeCallbacks(mShowIndicatorRunnable);
            postDelayed(mShowIndicatorRunnable, INDICATOR_DELAY_FOR_TAPS);
        } else {
            hideFloater();
        }

        mRipple.setState(state);
        mSeekBarDrawable.setState(state);
    }

    private void updateProgressMessage(int value) {
        if (!isInEditMode()) {
            if (mNumericTransformer.useStringTransform()) {
                mIndicator.setValue(mNumericTransformer.transformToString(value));
            } else {
                mIndicator.setValue(convertValueToMessage(mNumericTransformer.transform(value)));
            }
        }
    }

    private String convertValueToMessage(int value) {
        String format = mIndicatorFormatter != null ? mIndicatorFormatter : DEFAULT_FORMATTER;
        //We're trying to re-use the Formatter here to avoid too much memory allocations
        //But I'm not completey sure if it's doing anything good... :(
        //Previously, this condition was wrong so the Formatter was always re-created
        //But as I fixed the condition, the formatter started outputting trash characters from previous
        //calls, so I mark the StringBuilder as empty before calling format again.

        //Anyways, I see the memory usage still go up on every call to this method
        //and I have no clue on how to fix that... damn Strings...
        if (mFormatter == null || !mFormatter.locale().equals(Locale.getDefault())) {
            int bufferSize = format.length() + String.valueOf(mMax).length();
            if (mFormatBuilder == null) {
                mFormatBuilder = new StringBuilder(bufferSize);
            } else {
                mFormatBuilder.ensureCapacity(bufferSize);
            }
            mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        } else {
            mFormatBuilder.setLength(0);
        }
        return mFormatter.format(format, value).toString();
    }

    private void startDragging(MotionEvent ev, boolean ignoreTrackIfInScrollContainer) {
        final Rect bounds = mTempRect;

        mSeekBarDrawable.copyTouchBounds(bounds);

        //Grow the current thumb rect for a bigger touch area
        boolean isDragging = (bounds.contains((int) ev.getX(), (int) ev.getY()));
        if (!isDragging && mAllowTrackClick && !ignoreTrackIfInScrollContainer) {
            //If the user clicked outside the thumb, we compute the current position
            //and force an immediate drag to it.
            isDragging = true;
            mDragOffset = bounds.width() / 2;
            updateDragging(ev);
            //As the thumb may have moved, get the bounds again
            mSeekBarDrawable.setHotScale(mSeekBarDrawable.getHotScale());
        }
        if (isDragging) {
            onStartTrackingTouch();
            setHotspot(ev.getX(), ev.getY());
            mDragOffset = (int) (ev.getX() - bounds.centerX());
        }
    }

    private int getAnimatedProgress() {
        return isAnimationRunning() ? getAnimationTarget() : getProgress();
    }

    private boolean isAnimationRunning() {
        return mPositionAnimator != null && mPositionAnimator.isRunning();
    }

    private void animateSetProgress() {
        final float curProgress = isAnimationRunning() ? getAnimationPosition() : mSeekBarDrawable.getHotScale() * (mMax - mMin) + mMin;

        mAnimationTarget = getProgress();

        animateSetProgress(curProgress);
    }

    private void animateSetProgress(int progress) {
        final float curProgress = isAnimationRunning() ? getAnimationPosition() : getProgress();

        if (progress < mMin) {
            progress = mMin;
        } else if (progress > mMax) {
            progress = mMax;
        }

        mAnimationTarget = progress;

        animateSetProgress(curProgress);
    }

    private void animateSetProgress(float curProgress) {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
            mPositionAnimator.setFloatValues(curProgress, mAnimationTarget);
        } else {
            mPositionAnimator = ValueAnimator.ofFloat(curProgress, mAnimationTarget);
            mPositionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mAnimationPosition = (Float) animation.getAnimatedValue();
                    float currentScale = (mAnimationPosition - mMin) / (float) (mMax - mMin);
                    updateProgressFromAnimation(currentScale);
                }
            });
            mPositionAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        }
        mPositionAnimator.start();
    }

    private int getAnimationTarget() {
        return mAnimationTarget;
    }

    private float getAnimationPosition() {
        return mAnimationPosition;
    }

    private void updateDragging(MotionEvent ev) {
        setHotspot(ev.getX(), ev.getY());
        int x = (int) ev.getX();

        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();

        int posX = x - mDragOffset;
        if (posX < left) {
            posX = left;
        } else if (posX > right) {
            posX = right;
        }

        int available = right - left;
        float scale = (float) (posX - left) / (float) available;

        if (isRtl()) {
            scale = 1f - scale;
        }
        int progress = Math.round((scale * (mMax - mMin)) + mMin);
        setProgress(progress, true, scale);
    }

    private void updateProgressFromAnimation(float scale) {
        int progress = Math.round((scale * (mMax - mMin)) + mMin);
        //we don't want to just call setProgress here to avoid the animation being cancelled,
        //and this position is not bound to a real progress value but interpolated
        if (progress != getProgress()) {
            mValue = progress;
            onProgressChanged(mValue, true);
            updateProgressMessage(progress);
        }
        updateThumbPosForScale(scale);
    }

    private void updateThumbPosForScale(float scale) {
        // SeekBar
        if (scale == -1) {
            scale = (mValue - mMin) / (float) (mMax - mMin);
        }
        mSeekBarDrawable.setHotScale(scale);

        // Indicator Move
        final Rect finalBounds = mTempRect;
        mSeekBarDrawable.copyTouchBounds(finalBounds);
        if (!isInEditMode()) {
            mIndicator.move(finalBounds.centerX());
        }

        // Ripple
        mRipple.setBounds(finalBounds.left, finalBounds.top, finalBounds.right, finalBounds.bottom);

        // Invalidate
        mSeekBarDrawable.copyBounds(mInvalidateRect);
        invalidate(mInvalidateRect);
    }

    private void setHotspot(float x, float y) {
        DrawableCompat.setHotspot(mRipple, x, y);
    }

    private void attemptClaimDrag() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void showFloater() {
        if (!isInEditMode()) {
            mSeekBarDrawable.animateToPressed();
            mIndicator.showIndicator(this, mSeekBarDrawable.getPosPoint());
            onShowBubble();
        }
    }

    private void hideFloater() {
        removeCallbacks(mShowIndicatorRunnable);
        if (!isInEditMode()) {
            mIndicator.dismiss();
            onHideBubble();
        }
    }

    private Runnable mShowIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            showFloater();
        }
    };

    private final BalloonMarkerDrawable.MarkerAnimationListener mFloaterListener = new BalloonMarkerDrawable.MarkerAnimationListener() {
        @Override
        public void onClosingComplete() {
            mSeekBarDrawable.animateToNormal();
        }

        @Override
        public void onOpeningComplete() {

        }

    };


    /**
     * When the {@link GeniusAbsSeekBar} enters pressed or focused state
     * the bubble with the value will be shown, and this method called
     * <p>
     * Subclasses may override this to add functionality around this event
     * </p>
     */
    protected void onShowBubble() {
    }

    /**
     * When the {@link GeniusAbsSeekBar} exits pressed or focused state
     * the bubble with the value will be hidden, and this method called
     * <p>
     * Subclasses may override this to add functionality around this event
     * </p>
     */
    protected void onHideBubble() {
    }

    /**
     * This is called when the user has started touching this widget.
     */
    protected void onStartTrackingTouch() {
        mIsDragging = true;
        setPressed(true);
        attemptClaimDrag();
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    protected void onStopTrackingTouch() {
        mIsDragging = false;
        setPressed(false);
    }

    /**
     * When the {@link GeniusAbsSeekBar} value changes this method is called
     * <p>
     * Subclasses may override this to add functionality around this event
     * without having to specify a listener
     * </p>
     */
    protected void onProgressChanged(int value, boolean fromUser) {
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        CustomState state = new CustomState(superState);
        state.progress = getProgress();
        state.max = mMax;
        state.min = mMin;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(CustomState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        CustomState customState = (CustomState) state;
        setMin(customState.min);
        setMax(customState.max);
        setProgress(customState.progress);
        super.onRestoreInstanceState(customState.getSuperState());
    }

    static class CustomState extends BaseSavedState {
        private int progress;
        private int max;
        private int min;

        public CustomState(Parcel source) {
            super(source);
            progress = source.readInt();
            max = source.readInt();
            min = source.readInt();
        }

        public CustomState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(progress);
            dest.writeInt(max);
            dest.writeInt(min);
        }

        public static final Creator<CustomState> CREATOR =
                new Creator<CustomState>() {

                    @Override
                    public CustomState[] newArray(int size) {
                        return new CustomState[size];
                    }

                    @Override
                    public CustomState createFromParcel(Parcel incoming) {
                        return new CustomState(incoming);
                    }
                };
    }

}
