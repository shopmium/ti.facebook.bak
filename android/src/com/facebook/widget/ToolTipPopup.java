/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.widget;

import java.lang.ref.WeakReference;

//import com.facebook.android.R;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.facebook.internal.Utility;

public class ToolTipPopup {

    public static enum Style {
        /**
         * The tool tip will be shown with a blue style; including a blue background and blue
         * arrows.
         */
        BLUE,

        /**
         * The tool tip will be shown with a black style; including a black background and black
         * arrows.
         */
        BLACK
    }

    /**
     * The default time that the tool tip will be displayed
     */
    public static final long DEFAULT_POPUP_DISPLAY_TIME = 6000;

    private final String mText;
    private final WeakReference<View> mAnchorViewRef;
    private final Context mContext;
    private PopupContentView mPopupContent;
    private PopupWindow mPopupWindow;
    private Style mStyle = Style.BLUE;
    private long mNuxDisplayTime = DEFAULT_POPUP_DISPLAY_TIME;

    private final ViewTreeObserver.OnScrollChangedListener mScrollListener =
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (mAnchorViewRef.get() != null &&
                            mPopupWindow != null &&
                            mPopupWindow.isShowing()) {
                        if (mPopupWindow.isAboveAnchor()) {
                            mPopupContent.showBottomArrow();
                        } else {
                            mPopupContent.showTopArrow();
                        }
                    }
                }
            };

    /**
     * Create a new ToolTipPopup
     * @param text The text to be displayed in the tool tip
     * @param anchor The view to anchor this tool tip to.
     */
    public ToolTipPopup(String text, View anchor) {
        mText = text;
        mAnchorViewRef = new WeakReference<View>(anchor);
        mContext = anchor.getContext();
    }

    /**
     * Sets the {@link Style} of this tool tip.
     * @param mStyle
     */
    public void setStyle(Style mStyle) {
        this.mStyle = mStyle;
    }

    /**
     * Display this tool tip to the user
     */
    public void show() {
        if (mAnchorViewRef.get() != null) {
            mPopupContent = new PopupContentView(mContext);
            TextView body = (TextView) mPopupContent.findViewById(
                    Utility.resId_toolTipBubbleViewTextBody);
            body.setText(mText);
            if (mStyle == Style.BLUE) {
                mPopupContent.bodyFrame.setBackgroundResource(
                        Utility.resId_toolTipBlueBackground);
                mPopupContent.bottomArrow.setImageResource(
                        Utility.resId_toolTipBlueBottomNub);
                mPopupContent.topArrow.setImageResource(
                        Utility.resId_toolTipBlueTopNub);
                mPopupContent.xOut.setImageResource(Utility.resId_toolTipBlueXout);
            } else {
                mPopupContent.bodyFrame.setBackgroundResource(
                        Utility.resId_toolTipBlackBackground);
                mPopupContent.bottomArrow.setImageResource(
                        Utility.resId_toolTipBlackBottomNub);
                mPopupContent.topArrow.setImageResource(
                        Utility.resId_toolTipBlackTopNub);
                mPopupContent.xOut.setImageResource(Utility.resId_toolTipBlackXout);
            }

            final Window window = ((Activity) mContext).getWindow();
            final View decorView = window.getDecorView();
            final int decorWidth = decorView.getWidth();
            final int decorHeight = decorView.getHeight();
            registerObserver();
            mPopupContent.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(decorWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(decorHeight, View.MeasureSpec.AT_MOST));
            mPopupWindow = new PopupWindow(
                    mPopupContent,
                    mPopupContent.getMeasuredWidth(),
                    mPopupContent.getMeasuredHeight());
            mPopupWindow.showAsDropDown(mAnchorViewRef.get());
            updateArrows();
            if (mNuxDisplayTime > 0) {
                mPopupContent.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                }, mNuxDisplayTime);
            }
            mPopupWindow.setTouchable(true);
            mPopupContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    /**
     * Set the time (in milliseconds) the tool tip will be displayed. Any number less than or equal
     * to 0 will cause the tool tip to be displayed indefinitely
     * @param displayTime The amount of time (in milliseconds) to display the tool tip
     */
    public void setNuxDisplayTime(long displayTime) {
        this.mNuxDisplayTime = displayTime;
    }

    private void updateArrows() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            if (mPopupWindow.isAboveAnchor()) {
                mPopupContent.showBottomArrow();
            } else {
                mPopupContent.showTopArrow();
            }
        }
    }

    /**
     * Dismiss the tool tip
     */
    public void dismiss() {
        unregisterObserver();
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    private void registerObserver() {
        unregisterObserver();
        if (mAnchorViewRef.get() != null) {
            mAnchorViewRef.get().getViewTreeObserver().addOnScrollChangedListener(mScrollListener);
        }
    }

    private void unregisterObserver() {
        if (mAnchorViewRef.get() != null) {
            mAnchorViewRef.get().getViewTreeObserver().removeOnScrollChangedListener(mScrollListener);
        }
    }

    private class PopupContentView extends FrameLayout {
        private ImageView topArrow;
        private ImageView bottomArrow;
        private View bodyFrame;
        private ImageView xOut;

        public PopupContentView(Context context) {
            super(context);
            init();
        }

        private void init() {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(Utility.resId_toolTipBubble, this);
            topArrow = (ImageView) findViewById(Utility.resId_toolTipBubbleViewTop);
            bottomArrow = (ImageView) findViewById(
                    Utility.resId_toolTipBubbleViewBottom);
            bodyFrame = findViewById(Utility.resId_toolTipBodyFrame);
            xOut = (ImageView) findViewById(Utility.resId_toolTipButtonXout);
        }

        public void showTopArrow() {
            topArrow.setVisibility(View.VISIBLE);
            bottomArrow.setVisibility(View.INVISIBLE);
        }

        public void showBottomArrow() {
            topArrow.setVisibility(View.INVISIBLE);
            bottomArrow.setVisibility(View.VISIBLE);
        }

        // Expose so popup content can be sized
        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
