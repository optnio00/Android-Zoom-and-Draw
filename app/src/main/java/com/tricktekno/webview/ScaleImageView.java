package com.tricktekno.webview;

/**
 * Created by anura on 1/31/2017.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class ScaleImageView extends ImageView implements OnTouchListener {

    static final float STROKE_WIDTH = 10f;
    static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

    float lastTouchX;
    float lastTouchY;
    final RectF dirtyRect = new RectF();

    private Context mContext;
    private float MAX_SCALE = 2f;

    private static Matrix mMatrix;
    private final float[] mMatrixValues = new float[9];

    // display width height.
    private int mWidth;
    private int mHeight;

    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    private float mScale;
    private float mMinScale;

    private double mPrevDistance;
    private boolean isScaling;

    private int mPrevMoveX;
    private int mPrevMoveY;
    private GestureDetector mDetector;

    Paint paint = new Paint();
    public static Path path = new Path();

    public static int imageheight, imagewidth;

    String TAG = "ScaleImageView";

    public ScaleImageView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
        initialize();
    }

    public ScaleImageView(Context context) {
        super(context);
        this.mContext = context;
        initialize();
    }

    private void resetDirtyRect(float eventX, float eventY) {
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        this.initialize();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        this.initialize();
    }

    private void initialize() {
        this.setScaleType(ScaleType.MATRIX);
        this.mMatrix = new Matrix();
        Drawable d = getDrawable();

        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);

        if (d != null) {
            mIntrinsicWidth = d.getIntrinsicWidth();
            mIntrinsicHeight = d.getIntrinsicHeight();
            setOnTouchListener(this);
        }
        mDetector = new GestureDetector(mContext,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        maxZoomTo((int) e.getX(), (int) e.getY());
                        cutting();
                        return super.onDoubleTap(e);
                    }
                });

    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        mWidth = r - l;
        mHeight = b - t;

        mMatrix.reset();
        int r_norm = r - l;
        mScale = (float) r_norm / (float) mIntrinsicWidth;

        int paddingHeight = 0;
        int paddingWidth = 0;
        // scaling vertical
        if (mScale * mIntrinsicHeight > mHeight) {
            mScale = (float) mHeight / (float) mIntrinsicHeight;
            mMatrix.postScale(mScale, mScale);
            paddingWidth = (r - mWidth) / 2;
            paddingHeight = 0;
            // scaling horizontal
        } else {
            mMatrix.postScale(mScale, mScale);
            paddingHeight = (b - mHeight) / 2;
            paddingWidth = 0;
        }
        mMatrix.postTranslate(paddingWidth, paddingHeight);

        setImageMatrix(mMatrix);
        mMinScale = mScale;
        zoomTo(mScale, mWidth / 2, mHeight / 2);
        cutting();
        return super.setFrame(l, t, r, b);
    }

    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    protected float getScale() {
        return getValue(mMatrix, Matrix.MSCALE_X);
    }

    public float getTranslateX() {
        return getValue(mMatrix, Matrix.MTRANS_X);
    }

    protected float getTranslateY() {
        return getValue(mMatrix, Matrix.MTRANS_Y);
    }

    protected void maxZoomTo(int x, int y) {
        if (mMinScale != getScale() && (getScale() - mMinScale) > 0.1f) {
            // threshold 0.1f
            float scale = mMinScale / getScale();
            zoomTo(scale, x, y);
        } else {
            float scale = MAX_SCALE / getScale();
            zoomTo(scale, x, y);
        }
    }

    public void zoomTo(float scale, int x, int y) {
        if (getScale() * scale < mMinScale) {
            return;
        }
        if (scale >= 1 && getScale() * scale > MAX_SCALE) {
            return;
        }
        mMatrix.postScale(scale, scale);
        // move to center
        mMatrix.postTranslate(-(mWidth * scale - mWidth) / 2,
                -(mHeight * scale - mHeight) / 2);

        // move x and y distance
        mMatrix.postTranslate(-(x - (mWidth / 2)) * scale, 0);
        mMatrix.postTranslate(0, -(y - (mHeight / 2)) * scale);
        setImageMatrix(mMatrix);
    }

    public void cutting() {
        int width = (int) (mIntrinsicWidth * getScale());
        int height = (int) (mIntrinsicHeight * getScale());

        imagewidth = width;
        imageheight = height;

        if (getTranslateX() < -(width - mWidth)) {
            mMatrix.postTranslate(-(getTranslateX() + width - mWidth), 0);
        }
        if (getTranslateX() > 0) {
            mMatrix.postTranslate(-getTranslateX(), 0);
        }
        if (getTranslateY() < -(height - mHeight)) {
            mMatrix.postTranslate(0, -(getTranslateY() + height - mHeight));
        }
        if (getTranslateY() > 0) {
            mMatrix.postTranslate(0, -getTranslateY());
        }
        if (width < mWidth) {
            mMatrix.postTranslate((mWidth - width) / 2, 0);
        }
        if (height < mHeight) {
            mMatrix.postTranslate(0, (mHeight - height) / 2);
        }
        setImageMatrix(mMatrix);
    }

    private double distance(float x0, float x1, float y0, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return Math.sqrt(x * x + y * y);
    }

    private double dispDistance() {
        return Math.sqrt(mWidth * mWidth + mHeight * mHeight);
    }

    public void clear() {
        path.reset();
        invalidate();
    }

    public static void save() {

        Bitmap returnedBitmap = Bitmap.createBitmap(
                ScaleImageViewActivity.imageview.getWidth(),
                ScaleImageViewActivity.imageview.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);

        Drawable bgDrawable = ScaleImageViewActivity.imageview.getDrawable();

        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);

        ScaleImageViewActivity.imageview.draw(canvas);

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        returnedBitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);

        Bitmap FinalBitmap = BitmapFactory.decodeByteArray(bs.toByteArray(), 0,
                bs.toByteArray().length);

        ScaleImageViewActivity.imageview.setImageBitmap(FinalBitmap);
        path.reset();

        // ScaleImageViewActivity.imageview.setImageMatrix(mMatrix);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!ScaleImageViewActivity.flag) {

            if (mDetector.onTouchEvent(event)) {
                return true;
            }
            int touchCount = event.getPointerCount();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_1_DOWN:
                case MotionEvent.ACTION_POINTER_2_DOWN:
                    if (touchCount >= 2) {
                        double distance = distance(event.getX(0), event.getX(1),
                                event.getY(0), event.getY(1));
                        mPrevDistance = distance;
                        isScaling = true;
                    } else {
                        mPrevMoveX = (int) event.getX();
                        mPrevMoveY = (int) event.getY();
                    }
                case MotionEvent.ACTION_MOVE:
                    if (touchCount >= 2 && isScaling) {
                        double dist = distance(event.getX(0), event.getX(1),
                                event.getY(0), event.getY(1));
                        float scale = (float) ((dist - mPrevDistance) / dispDistance());
                        mPrevDistance = dist;
                        scale += 1;
                        scale = scale * scale;
                        zoomTo(scale, mWidth / 2, mHeight / 2);
                        cutting();
                    } else if (!isScaling) {
                        int distanceX = mPrevMoveX - (int) event.getX();
                        int distanceY = mPrevMoveY - (int) event.getY();
                        mPrevMoveX = (int) event.getX();
                        mPrevMoveY = (int) event.getY();
                        mMatrix.postTranslate(-distanceX, -distanceY);
                        cutting();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_POINTER_2_UP:
                    if (event.getPointerCount() <= 1) {
                        isScaling = false;
                    }
                    break;
            }
        } else {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        if (ScaleImageViewActivity.flag)
            canvas.drawPath(path, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return super.onTouchEvent(event);
    }

}
