package com.example.spreadviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.Objects;

public class ShadedImageView extends android.support.v7.widget.AppCompatImageView {
    Bitmap bm = null;
    private Context context;
    private float scale = 1;
    private int screenHeight;
    private ViewTreeObserver viewTreeObserver;
    private float oriYAbs, oriXAbs , oriRAbs;

    public ShadedImageView(Context context) {
        super(context);
        this.context = context;
        this.setScaleType(ScaleType.CENTER_CROP);
    }

    public ShadedImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.setScaleType(ScaleType.CENTER_CROP);
    }

    public ShadedImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            bm = drableToBitMap(drawable);
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Objects.requireNonNull(windowManager).getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        viewTreeObserver = getViewTreeObserver();
        viewTreeObserver.addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                drawLocation();
            }
        });
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawLocation();
            }
        });
    }

    private Bitmap drableToBitMap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap outPut = Bitmap.createBitmap(width, height, drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(outPut);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return outPut;
    }

    private float getScale(Bitmap bitmap, float vm, float vh) {
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        float scale;
        scale = vm / width > vh / height ? vm / width : vh / height;
        return scale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (bm != null)
            scale = getScale(bm, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageResource(int resId) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        setImageDrawable(drawable);
    }

    private void drawLocation() {
        if (bm != null) {
            int[] location = new int[2];
            getLocationOnScreen(location);
            float locationY = location[1];
            oriXAbs = (bm.getWidth()*scale-getMeasuredWidth())/2+getMeasuredHeight()/4;//由于绘制的时候是根据画布来进行坐标计算，
            // 所以需要计算圆的原点。
            oriYAbs = (bm.getHeight()*scale-getMeasuredHeight())/2+getMeasuredHeight()/4;
            //oriX,oriY实际在画布里的原点
            int oriX = (int) (oriXAbs/scale);
            int oriY = (int) (oriYAbs/scale);
            float centerY = screenHeight/2;
            float startY = screenHeight/2-getMeasuredHeight();
            //计算当控件滑到中心以下时，圆全部覆盖后的半径。
//            float endX = getMeasuredHeight()*3/4;
//            float endY = getMeasuredWidth() - getMeasuredHeight()/4;
            float endX = getMeasuredWidth();
            float endY = getMeasuredHeight();
            float endR = (float) Math.sqrt(endX*endX+endY*endY)/scale;
            if(locationY<centerY && locationY>startY) {
                //开始绘画位置
                int r = (int) ((locationY-startY)*endR/(centerY-startY));
                oriRAbs = r*scale;//记录控件你显示的半径范围，用于实际点击
                Bitmap shadeBm = getShadeBitmap(bm, r, oriX, oriY);
                setImageBitmap(shadeBm);
            }
            else if(locationY<=startY)
            {
                Bitmap shadeBm = getShadeBitmap(bm, 0, oriX, oriY);
                oriRAbs = 0;//记录控件你显示的半径范围，用于实际点击
                setImageBitmap(shadeBm);
            }
            else
            {
                Bitmap shadeBm = getShadeBitmap(bm, (int) endR, oriX, oriY);
                oriRAbs = endR*scale;//记录控件你显示的半径范围，用于实际点击
                setImageBitmap(shadeBm);
            }
        }
    }
    private Bitmap getShadeBitmap(Bitmap bitmap, int r , int oriX, int oriY)
    {
        Paint paint = new Paint(); //创建笔
        paint.setAntiAlias(true);//给Paint加上抗锯齿标志

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output); //创建一个画布,以画布的坐标为标准
        Rect rect = new Rect(oriX-r, oriY-r, r+oriX, r+oriY); //构造一个矩形，前面两个参数代表的是左上点
        RectF rectF = new RectF(rect);
        canvas.drawRoundRect(rectF, r, r, paint);
        canvas.save();

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));//取两层绘制交集。显示上层。遮罩，先画的在上面，后画的在下面
        //要先画圆再设置

        Rect rect2 = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()); //构造一个矩形
        canvas.drawBitmap(bitmap, rect2, rect2, paint);//第一个Rect 代表要绘制的bitmap 区域，第二个 Rect 代表的是要将bitmap 绘制在画布的什么地方，绘制的时候不会改变图片本身
        return output;
    }
}
