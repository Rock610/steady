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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.qiujuer.genius.R;
import net.qiujuer.genius.drawable.BalloonMarkerDrawable;
import net.qiujuer.genius.widget.compat.GeniusCompat;

/**
 * This is a BalloonMarker
 */
public class GeniusBalloonMarker extends ViewGroup implements BalloonMarkerDrawable.MarkerAnimationListener {
    private static final int PADDING_DP = 4;
    private static final int ELEVATION_DP = 8;
    private static final int SEPARATION_DP = 22;
    //The TextView to show the info
    private TextView mNumber;
    //The max width of this View
    private int mWidth;
    //some distance between the thumb and our bubble marker.
    //This will be added to our measured height
    private int mSeparation;
    BalloonMarkerDrawable mBalloonMarkerDrawable;

    public GeniusBalloonMarker(Context context) {
        this(context, null);
    }

    public GeniusBalloonMarker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.GeniusBalloonMarkerStyle);
    }

    public GeniusBalloonMarker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, "0");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public GeniusBalloonMarker(Context context, AttributeSet attrs, int defStyleAttr, String maxValue) {
        super(context, attrs, defStyleAttr);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int padding = (int) (PADDING_DP * displayMetrics.density) * 2;
        mNumber = new TextView(context);
        //Add some padding to this textView so the bubble has some space to breath
        mNumber.setPadding(padding, 0, padding, 0);
        mNumber.setGravity(Gravity.CENTER);
        mNumber.setText(maxValue);
        mNumber.setMaxLines(1);
        mNumber.setSingleLine(true);
        GeniusCompat.setTextDirection(mNumber, TEXT_DIRECTION_LOCALE);
        mNumber.setVisibility(View.INVISIBLE);

        //add some padding for the elevation shadow not to be clipped
        //I'm sure there are better ways of doing this...
        setPadding(padding, padding, padding, padding);

        resetSizes(maxValue);

        mSeparation = (int) (SEPARATION_DP * displayMetrics.density);

        mBalloonMarkerDrawable = new BalloonMarkerDrawable(ColorStateList.valueOf(Color.TRANSPARENT), 0);
        mBalloonMarkerDrawable.setCallback(this);
        mBalloonMarkerDrawable.setMarkerListener(this);
        mBalloonMarkerDrawable.setExternalOffset(padding);

        GeniusCompat.setOutlineProvider(this, mBalloonMarkerDrawable);


        if (attrs != null) {

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GeniusBalloonMarker,
                    R.attr.GeniusBalloonMarkerStyle, R.style.DefaultBalloonMarkerStyle);
            int textAppearanceId = a.getResourceId(R.styleable.GeniusBalloonMarker_g_markerTextAppearance,
                    R.style.DefaultBalloonMarkerTextAppearanceStyle);

            setTextAppearance(textAppearanceId);

            ColorStateList color = a.getColorStateList(R.styleable.GeniusBalloonMarker_g_markerBackgroundColor);
            setBackgroundColor(color);

            //Elevation for android 5+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                float elevation = a.getDimension(R.styleable.GeniusBalloonMarker_g_markerElevation, ELEVATION_DP * displayMetrics.density);
                ViewCompat.setElevation(this, elevation);
            }
            a.recycle();
        }
    }

    public void setTextAppearance(int resid) {
        mNumber.setTextAppearance(getContext(), resid);
    }

    public void setBackgroundColor(ColorStateList color) {
        mBalloonMarkerDrawable.setColorStateList(color);
    }

    public void setClosedSize(float closedSize) {
        mBalloonMarkerDrawable.setClosedStateSize(closedSize);
    }

    public void resetSizes(String maxValue) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        //Account for negative numbers... is there any proper way of getting the biggest string between our range????
        mNumber.setText("-" + maxValue);
        //Do a first forced measure call for the TextView (with the biggest text content),
        //to calculate the max width and use always the same.
        //this avoids the TextView from shrinking and growing when the text content changes
        int wSpec = MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, MeasureSpec.AT_MOST);
        int hSpec = MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, MeasureSpec.AT_MOST);
        mNumber.measure(wSpec, hSpec);
        mWidth = Math.max(mNumber.getMeasuredWidth(), mNumber.getMeasuredHeight());
        removeView(mNumber);
        addView(mNumber, new FrameLayout.LayoutParams(mWidth, mWidth, Gravity.LEFT | Gravity.TOP));
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        mBalloonMarkerDrawable.draw(canvas);
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int widthSize = mWidth + getPaddingLeft() + getPaddingRight();
        int heightSize = mWidth + getPaddingTop() + getPaddingBottom();
        //This diff is the basic calculation of the difference between
        //a square side size and its diagonal
        //this helps us account for the visual offset created by MarkerDrawable
        //when leaving one of the corners un-rounded
        int diff = (int) ((1.41f * mWidth) - mWidth) / 2;
        setMeasuredDimension(widthSize, heightSize + diff + mSeparation);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int bottom = getHeight() - getPaddingBottom();
        //the TetView is always layout at the top
        mNumber.layout(left, top, left + mWidth, top + mWidth);
        //the MarkerDrawable uses the whole view, it will adapt itself...
        // or it seems so...
        mBalloonMarkerDrawable.setBounds(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBalloonMarkerDrawable || super.verifyDrawable(who);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //HACK: Sometimes, the animateOpen() call is made before the View is attached
        //so the drawable cannot schedule itself to run the animation
        //I think we can call it here safely.
        //I've seen it happen in android 2.3.7
        animateOpen();
    }

    public void setValue(CharSequence value) {
        mNumber.setText(value);
    }

    public CharSequence getValue() {
        return mNumber.getText();
    }

    public void animateOpen() {
        mBalloonMarkerDrawable.stop();
        mBalloonMarkerDrawable.animateToPressed();
    }

    public void animateClose() {
        mBalloonMarkerDrawable.stop();
        ViewCompat.animate(mNumber)
                .alpha(0f)
                .setDuration(100)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        //We use INVISIBLE instead of GONE to avoid a requestLayout
                        mNumber.setVisibility(View.INVISIBLE);
                        mBalloonMarkerDrawable.animateToNormal();
                    }
                }).start();
    }

    @Override
    public void onOpeningComplete() {
        mNumber.setVisibility(View.VISIBLE);
        ViewCompat.animate(mNumber)
                .alpha(1f)
                .setDuration(100)
                .start();
        if (getParent() instanceof BalloonMarkerDrawable.MarkerAnimationListener) {
            ((BalloonMarkerDrawable.MarkerAnimationListener) getParent()).onOpeningComplete();
        }
    }

    @Override
    public void onClosingComplete() {
        if (getParent() instanceof BalloonMarkerDrawable.MarkerAnimationListener) {
            ((BalloonMarkerDrawable.MarkerAnimationListener) getParent()).onClosingComplete();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBalloonMarkerDrawable.stop();
    }

    public void setColors(int startColor, int endColor) {
        mBalloonMarkerDrawable.setColors(startColor, endColor);
    }
}
