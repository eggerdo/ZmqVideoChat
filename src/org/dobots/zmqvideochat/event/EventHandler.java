package org.dobots.zmqvideochat.event;

import org.dobots.zmqvideochat.ChatSettings;
import org.dobots.zmqvideochat.ChatTypes;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class EventHandler {
	
	private ZContext m_oZContext;
	private ChatSettings m_oSettings;

	// the channel used for sending
	private Socket m_oEventSender;
	// the channel used for receiving events
	private Socket m_oEventReceiver;
	
	// the handler to update the UI
	private Handler m_oUiHandler;
	
	// the thread to handle the reception of events
	private EventReceiveThread m_oReceiveThread;
	
	private boolean m_bConnected;
	
	public EventHandler(ZContext i_oContext, ChatSettings i_oSettings, Handler i_oUiHandler) {
		m_oZContext = i_oContext;
		m_oSettings = i_oSettings;
		m_oUiHandler = i_oUiHandler;
	}
	
	public void setupConnections() {
		
		m_oEventSender = m_oZContext.createSocket(ZMQ.PUB);
		m_oEventReceiver = m_oZContext.createSocket(ZMQ.SUB);

		// obtain client ports from settings
		// receive port is always equal to send port + 1
		int nEventSendPort = m_oSettings.getEventPort();
		int nEventRecvPort = nEventSendPort + 1;
		
		m_oEventSender.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nEventSendPort));
		m_oEventReceiver.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nEventRecvPort));
		
		m_oEventReceiver.subscribe("".getBytes());
		
		m_oReceiveThread = new EventReceiveThread();
		m_oReceiveThread.start();

		m_bConnected = true;
		
		sendRegisterEvent();
		
	}
	
	public void closeConnections() {
		
		sendUnregisterEvent();

		if (m_oReceiveThread != null) {
			m_oReceiveThread.bRun = false;
			m_oReceiveThread.interrupt();
			m_oReceiveThread = null;
		}
		
		if (m_oEventSender != null) {
			m_oEventSender.close();
			m_oEventSender = null;
		}
		
		if (m_oEventReceiver != null) {
			m_oEventReceiver.close();
			m_oEventReceiver = null;
		}
		
		m_bConnected = false;
	}
	
	private void sendRegisterEvent() {
		sendEvent("register", m_oSettings.getNickName());
	}
	
	private void sendUnregisterEvent() {
		sendEvent("unregister", m_oSettings.getNickName());
	}

	public void refresh() {
		sendEvent("list", m_oSettings.getNickName());
	}

	private void sendEvent(String i_strEvent, String i_strEventData) {
		
		if (m_bConnected) {
			EventMessage oEventMsg = new EventMessage(i_strEvent, i_strEventData);
			ZMsg oMsg = oEventMsg.toZmsg();
			
			oMsg.send(m_oEventSender);
		}
	}
	

	class EventReceiveThread extends Thread {

		boolean bRun = true;
		
		@Override
		public void run() {
			while(bRun) {
				ZMsg msg = ZMsg.recvMsg(m_oEventReceiver);
				if (msg != null) {
					EventMessage oEventMsg = EventMessage.fromZmsg(msg);
					
					Message uiMsg = m_oUiHandler.obtainMessage();
					uiMsg.what = ChatTypes.UPDATE_CLIENTS;
					uiMsg.obj = oEventMsg.data;
					m_oUiHandler.dispatchMessage(uiMsg);
				}
			}
		}
		
	}

}
