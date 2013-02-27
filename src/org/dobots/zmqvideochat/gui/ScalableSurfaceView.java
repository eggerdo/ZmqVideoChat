package org.dobots.zmqvideochat.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class ScalableSurfaceView extends SurfaceView {
	public boolean isScaled = true;
	private int m_nMaxWidth = 0;

    public ScalableSurfaceView(Context context) {
        super(context);
    	requestLayout();
    }

    public ScalableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    	requestLayout();
    }

    public ScalableSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    	requestLayout();
    }
    
    public void setScale(boolean i_bScale) {
    	isScaled = i_bScale;
    	requestLayout();
    }
    
    public void setMaxWidth(int i_nMaxWidth) {
    	m_nMaxWidth = i_nMaxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	
    	if (isScaled) {
	        try
	        {
//	            Drawable drawable = getd;
//	
//	            if (drawable == null)
//	            {
//	                setMeasuredDimension(0, 0);
//	            }
//	            else
//	            {
	            	int width;
            		width = MeasureSpec.getSize(widthMeasureSpec);
	            	if (m_nMaxWidth != 0) {
	            		width = Math.min(width, m_nMaxWidth);
	            	}
//	                int height = width * drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
	            	int height = width;
	                setMeasuredDimension(width, height);
//	            }
	        }
	        catch (Exception e)
	        {
	            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        }
    	} else {
    		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    	}
    }
}
