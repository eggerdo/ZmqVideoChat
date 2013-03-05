package org.dobots.utils;

import org.dobots.zmqvideochat.ZmqVideoChatActivity;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class Utils {
	
	// show a toast message
    public static void showToast(String textToShow, int duration) {
    	Toast oToast = Toast.makeText(ZmqVideoChatActivity.getContext(), textToShow, duration);
		oToast.show();
	}
    
    // run an asynchronous task, if the task has to modifie UI elements
    // use runAsyncUiTask instead
	public static void runAsyncTask(Runnable runner) {
		new Thread(runner).start();
	}
	
	// run an asynchronous UI task
	public static void runAsyncUiTask(Runnable runner) {
		Handler oHandler = new Handler(Looper.getMainLooper());
		oHandler.post(runner);
	}

    public static void waitSomeTime(int millis) {
        try {
            Thread.sleep(millis);

        } catch (InterruptedException e) {
        	e.printStackTrace();
        }
    }

	public static boolean isInteger(String str) {
		try {
			Integer.valueOf(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public static boolean isNumeric(String str) {
		try {
			Double.valueOf(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

}
