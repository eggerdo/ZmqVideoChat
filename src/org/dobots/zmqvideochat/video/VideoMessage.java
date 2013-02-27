package org.dobots.zmqvideochat.video;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

public class VideoMessage {
	
	// A ZMQ video message consists of four ZFrames:
	//
	// ------------------------------------
	// | nick | width | height | rgb data |
	// ------------------------------------
	//
	// where nick		is the nick name of the sender which is used for
	//					channel subscription / filtering
	// 		 width	 	is the width of the contained video frame
	//		 height		is the height of the contained video frame
	//		 rgb data	is the video frame as a JPEG rgb array. 

	// the nick name of the sender
	public String nickname = "";
	// the video data as an rgb array
	public byte[] videoData = null;
	// width and hight of the video frame
	public int width, height;
	
	private VideoMessage() {
		// nothing to do
	}
	
	public VideoMessage(String i_strUserName, byte[] i_data, int i_width, int i_height) {
		nickname = i_strUserName;
		videoData  = i_data;
		width = i_width;
		height = i_height;
	}
	
	// create a video message out of the zmq message
	public static VideoMessage fromZMsg(ZMsg i_oMsg) {
		VideoMessage oVideoMsg = new VideoMessage();
		
		ZFrame target = i_oMsg.pop();
		ZFrame width = i_oMsg.pop();
		ZFrame height = i_oMsg.pop();
		ZFrame data = i_oMsg.pop();
		
		oVideoMsg.nickname = target.toString();
		oVideoMsg.height = Integer.valueOf(height.toString());
		oVideoMsg.width = Integer.valueOf(width.toString());
		oVideoMsg.videoData = data.getData();

		return oVideoMsg;
	}
	
	// create a zmq message
	public ZMsg toZmsg() {
		ZMsg msg = new ZMsg();
		
		ZFrame target = new ZFrame(nickname);
		ZFrame oHeight = new ZFrame(Integer.toString(width).getBytes());
		ZFrame oWidth = new ZFrame(Integer.toString(height).getBytes());
		ZFrame oVideoData = new ZFrame(videoData);
		
		msg.push(oVideoData);
		msg.push(oHeight);
		msg.push(oWidth);
		msg.push(target);
		
		return msg;
	}
}

