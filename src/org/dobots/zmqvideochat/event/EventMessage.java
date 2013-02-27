package org.dobots.zmqvideochat.event;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

public class EventMessage {
	
	// A ZMQ event message consists of two ZFrames:
	//
	// -------------------
	// | event | clients |
	// -------------------
	//
	// where event		is the event name which can be either 
	//					a register or an unregister
	// 		 data 		is the data sent with the event
	//
	// as of now, only one three events are used to register and
	// unregister a client and to receive a list of all registered
	// clients. in these cases, the data being sent is a comma
	// separated list of nicknames
	
	// event
	public String event = "";
	// list of nick names
	public String data = "";
	
	private EventMessage() {
		// nothing to do
	}
	
	public EventMessage(String i_strEvent, String i_strData) {
		event = i_strEvent;
		data = i_strData;
	}
	
	public static EventMessage fromZmsg(ZMsg i_oMsg) {
		EventMessage oEventMsg = new EventMessage();
		ZFrame oEvent = i_oMsg.pop();
		ZFrame oData = i_oMsg.pop();
		
		oEventMsg.event = oEvent.toString();
		oEventMsg.data = oData.toString();
		
		return oEventMsg;
	}
	
	public ZMsg toZmsg() {
		ZMsg oMsg = new ZMsg();
		ZFrame oEvent = new ZFrame(event);
		ZFrame oData = new ZFrame(data);
		
		oMsg.push(oData);
		oMsg.push(oEvent);
		
		return oMsg;
	}
	

}
