package org.dobots.zmqvideochat.chat;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

public class ChatMessage {
	
	// A ZMQ chat message consists of two ZFrames:
	//
	// -----------------
	// | target | data |
	// -----------------
	//
	// where target		is the name of the chat room to which the message should be sent and which is used
	//					for channel subscription / filtering
	// 		 data	 	is a JSON object which is of the form:
	//
	//		 {
	//		 		"target": "";
	//				"nick": "";
	//				"message": "";
	//		 }
	//		 
	//		 where target		is again the name of the chat room
	//				 nick 		is the nickname of the sender
	//				 message	is the message to be sent
	
	// the message target
	public String target = "";
	// the nickname of the sender
	public String nickname = "";
	// the message
	public String message = "";
	
	private ChatMessage() {
		// nothing to do
	}
	
	public ChatMessage(String i_strUserName, String i_strChannel, String i_strMessage) {
		
//		if (i_strMessage.contains("#")) {
//			int terminator = i_strMessage.indexOf("#");
//			target = "#" + i_strMessage.substring(0, terminator);
//			message = i_strMessage.substring(terminator + 1, i_strMessage.length());
//		} else {
			target = i_strChannel;
			message = i_strMessage;
//		}
		nickname = i_strUserName;
	}
	
	public static ChatMessage fromZMsg(ZMsg i_oMsg) {
		ChatMessage oMsg = new ChatMessage();
		ZFrame oChannel = i_oMsg.pop();
		ZFrame oData = i_oMsg.pop();
		
		// get the target, nickname and message from the json string contained in the
		// ZFrame data
		JSONObject oJson;
		try {
			oJson = new JSONObject(oData.toString());
			
			oMsg.target = oJson.getString("target");
			oMsg.nickname = oJson.getString("nick");
			oMsg.message = oJson.getString("message");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oMsg;
	}
	
	// create a zmsg from the chat message
	public ZMsg toZmsg() {
		ZMsg msg = new ZMsg();
		
		ZFrame channel = new ZFrame(target);
		ZFrame msgData = new ZFrame(toJSONString());
		
		msg.push(msgData);
		msg.push(channel);
		
		return msg;
	}
	
	// create a json string out of target, nickname and message
	public String toJSONString() {
		JSONObject data = new JSONObject();

		try {
			data.put("target", target);
			data.put("nick", nickname);
			data.put("message", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return data.toString();
	}
}

