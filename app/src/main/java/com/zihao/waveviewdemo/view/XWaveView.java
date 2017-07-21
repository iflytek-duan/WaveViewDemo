package com.zihao.waveviewdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * ClassName：XWaveView
 * Description：TODO<自定义水波纹视图>
 * Author：zihao
 * Date：2017/7/19 11:02
 * Email：crazy.zihao@gmail.com
 * Version：v1.0
 */
public class XWaveView extends View {

    /**
     * WaveView的宽高
     */
    private int width, height;
    /**
     * 振幅，振幅值越大，绘制出来的波形在Y轴上表现的最大值与最小值之差就越大。
     * 可以通过与半径之比进行对比取值，建议取值范围 < 0.1
     */
    private static final float A = 0.1f;
    /**
     * x方向偏移量
     */
    private int xOffset;
    /**
     * 当前进度、最大进度(最大进度值默认为100)
     */
    private float progress = 55f, maxProgress = 100f;
    /**
     * 圆心x/y坐标点
     */
    private int centerX, centerY;
    private int circleStrokeWidth;// 外圆线宽
    private int validRadius;// 去除线宽后的内圆有效半径

    // 需要用到的画笔
    private Paint circlePaint;// 绘制圆环的画笔
    private Paint wavePaint;// 绘制水波纹的画笔
    private Paint textPaint;// 绘制文本的画笔

    // 绘制水波纹相关
    private RectF circleRectF;// 内圆所在的矩形
    private Path wavePath;
    private float RADIUS_PER_X;// 每一个像素对应的弧度数

    // 绘制文本内容相关
    private Rect textBounds = new Rect();
    /**
     * 标记View是否已经销毁
     */
    private boolean isDetached = false;

    public XWaveView(Context context) {
        super(context);
        init();
    }

    public XWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public XWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 这里提前进行一些内容的初始化操作
     */
    private void init() {
        initPaint();
        autoRefreshView();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);// 设置抗锯齿效果
        circlePaint.setColor(Color.parseColor("#00FF00"));// 设置画笔颜色
        circlePaint.setStyle(Paint.Style.STROKE);// 设置画笔绘制样式为圆环

        wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setColor(Color.parseColor("#00FF00"));

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.parseColor("#008B00"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (width == 0 || height == 0) {
            // 获取View宽高
            width = getWidth();
            height = getHeight();

            // 计算获取外侧外圆的半径、线宽、内圆有效半径、圆心
            int circleRadius = Math.min(width, height) >> 1;// 取width/height的最小值的1/2作为半径
            circleStrokeWidth = circleRadius / 80;// 默认设置圆环线宽为半径的1/10
            circlePaint.setStrokeWidth(circleStrokeWidth);// 设置圆环线宽

            validRadius = circleRadius - circleStrokeWidth;// 内圆有效半径

            centerX = width / 2;// 圆心x/y点取对应width/height的1/2即可
            centerY = height / 2;

            RADIUS_PER_X = (float) (Math.PI / validRadius);

            circleRectF = new RectF(centerX - validRadius, centerY - validRadius,
                    centerX + validRadius, centerY + validRadius);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 1.绘制外层的圆环
        canvas.drawCircle(centerX, centerY, validRadius + (circleStrokeWidth >> 1), circlePaint);
        // 2.绘制水波纹效果
        canvas.drawPath(getWavePath(xOffset), wavePaint);
        // 3.绘制文本
        textPaint.setTextSize(validRadius >> 1);
        String text = progress / maxProgress + "%";
        // 测量文字长度
        float w1 = textPaint.measureText(text);
        // 测量文字高度
        textPaint.getTextBounds(text, 0, 1, textBounds);
        float h1 = textBounds.height();
        canvas.drawText(text, centerX - w1 / 2, centerY + h1 / 2, textPaint);
    }

    /**
     * 获取水波曲线（包含圆弧部分）的Path.
     *
     * @param xOffset x方向像素偏移量.
     */
    private Path getWavePath(int xOffset) {
        if (wavePath == null) {
            wavePath = new Path();
        } else {
            wavePath.reset();
        }

        float[] startPoint = new float[2];// 波浪线起点
        float[] endPoint = new float[2];// 波浪线终点

        for (int i = 0; i < validRadius * 2; i += 2) {
            float x = centerX - validRadius + i;
            float y = (float) (centerY + validRadius * (1.0f + A) * 2 * (0.5f - progress / maxProgress) // 这里实际上是根据当前进度的百分比绘制一条水平刻度线
                    + validRadius * A * Math.sin((xOffset + i) * RADIUS_PER_X));// 真正产生水波纹的是在这里，利用sin函数来对y轴的高度进行新增或者减小

            // 只计算内圆内部的点，边框上的忽略
            if (calDistance(x, y, centerX, centerY) > validRadius) {// 判断要绘制的点的位置是否已经超出范围
                if (x < centerX) {
                    continue;// 左边框,继续循环
                } else {
                    break;// 右边框,结束循环
                }
            }

            // 第1个点
            if (wavePath.isEmpty()) {
                startPoint[0] = x;
                startPoint[1] = y;
                wavePath.moveTo(x, y);
            } else {
                wavePath.lineTo(x, y);
            }

            endPoint[0] = x;
            endPoint[1] = y;
        }

        if (!wavePath.isEmpty()) {
            // 通过画圆弧的方式，来填充下方区域的空白
            float startDegree = calDegreeByPosition(startPoint[0], startPoint[1]);  //0~180
            float endDegree = calDegreeByPosition(endPoint[0], endPoint[1]); //180~360
            wavePath.arcTo(circleRectF, endDegree - 360, startDegree - (endDegree - 360));
            // 这段代码实现的是矩形水波纹效果
//            wavePath.lineTo(endPoint[0], centerY + validRadius);
//            wavePath.lineTo(startPoint[0], centerY + validRadius);
//            wavePath.close();
        }
        return wavePath;
    }

    /**
     * 测算传入的坐标点(x1,y1)与(x2，y2)之间的直线距离
     *
     * @param x1 坐标点x1
     * @param y1 坐标点y1
     * @param x2 坐标点x2
     * @param y2 坐标点y2
     * @return 两点之间的直线距离
     */
    private float calDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * 根据当前位置，计算出进度条已经转过的角度
     *
     * @param currentX 当前X
     * @param currentY 当前Y
     * @return 已经旋转过的角度值
     */
    private float calDegreeByPosition(float currentX, float currentY) {
        float a1 = (float) (Math.atan(1.0f * (centerX - currentX) / (currentY - centerY)) / Math.PI * 180);
        if (currentY < centerY) {
            a1 += 180;
        } else if (currentY > centerY && currentX > centerX) {
            a1 += 360;
        }

        return a1 + 90;
    }

    /**
     * 自动刷新视图
     */
    private void autoRefreshView() {
        autoRefreshThread.start();
    }

    private Thread autoRefreshThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!isDetached) {
                xOffset += (validRadius >> 4);
                SystemClock.sleep(100);
                postInvalidate();
            }
        }
    });

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isDetached = true;
    }

    public void setProgress(float progress) {
        this.progress = progress > maxProgress ? progress - maxProgress : progress;
        invalidate();
    }

    public void setMaxProgress(float maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }
}
