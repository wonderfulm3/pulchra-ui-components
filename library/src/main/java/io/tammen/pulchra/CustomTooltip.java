package io.tammen.pulchra;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Created by Tammen Bruccoleri on 11/6/16.
 *
 * Building off the shoulders of giants
 * https://github.com/douglasjunior/android-simple-tooltip
 */

public class CustomTooltip implements PopupWindow.OnDismissListener {

    private final String TAG = this.getClass().getSimpleName();

    // Default Resources
    private static final int defaultPopupWindowStyleRes = android.R.attr.popupWindowStyle;
    private static final int defaultTextAppearanceRes = R.style.customtooltip_default;
    private static final int defaultBackgroundColorRes = R.color.customtooltip_background;
    private static final int defaultTextColorRes = R.color.customtooltip_text;
    private static final int defaultArrowColorRes = R.color.customtooltip_arrow;
    private static final int defaultMarginRes = R.dimen.customtooltip_margin;
    private static final int defaultPaddingRes = R.dimen.customtooltip_padding;
    private static final int defaultAnimationPaddingRes = R.dimen.customtooltip_animation_padding;
    private static final int defaultAnimationDurationRes = R.integer.customtooltip_animation_duration;
    private static final int defaultArrowWidthRes = R.dimen.customtooltip_arrow_width;
    private static final int defaultArrowHeightRes = R.dimen.customtooltip_arrow_height;

    private final Context context;
    private OnDismissListener mOnDismissListener;
    private OnShowListener mOnShowListener;
    private PopupWindow mPopupWindow;
    private final int mGravity;
    private final int mArrowDirection;
    private final boolean mDismissOnInsideTouch;
    private final boolean mDismissOnOutsideTouch;
    private final boolean mModal;
    private final View mContentView;
    private View mContentLayout;
    @IdRes
    private final int mTextViewId;
    private final CharSequence mText;
    private final View mAnchorView;
    private final boolean mTransparentOverlay;
    private final float mMaxWidth;
    private View mOverlay;
    private ViewGroup mRootView;
    private final boolean mShowArrow;
    private ImageView mArrowView;
    private final Drawable mArrowDrawable;
    private final boolean mAnimated;
    private AnimatorSet mAnimator;
    private final float mMargin;
    private final float mPadding;
    private final float mAnimationPadding;
    private final long mAnimationDuration;
    private final float mArrowWidth;
    private final float mArrowHeight;
    private boolean dismissed = false;

    private CustomTooltip(Builder builder) {
        context = builder.context;
        mGravity = builder.gravity;
        mArrowDirection = builder.arrowDirection;
        mDismissOnInsideTouch = builder.dismissOnInsideTouch;
        mDismissOnOutsideTouch = builder.dismissOnOutsideTouch;
        mModal = builder.modal;
        mContentView = builder.contentView;
        mTextViewId = builder.textViewId;
        mText = builder.text;
        mAnchorView = builder.anchorView;
        mTransparentOverlay = builder.transparentOverlay;
        mMaxWidth = builder.maxWidth;
        mShowArrow = builder.showArrow;
        mArrowWidth = builder.arrowWidth;
        mArrowHeight = builder.arrowHeight;
        mArrowDrawable = builder.arrowDrawable;
        mAnimated = builder.animated;
        mMargin = builder.margin;
        mPadding = builder.padding;
        mAnimationPadding = builder.animationPadding;
        mAnimationDuration = builder.animationDuration;
        mOnDismissListener = builder.onDismissListener;
        mOnShowListener = builder.onShowListener;
        mRootView = (ViewGroup) mAnchorView.getRootView();

        init();
    }

    private void init() {
        configPopupWindow();
        configContentView();
    }

    private void configPopupWindow() {
        mPopupWindow = new PopupWindow(context, null, defaultPopupWindowStyleRes);
        mPopupWindow.setOnDismissListener(this);
        mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setClippingEnabled(false);
    }

    public void show() {
        verifyDismissed();

        mContentLayout.getViewTreeObserver().addOnGlobalLayoutListener(mLocationLayoutListener);
        mContentLayout.getViewTreeObserver().addOnGlobalLayoutListener(mAutoDismissLayoutListener);

        mRootView.post(new Runnable() {
            @Override
            public void run() {
                if (mRootView.isShown())
                    mPopupWindow.showAtLocation(mRootView, Gravity.NO_GRAVITY, mRootView.getWidth(), mRootView.getHeight());
                else
                    Log.e(TAG, "Tooltip cannot be shown, root view is invalid or has been closed.");
            }
        });
    }

    private void verifyDismissed() {
        if (dismissed) {
            throw new IllegalArgumentException("Tooltip has ben dismissed.");
        }
    }

    private void createOverlay() {
        mOverlay = mTransparentOverlay ? new View(context) : new OverlayView(context, mAnchorView);
        mOverlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mOverlay.setOnTouchListener(mOverlayTouchListener);
        mRootView.addView(mOverlay);
    }

    private PointF calculePopupLocation() {
        PointF location = new PointF();

        final RectF anchorRect = CustomTooltipUtil.calculeRectInWindow(mAnchorView);
        final PointF anchorCenter = new PointF(anchorRect.centerX(), anchorRect.centerY());

        switch (mGravity) {
            case Gravity.START:
                location.x = anchorRect.left - mPopupWindow.getContentView().getWidth() - mMargin;
                location.y = anchorCenter.y - mPopupWindow.getContentView().getHeight() / 2f;
                break;
            case Gravity.END:
                location.x = anchorRect.right + mMargin;
                location.y = anchorCenter.y - mPopupWindow.getContentView().getHeight() / 2f;
                break;
            case Gravity.TOP:
                location.x = anchorCenter.x - mPopupWindow.getContentView().getWidth() / 2f;
                location.y = anchorRect.top - mPopupWindow.getContentView().getHeight() - mMargin;
                break;
            case Gravity.BOTTOM:
                location.x = anchorCenter.x - mPopupWindow.getContentView().getWidth() / 2f;
                location.y = anchorRect.bottom + mMargin;
                break;
            case Gravity.CENTER:
                location.x = anchorCenter.x - mPopupWindow.getContentView().getWidth() / 2f;
                location.y = anchorCenter.y - mPopupWindow.getContentView().getHeight() / 2f;
                break;
            default:
                throw new IllegalArgumentException("Gravity must have be CENTER, START, END, TOP or BOTTOM.");
        }

        return location;
    }

    private void configContentView() {
        if (mContentView instanceof TextView) {
            TextView tv = (TextView) mContentView;
            tv.setText(mText);
        } else {
            TextView tv = (TextView) mContentView.findViewById(mTextViewId);
            if (tv != null)
                tv.setText(mText);
        }

        mContentView.setPadding((int) mPadding, (int) mPadding, (int) mPadding, (int) mPadding);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(mArrowDirection == ArrowDrawable.LEFT || mArrowDirection == ArrowDrawable.RIGHT ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        int layoutPadding = (int) (mAnimated ? mAnimationPadding : 0);
        linearLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding);

        if (mShowArrow) {
            mArrowView = new ImageView(context);
            mArrowView.setImageDrawable(mArrowDrawable);
            LinearLayout.LayoutParams arrowLayoutParams;

            if (mArrowDirection == ArrowDrawable.TOP || mArrowDirection == ArrowDrawable.BOTTOM) {
                arrowLayoutParams = new LinearLayout.LayoutParams((int) mArrowWidth, (int) mArrowHeight, 0);
            } else {
                arrowLayoutParams = new LinearLayout.LayoutParams((int) mArrowHeight, (int) mArrowWidth, 0);
            }

            arrowLayoutParams.gravity = Gravity.CENTER;
            mArrowView.setLayoutParams(arrowLayoutParams);

            if (mArrowDirection == ArrowDrawable.BOTTOM || mArrowDirection == ArrowDrawable.RIGHT) {
                linearLayout.addView(mContentView);
                linearLayout.addView(mArrowView);
            } else {
                linearLayout.addView(mArrowView);
                linearLayout.addView(mContentView);
            }
        } else {
            linearLayout.addView(mContentView);
        }

        LinearLayout.LayoutParams contentViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        contentViewParams.gravity = Gravity.CENTER;
        mContentView.setLayoutParams(contentViewParams);

        if (mDismissOnInsideTouch || mDismissOnOutsideTouch)
            mContentView.setOnTouchListener(mPopupWindowTouchListener);

        mContentLayout = linearLayout;
        mContentLayout.setVisibility(View.INVISIBLE);
        mPopupWindow.setContentView(mContentLayout);
    }

    public void dismiss() {
        if (dismissed)
            return;

        dismissed = true;
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    public <T extends View> T findViewById(int id) {
        //noinspection unchecked
        return (T) mContentLayout.findViewById(id);
    }

    @Override
    public void onDismiss() {
        dismissed = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (mAnimator != null) {
                mAnimator.removeAllListeners();
                mAnimator.end();
                mAnimator.cancel();
                mAnimator = null;
            }
        }

        if (mRootView != null && mOverlay != null) {
            mRootView.removeView(mOverlay);
        }
        mRootView = null;
        mOverlay = null;

        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        mOnDismissListener = null;

        CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), mLocationLayoutListener);
        CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), mArrowLayoutListener);
        CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), mShowLayoutListener);
        CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), mAnimationLayoutListener);
        CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), mAutoDismissLayoutListener);

        mPopupWindow = null;
    }

    private final View.OnTouchListener mPopupWindowTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getX() > 0 && event.getX() < v.getWidth() &&
                    event.getY() > 0 && event.getY() < v.getHeight()) {
                if (mDismissOnInsideTouch) {
                    dismiss();
                    return mModal;
                }
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return mModal;
        }
    };

    private final View.OnTouchListener mOverlayTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mDismissOnOutsideTouch) {
                dismiss();
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return mModal;
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mLocationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (dismissed) {
                CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
                return;
            }

            if (mMaxWidth > 0 && mContentView.getWidth() > mMaxWidth) {
                CustomTooltipUtil.setWidth(mContentView, mMaxWidth);
                mPopupWindow.update(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                return;
            }

            CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
            mPopupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(mArrowLayoutListener);
            PointF location = calculePopupLocation();
            mPopupWindow.setClippingEnabled(true);
            mPopupWindow.update((int) location.x, (int) location.y, mPopupWindow.getWidth(), mPopupWindow.getHeight());
            mPopupWindow.getContentView().requestLayout();
            createOverlay();
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mArrowLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
            if (dismissed)
                return;

            mPopupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(mAnimationLayoutListener);
            mPopupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(mShowLayoutListener);
            if (mShowArrow) {
                RectF achorRect = CustomTooltipUtil.calculeRectOnScreen(mAnchorView);
                RectF contentViewRect = CustomTooltipUtil.calculeRectOnScreen(mContentLayout);
                float x, y;
                if (mArrowDirection == ArrowDrawable.TOP || mArrowDirection == ArrowDrawable.BOTTOM) {
                    x = mContentLayout.getPaddingLeft() + CustomTooltipUtil.pxFromDp(2);
                    float centerX = (contentViewRect.width() / 2f) - (mArrowView.getWidth() / 2f);
                    float newX = centerX - (contentViewRect.centerX() - achorRect.centerX());
                    if (newX > x) {
                        if (newX + mArrowView.getWidth() + x > contentViewRect.width()) {
                            x = contentViewRect.width() - mArrowView.getWidth() - x;
                        } else {
                            x = newX;
                        }
                    }
                    y = mArrowView.getTop();
                    y = y + (mArrowDirection == ArrowDrawable.BOTTOM ? -1 : +1);
                } else {
                    y = mContentLayout.getPaddingTop() + CustomTooltipUtil.pxFromDp(2);
                    float centerY = (contentViewRect.height() / 2f) - (mArrowView.getHeight() / 2f);
                    float newY = centerY - (contentViewRect.centerY() - achorRect.centerY());
                    if (newY > y) {
                        if (newY + mArrowView.getHeight() + y > contentViewRect.height()) {
                            y = contentViewRect.height() - mArrowView.getHeight() - y;
                        } else {
                            y = newY;
                        }
                    }
                    x = mArrowView.getLeft();
                    x = x + (mArrowDirection == ArrowDrawable.RIGHT ? -1 : +1);
                }
                CustomTooltipUtil.setX(mArrowView, (int) x);
                CustomTooltipUtil.setY(mArrowView, (int) y);
            }
            mPopupWindow.getContentView().requestLayout();
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mShowLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
            if (dismissed)
                return;

            if (mOnShowListener != null)
                mOnShowListener.onShow(CustomTooltip.this);
            mOnShowListener = null;

            mContentLayout.setVisibility(View.VISIBLE);
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mAnimationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
            if (dismissed)
                return;

            if (mAnimated) {
                startAnimation();
            }
            mPopupWindow.getContentView().requestLayout();
        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void startAnimation() {
        final String property = mGravity == Gravity.TOP || mGravity == Gravity.BOTTOM ? "translationY" : "translationX";

        final ObjectAnimator anim1 = ObjectAnimator.ofFloat(mContentLayout, property, -mAnimationPadding, mAnimationPadding);
        anim1.setDuration(mAnimationDuration);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());

        final ObjectAnimator anim2 = ObjectAnimator.ofFloat(mContentLayout, property, mAnimationPadding, -mAnimationPadding);
        anim2.setDuration(mAnimationDuration);
        anim2.setInterpolator(new AccelerateDecelerateInterpolator());

        mAnimator = new AnimatorSet();
        mAnimator.playSequentially(anim1, anim2);
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!dismissed && isShowing()) {
                    animation.start();
                }
            }
        });
        mAnimator.start();
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mAutoDismissLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (dismissed) {
                CustomTooltipUtil.removeOnGlobalLayoutListener(mPopupWindow.getContentView(), this);
                return;
            }

            if (!mRootView.isShown())
                dismiss();
        }
    };

    public interface OnDismissListener {
        void onDismiss(CustomTooltip tooltip);
    }

    public interface OnShowListener {
        void onShow(CustomTooltip tooltip);
    }

    @SuppressWarnings("SameParameterValue")
    public static class Builder {
        private final Context context;
        private boolean dismissOnInsideTouch = true;
        private boolean dismissOnOutsideTouch = true;
        private boolean modal = false;
        private View contentView;
        @IdRes
        private int textViewId = android.R.id.text1;
        private CharSequence text = "";
        private View anchorView;
        private int arrowDirection = ArrowDrawable.AUTO;
        private int gravity = Gravity.BOTTOM;
        private boolean transparentOverlay = true;
        private float maxWidth;
        private boolean showArrow = true;
        private Drawable arrowDrawable;
        private boolean animated = false;
        private float margin = -1;
        private float padding = -1;
        private float animationPadding = -1;
        private OnDismissListener onDismissListener;
        private OnShowListener onShowListener;
        private long animationDuration;
        private int backgroundColor;
        private int textColor;
        private int arrowColor;
        private float arrowHeight;
        private float arrowWidth;

        public Builder(Context context) {
            this.context = context;
        }

        public CustomTooltip build() throws IllegalArgumentException {
            validateArguments();
            if (backgroundColor == 0) {
                backgroundColor = CustomTooltipUtil.getColor(context, defaultBackgroundColorRes);
            }
            if (textColor == 0) {
                textColor = CustomTooltipUtil.getColor(context, defaultTextColorRes);
            }
            if (contentView == null) {
                TextView tv = new TextView(context);
                CustomTooltipUtil.setTextAppearance(tv, defaultTextAppearanceRes);
                tv.setBackgroundColor(backgroundColor);
                tv.setTextColor(textColor);
                contentView = tv;
            }
            if (arrowColor == 0) {
                arrowColor = CustomTooltipUtil.getColor(context, defaultArrowColorRes);
            }
            if (margin < 0) {
                margin = context.getResources().getDimension(defaultMarginRes);
            }
            if (padding < 0) {
                padding = context.getResources().getDimension(defaultPaddingRes);
            }
            if (animationPadding < 0) {
                animationPadding = context.getResources().getDimension(defaultAnimationPaddingRes);
            }
            if (animationDuration == 0) {
                animationDuration = context.getResources().getInteger(defaultAnimationDurationRes);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                animated = false;
            }
            if (showArrow) {
                if (arrowDirection == ArrowDrawable.AUTO)
                    arrowDirection = CustomTooltipUtil.tooltipGravityToArrowDirection(gravity);
                if (arrowDrawable == null)
                    arrowDrawable = new ArrowDrawable(arrowColor, arrowDirection);
                if (arrowWidth == 0)
                    arrowWidth = context.getResources().getDimension(defaultArrowWidthRes);
                if (arrowHeight == 0)
                    arrowHeight = context.getResources().getDimension(defaultArrowHeightRes);
            }
            return new CustomTooltip(this);
        }

        private void validateArguments() throws IllegalArgumentException {
            if (context == null) {
                throw new IllegalArgumentException("Context not specified.");
            }
            if (anchorView == null) {
                throw new IllegalArgumentException("Anchor view not specified.");
            }
        }


        public Builder contentView(TextView textView) {
            this.contentView = textView;
            this.textViewId = 0;
            return this;
        }


        public Builder contentView(View contentView, @IdRes int textViewId) {
            this.contentView = contentView;
            this.textViewId = textViewId;
            return this;
        }


        public Builder contentView(@LayoutRes int contentViewId, @IdRes int textViewId) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.contentView = inflater.inflate(contentViewId, null, false);
            this.textViewId = textViewId;
            return this;
        }

        public Builder contentView(@LayoutRes int contentViewId) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.contentView = inflater.inflate(contentViewId, null, false);
            this.textViewId = 0;
            return this;
        }


        public Builder dismissOnInsideTouch(boolean dismissOnInsideTouch) {
            this.dismissOnInsideTouch = dismissOnInsideTouch;
            return this;
        }


        public Builder dismissOnOutsideTouch(boolean dismissOnOutsideTouch) {
            this.dismissOnOutsideTouch = dismissOnOutsideTouch;
            return this;
        }

        public Builder modal(boolean modal) {
            this.modal = modal;
            return this;
        }


        public Builder text(CharSequence text) {
            this.text = text;
            return this;
        }

        public Builder text(@StringRes int textRes) {
            this.text = context.getString(textRes);
            return this;
        }

        public Builder anchorView(View anchorView) {
            this.anchorView = anchorView;
            return this;
        }

        public Builder gravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder arrowDirection(int arrowDirection) {
            this.arrowDirection = arrowDirection;
            return this;
        }

        public Builder transparentOverlay(boolean transparentOverlay) {
            this.transparentOverlay = transparentOverlay;
            return this;
        }

        public Builder maxWidth(@DimenRes int maxWidthRes) {
            this.maxWidth = context.getResources().getDimension(maxWidthRes);
            return this;
        }

        public Builder maxWidth(float maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Builder animated(boolean animated) {
            this.animated = animated;
            return this;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Builder animationPadding(float animationPadding) {
            this.animationPadding = animationPadding;
            return this;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Builder animationPadding(@DimenRes int animationPaddingRes) {
            this.animationPadding = context.getResources().getDimension(animationPaddingRes);
            return this;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Builder animationDuration(long animationDuration) {
            this.animationDuration = animationDuration;
            return this;
        }

        public Builder padding(float padding) {
            this.padding = padding;
            return this;
        }

        public Builder padding(@DimenRes int paddingRes) {
            this.padding = context.getResources().getDimension(paddingRes);
            return this;
        }

        public Builder margin(float margin) {
            this.margin = margin;
            return this;
        }

        public Builder margin(@DimenRes int marginRes) {
            this.margin = context.getResources().getDimension(marginRes);
            return this;
        }

        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder backgroundColor(@ColorInt int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder showArrow(boolean showArrow) {
            this.showArrow = showArrow;
            return this;
        }

        public Builder arrowDrawable(Drawable arrowDrawable) {
            this.arrowDrawable = arrowDrawable;
            return this;
        }

        public Builder arrowDrawable(@DrawableRes int drawableRes) {
            this.arrowDrawable = CustomTooltipUtil.getDrawable(context, drawableRes);
            return this;
        }

        public Builder arrowColor(@ColorInt int arrowColor) {
            this.arrowColor = arrowColor;
            return this;
        }

        public Builder arrowHeight(float arrowHeight) {
            this.arrowHeight = arrowHeight;
            return this;
        }

        public Builder arrowWidth(float arrowWidth) {
            this.arrowWidth = arrowWidth;
            return this;
        }

        public Builder onDismissListener(OnDismissListener onDismissListener) {
            this.onDismissListener = onDismissListener;
            return this;
        }

        public Builder onShowListener(OnShowListener onShowListener) {
            this.onShowListener = onShowListener;
            return this;
        }
    }
}
