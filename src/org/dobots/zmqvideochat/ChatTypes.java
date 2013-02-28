package org.dobots.zmqvideochat;

public class ChatTypes {

	public static final int INCOMING_CHAT_MSG 	= 1000;

	public static final int INCOMING_VIDEO_MSG 	= 2000;
	public static final int SET_USER_FPS 		= 2001;
	public static final int SET_PARTNER_FPS 	= 2002;

	public static final int UPDATE_CLIENTS 		= 3000;
	
	// The following strings are used as keys for reading and writing the
	// values needed to connect to Spykee.
	public static final String CHAT_PREFS_ADDRESS 		= "address";
	public static final String CHAT_PREFS_CHATPORT 		= "chat_port";
	public static final String CHAT_PREFS_VIDEOPORT 	= "video_port";
	public static final String CHAT_PREFS_EVENTPORT 	= "event_port";
	public static final String CHAT_PREFS_NICKNAME		= "nickname";
	
	public static final String CHAT_DEFAULT_ADDRESS 	= null;
	public static final String CHAT_DEFAULT_CHATPORT 	= null;
	public static final String CHAT_DEFAULT_VIDEOPORT 	= null;
	public static final String CHAT_DEFAULT_NICKNAME	= null;
	public static final String CHAT_DEFAULT_EVENTPORT 	= null;

	
}
