package org.dobots.zmqvideochat.chat;

import org.dobots.zmqvideochat.ChatSettings;
import org.dobots.zmqvideochat.ChatTypes;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import android.os.Handler;
import android.os.Message;

public class ChatHandler {

	private ZContext m_oZContext;
	private ChatSettings m_oSettings;
	
	// the channel on which messages are sent out
	private Socket m_oMsgSender = null;
	// the channel on which messages are coming in
	private Socket m_oMsgReceiver = null;
	
	// thread handling the message reception
	private ChatReceiveThread m_oRecvThread;
	
	// defines the current chat room
	private String m_strChatRoom = "#";
	
	// handler to display received messages
	private Handler m_oUiHandler;
	
	private boolean m_bConnected;
	
	public ChatHandler(ZContext i_oContext, ChatSettings i_oSettings, Handler i_oUiHandler) {
		m_oZContext = i_oContext;
		m_oSettings = i_oSettings;
		m_oUiHandler = i_oUiHandler;
	}
	
	public void setChatRoom(String i_strChatRoom) {
		m_strChatRoom = i_strChatRoom;
	}
	
	public void setupConnections() {

		m_oMsgSender = m_oZContext.createSocket(ZMQ.PUB);
		m_oMsgReceiver = m_oZContext.createSocket(ZMQ.SUB);

		// obtain chat ports from settings
		// receive port is always equal to send port + 1
		int nChatSendPort = m_oSettings.getChatPort();
		int nChatRecvPort = nChatSendPort + 1;
		
		m_oMsgSender.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nChatSendPort));
		m_oMsgReceiver.connect(String.format("tcp://%s:%d", m_oSettings.getAddress(), nChatRecvPort));

		// subscribe to the current chat room
		m_oMsgReceiver.subscribe(m_strChatRoom.getBytes());
		// subscribe to messages which are targeted at us directly
		m_oMsgReceiver.subscribe(m_oSettings.getNickName().getBytes());

		m_oRecvThread = new ChatReceiveThread();
		m_oRecvThread.start();
		
		m_bConnected = true;
		
	}

	public void closeConnections() {

		if (m_oRecvThread != null) {
			m_oRecvThread.bRun = false;
			m_oRecvThread.interrupt();
			m_oRecvThread = null;
		}
		
		if (m_oMsgSender != null) {
			m_oMsgSender.close();
			m_oMsgSender = null;
		}
		
		if (m_oMsgReceiver != null) {
			m_oMsgReceiver.close();
			m_oMsgReceiver = null;
		}

		m_bConnected = false;
	}

	public void sendMessage(String i_strMessage) {
		if (m_bConnected) {
			// create a chat message out of the message string
			ChatMessage oChatMessage = new ChatMessage(m_oSettings.getNickName(), m_strChatRoom, i_strMessage);
			// create a zmq message out of the chat message
			ZMsg oZmqMessage = oChatMessage.toZmsg();
			// send the zmq message out
			oZmqMessage.send(m_oMsgSender);
		}
	}
	
	class ChatReceiveThread extends Thread {

		public boolean bRun = true;
		
		@Override
		public void run() {
			while(bRun) {
				try {
					ZMsg oZMsg = ZMsg.recvMsg(m_oMsgReceiver);
					if (oZMsg != null) {
						// create a chat message out of the zmq message
						ChatMessage oChatMsg = ChatMessage.fromZMsg(oZMsg);
						
						if (!(oChatMsg.target.equals(m_strChatRoom) || 
							  oChatMsg.target.equals(m_oSettings.getNickName()))) {
							// double check if the message is for us
							continue;
						}
						if (oChatMsg.nickname.equals(m_oSettings.getNickName())) {
							// adjust the name if it was sent by us
							oChatMsg.nickname = "Me";
						}
						
						final String message = String.format("%s: %s", oChatMsg.nickname, oChatMsg.message);
	
						// display the message in the chat view
						Message uiMsg = m_oUiHandler.obtainMessage();
						uiMsg.what = ChatTypes.INCOMING_CHAT_MSG;
						uiMsg.obj = message;
						m_oUiHandler.dispatchMessage(uiMsg);
	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}
