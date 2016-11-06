package io.tammen.pulchra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.View;

/**
 * Created by Tammen Bruccoleri on 11/6/16.
 */

public class OverlayView extends View {

    private static final int mDefaultOverlayCircleOffsetRes = R.dimen.customtooltip_overlay_circle_offset;
    private static final int mDefaultOverlayAlphaRes = R.integer.customtooltip_overlay_alpha;

    private View mAnchorView;
    private Bitmap bitmap;
    private float offset = 0;
    private boolean invalidated = true;

    OverlayView(Context context, View anchorView) {
        super(context);
        this.mAnchorView = anchorView;
        this.offset = context.getResources().getDimension(mDefaultOverlayCircleOffsetRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (invalidated)
            createWindowFrame();

        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    private void createWindowFrame() {
        final int width = getMeasuredWidth(), height = getMeasuredHeight();
        if (width <= 0 || height <= 0)
            return;

        if (bitmap != null && !bitmap.isRecycled())
            bitmap.recycle();
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas osCanvas = new Canvas(bitmap);

        RectF outerRectangle = new RectF(0, 0, width, height);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setAlpha(getResources().getInteger(mDefaultOverlayAlphaRes));
        osCanvas.drawRect(outerRectangle, paint);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        RectF anchorRecr = CustomTooltipUtil.calculeRectInWindow(mAnchorView);
        RectF overlayRecr = CustomTooltipUtil.calculeRectInWindow(this);

        float left = anchorRecr.left - overlayRecr.left;
        float top = anchorRecr.top - overlayRecr.top;
        RectF oval = new RectF(left - offset, top - offset, left + mAnchorView.getMeasuredWidth() + offset, top + mAnchorView.getMeasuredHeight() + offset);

        osCanvas.drawOval(oval, paint);

        invalidated = false;
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        invalidated = true;
    }

    public View getAnchorView() {
        return mAnchorView;
    }

    public void setAnchorView(View anchorView) {
        this.mAnchorView = anchorView;
        invalidate();
    }
}