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


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

@SuppressWarnings("UnusedDeclaration")
public class GeniusSeekBar extends GeniusAbsSeekBar {

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar  The GeniusSeekBar
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link GeniusAbsSeekBar#setMax(int)}. (The default value for max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(GeniusSeekBar seekBar, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the SeekBar.
         *
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(GeniusSeekBar seekBar);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the SeekBar.
         *
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(GeniusSeekBar seekBar);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public GeniusSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GeniusSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GeniusSeekBar(Context context) {
        super(context);
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     * And provides notifications of when the AbsSeekBar shows/hides the bubble indicator.
     *
     * @param l The seek bar notification listener
     * @see GeniusSeekBar.OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    @Override
    protected void onProgressChanged(int scale, boolean fromUser) {
        super.onProgressChanged(scale, fromUser);

        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(this, scale, fromUser);
        }
    }

    @Override
    protected void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    @Override
    protected void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(GeniusSeekBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(GeniusSeekBar.class.getName());
    }
}
