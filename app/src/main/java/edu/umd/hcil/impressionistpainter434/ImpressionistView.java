package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    public Canvas _offScreenCanvas = null;
    public Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public Bitmap _bitmap;

    private float _xPrev = 0;
    private float _yPrev = 0;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(7);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
        this._offScreenBitmap = Bitmap.createBitmap(
                rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        this._offScreenCanvas = new Canvas(this._offScreenBitmap);
        _paint.setColor(Color.WHITE);
        _offScreenCanvas.drawRect(0, 0,rect.width(), rect.height(),_paint);
        invalidate();
    }

    public void invert(){
        Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
        int col;
        for(int x = 0; x < rect.width(); x++){
            for(int y = 0; y < rect.height(); y++){
                col = _offScreenBitmap.getPixel(x, y);

                int red = 0xFF - ((col>>16)&0xFF);
                int green = 0xFF - ((col>>8)&0xFF);
                int blue = 0xFF - (col&0xFF);
                _offScreenBitmap.setPixel(x,y, Color.argb(255, red, green, blue));
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        if(_offScreenBitmap != null) {
//            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
//        }
        try {
            Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
            canvas.drawBitmap(_offScreenBitmap, rect.left, rect.top, _paint);
        } catch (Exception e){

        }



        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to list for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        //******ADD CHECKS FOR TOP, LEFT, BOTTOM, RIGHT********

        float x = motionEvent.getX();
        float y = motionEvent.getY();

        int type = motionEvent.getAction();
        Rect rect = this.getBitmapPositionInsideImageView(this._imageView);

        if (type == MotionEvent.ACTION_MOVE){
            int bitMapX = (int) (x-rect.left);
            int bitMapY = (int) (y-rect.top);
            int color = this._bitmap.getPixel(bitMapX, bitMapY);
            this._paint.setColor(color);
            switch (_brushType){
                case Circle:
                    this._offScreenCanvas.drawCircle(bitMapX, bitMapY, 10, _paint);
                    invalidate();
                    break;
                case Square:
                    this._offScreenCanvas.drawRect(
                            bitMapX - 10, bitMapY - 10, bitMapX + 10, bitMapY + 10, _paint);
                    invalidate();
                    break;
                case Line:
                    if(!(_xPrev == 0 || _yPrev == 0)) {
                        float length = Math.abs(bitMapX - _xPrev) + Math.abs(bitMapY - _yPrev);

                        this._offScreenCanvas.drawLine(
                                bitMapX - length / 2, bitMapY - length / 2, bitMapX + length / 2, bitMapY + length / 2, _paint);
                    }
                    _xPrev = bitMapX;
                    _yPrev = bitMapY;
                    invalidate();
                break;
            }
        }else if (type == MotionEvent.ACTION_UP){
            _xPrev = 0;
            _yPrev = 0;
        }

        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public void Bitmapper(Bitmap bitmap){
        Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
        this._bitmap = Bitmap.createScaledBitmap(
                bitmap, rect.width(), rect.height(), false);
        this._offScreenBitmap = Bitmap.createBitmap(
                rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        this._offScreenCanvas = new Canvas(this._offScreenBitmap);
        _paint.setColor(Color.WHITE);
        _offScreenCanvas.drawRect(0, 0,rect.width(), rect.height(),_paint);
    }

}

