package org.dobots.zmqvideochat.video;

import java.io.ByteArrayInputStream;

import org.dobots.zmqvideochat.ChatSettings;
import org.dobots.zmqvideochat.ChatTypes;
import org.dobots.zmqvideochat.gui.CameraPreview.CameraPreviewCallback;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

public class VideoHandler implements CameraPreviewCallback {

	private ZContext m_oZContext;
	private ChatSettings m_oSettings;

	// the channel used to send our own video
	private Socket m_oVideoSender = null;
	// the channel used to receive the partner's video
	private Socket m_oVideoReceiver = null;
	
	// thread handling the video reception
	private VideoReceiveThread m_oVideoRecvThread;
	
	// handler to display received frames
	private Handler m_oUiHandler;

	private boolean m_bVideoEnabled = true;

	// debug frame counters
    private int m_nFpsCounterUser = 0;
    private long m_lLastTimeUser = System.currentTimeMillis();

    private int m_nFpsCounterPartner = 0;
    private long m_lLastTimePartner = System.currentTimeMillis();

    private boolean m_bDebug;
    
    // on startup don't display the video of anybody, but wait until a partner is selected
    private String m_strPartner = "nobody";
    
	private boolean m_bConnected;
    
	public VideoHandler(ZContext i_oContext, ChatSettings i_oSettings, Handler i_oUiHandler) {
		m_oZContext = i_oContext;
		m_oSettings = i_oSettings;
		m_oUiHandler = i_oUiHandler;
	}

	public String getPartner() {
		return m_strPartner;
	}

	public void setPartner(String i_strPartner) {
		// unsubscribe from previous partner
		m_oVideoReceiver.unsubscribe(m_strPartner.getBytes());
		
		m_strPartner = i_strPartner;
		// subscribe to new partner
		m_oVideoReceiver.subscribe(m_strPartner.getBytes());
	}
	
	public void setDebug(boolean i_bDebug) {
		m_bDebug = i_bDebug;
	}
	
	public void setVideoEnabled(boolean i_bEnabled) {
		m_bVideoEnabled = i_bEnabled;
	}

	@Override
	public void onFrame(byte[] rgb, int width, int height) {
		// send the frame out to the server
		sendVideoMessage(rgb, width, height);
		
		if (m_bDebug) {
	        ++m_nFpsCounterUser;
	        long now = System.currentTimeMillis();
	        if ((now - m_lLastTimeUser) >= 1000)
	        {
	        	Message uiMsg = m_oUiHandler.obtainMessage();
	        	uiMsg.what = ChatTypes.SET_USER_FPS;
	        	uiMsg.obj = m_nFpsCounterUser;
				m_oUiHandler.dispatchMessage(uiMsg);
	            
	            m_lLastTimeUser = now;
	            m_nFpsCounterUser = 0;
	        }
		}
	}

	private void sendVideoMessage(byte[] rgb, int width, int height) {
		
		if (m_bVideoEnabled && m_bConnected) {
			// create a video message from the rgb data
			VideoMessage videoMsg = new VideoMessage(m_oSettings.getNickName(), rgb, width, height);
			// make a zmq message
			ZMsg outMsg = videoMsg.toZmsg();
			// send the zmq message out
			outMsg.send(m_oVideoSender);
		}
	}

	class VideoReceiveThread extends Thread {

		public boolean bRun = true;
		
		@Override
		public void run() {
			while(bRun) {
				ZMsg msg = ZMsg.recvMsg(m_oVideoReceiver);
				if (msg != null) {
					// create a video message out of the zmq message
					VideoMessage oVideoMsg = VideoMessage.fromZMsg(msg);
					
					if (!oVideoMsg.nickname.equals(m_strPartner)) {
						// double check that we are really interested in the message
						continue;
					}

					// decode the received frame from jpeg to a bitmap
					ByteArrayInputStream stream = new ByteArrayInputStream(oVideoMsg.videoData);
					Bitmap bmp = BitmapFactory.decodeStream(stream);
					
					Message uiMsg = m_oUiHandler.obtainMessage();
					uiMsg.what = ChatTypes.INCOMING_VIDEO_MSG;
					uiMsg.obj = bmp;
					m_oUiHandler.dispatchMessage(uiMsg);
				
                    if (m_bDebug) {
	                    ++m_nFpsCounterPartner;
	                    long now = System.currentTimeMillis();
	                    if ((now - m_lLastTimePartner) >= 1000)
	                    {
	        	        	uiMsg = m_oUiHandler.obtainMessage();
	        	        	uiMsg.what = ChatTypes.SET_PARTNER_FPS;
	        	        	uiMsg.obj = m_nFpsCounterPartner;
	    					m_oUiHandler.dispatchMessage(uiMsg);
	        	            
	                        m_lLastTimePartner = now;
	                        m_nFpsCounterPartner = 0;
	                    }
                    }
				}
			}
		}
		
	}

	public void setupConnections() {

		if (m_bVideoEnabled) {
			m_oVideoSender = m_oZContext.createSocket(ZMQ.PUB);
			m_oVideoReceiver = m_oZContext.createSocket(ZMQ.SUB);

			// obtain video ports from settings
			// receive port is always equal to send port + 1
			int nVideoSendPort = m_oSettings.getVideoPort();
			int nVideoRecvPort = nVideoSendPort + 1;
			
			// set the output queue size down, we don't really want to have old video frames displayed
			// we only want the most recent ones
			m_oVideoSender.setHWM(20);

			m_oVideoSender.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nVideoSendPort));
			m_oVideoReceiver.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nVideoRecvPort));
			
			// subscribe to the partner's video
			m_oVideoReceiver.subscribe(m_strPartner.getBytes());
	
			m_oVideoRecvThread = new VideoReceiveThread();
			m_oVideoRecvThread.start();
			
			m_bConnected = true;
		}
		
	}


	public void closeConnections() {
		
		if (m_oVideoRecvThread != null) {
			m_oVideoRecvThread.bRun = false;
			m_oVideoRecvThread.interrupt();
			m_oVideoRecvThread = null;
		}
		
		if (m_oVideoSender != null) {
			m_oVideoSender.close();
			m_oVideoSender = null;
		}
		
		if (m_oVideoReceiver != null) {
			m_oVideoReceiver.close();
			m_oVideoReceiver = null;
		}
		
		m_bConnected = false;
	}
	
}
