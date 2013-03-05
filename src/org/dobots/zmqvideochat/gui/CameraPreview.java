package org.dobots.zmqvideochat.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class CameraPreview extends ScalableSurfaceView implements SurfaceHolder.Callback, PreviewCallback {
	
	private static final String TAG = "CameraPreview";
	
	public interface CameraPreviewCallback {
		public void onFrame(byte[] rgb, int width, int height);
	}
	
    SurfaceHolder mHolder;  
      
    Camera mCamera;  
      
    //This variable is responsible for getting and setting the camera settings  
    private Parameters mParameters;  
    //this variable stores the camera preview size   
    private Size mPreviewSize;  
    
    private CameraPreviewCallback mFrameListener = null;
    
    public CameraPreview(Context context) {  
        super(context);  
          
        // Install a SurfaceHolder.Callback so we get notified when the  
        // underlying surface is created and destroyed.  
        mHolder = getHolder();  
        mHolder.addCallback(this);  
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
    }  

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Install a SurfaceHolder.Callback so we get notified when the  
        // underlying surface is created and destroyed.  
        mHolder = getHolder();  
        mHolder.addCallback(this);  
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Install a SurfaceHolder.Callback so we get notified when the  
        // underlying surface is created and destroyed. 
        mHolder = getHolder();  
        mHolder.addCallback(this);  
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
    }
    
    public void setFrameListener(CameraPreviewCallback listener) {
    	mFrameListener = listener;
    }
  
    public void surfaceCreated(SurfaceHolder holder) {  
        // The Surface has been created, acquire the camera and tell it where  
        // to draw.  
        mCamera = Camera.open(Camera.getNumberOfCameras() - 1);  
        try {  
        	mCamera.setDisplayOrientation(90);
        	mCamera.setPreviewDisplay(holder);  
             
			//sets the camera callback to be the one defined in this class  
			mCamera.setPreviewCallback(this);  
			
			///initialize the variables  
			mParameters = mCamera.getParameters();  
			mPreviewSize = mParameters.getPreviewSize();  
        } catch (IOException exception) {  
            mCamera.release();  
            mCamera = null;  
            // TODO: add more exception handling logic here  
        }  
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {  
        // Surface will be destroyed when we return, so stop the preview.  
        // Because the CameraDevice object is not a shared resource, it's very  
        // important to release it when the activity is paused.  
		mCamera.setPreviewCallback(null);  
        mCamera.stopPreview();  
        mCamera.release();  
        mCamera = null;  
    }  
  
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {  
        // Now that the surface is created, start the preview. 
    	// Note: not all phones support arbitrary preview sizes, so we leave
    	// the default size for now
        mCamera.startPreview();  
    }  
    @Override  
    public void onPreviewFrame(byte[] data, Camera camera) {  
        //transforms NV21 pixel data into RGB pixels 
        byte[] frame = decodeYUV(data, mPreviewSize.width,  mPreviewSize.height);
        if (mFrameListener != null) {
        	mFrameListener.onFrame(frame, mPreviewSize.width, mPreviewSize.height);
        }
    }
    
    private byte[] decodeYUV(byte[] yuv, int width, int height) {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
    	yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
    	byte[] imageBytes = out.toByteArray();
    	return imageBytes;
    }
      
}  