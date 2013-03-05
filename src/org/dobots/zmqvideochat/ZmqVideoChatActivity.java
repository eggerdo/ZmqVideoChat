package org.dobots.zmqvideochat;

import org.dobots.zmqvideochat.R;
import org.dobots.utils.Utils;
import org.dobots.zmqvideochat.ChatSettings.ChatSettingsChangeListener;
import org.dobots.zmqvideochat.chat.ChatHandler;
import org.dobots.zmqvideochat.event.EventHandler;
import org.dobots.zmqvideochat.gui.CameraPreview;
import org.dobots.zmqvideochat.video.VideoHandler;
import org.zeromq.ZContext;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ZmqVideoChatActivity extends Activity {

	// menu id
	private static final int SETTINGS_ID 		= 0;
	private static final int REFRESH			= 1;
	private static final int SCALE_ID 			= 2;
	private static final int ROTATE_LEFT_ID 	= 3;
	private static final int ROTATE_RIGHT_ID	= 4;
	
	private static ZmqVideoChatActivity INSTANCE;

	private ZContext ctx;
		
	private ChatSettings m_oSettings;
	private ChatHandler m_oChatHandler;
	private VideoHandler m_oVideoHandler;
	private EventHandler m_oEventHandler;

	// gui elements
	private EditText m_edtMessage;
	private ListView m_lvChat;
	private Button m_btnSend;
	private ListView m_lvClients;
	private TextView lblFPSUser;
	private TextView lblFPSPartner;
	private CameraPreview m_svCameraUser;
	private SurfaceView m_svCameraPartner;

	// array stores the received and sent messages
	private ArrayAdapter<String> chatMessages;
	// array stores the list of registered clients
	private ArrayAdapter<String> chatClients;
	
	private String m_strChannel = "#something";

	// debug, shows FPS for video
	private boolean m_bDebug = true;
	// flag to specify if we have connection
	private boolean m_bConnected = false;
	// flag defines if the received video frame should be scaled to the
	// available image size
	private boolean m_bScaleReceivedVideo = false;

	// defines by which angle the received video frame should be rotated
	// by default the image received from the camera (and the one sent over
	// zmq is rotated by 90°. thus we have to rotate it back again to 
	// display it normally on the screen
	private int nRotation = -90;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		INSTANCE = this;
		
		m_oSettings = new ChatSettings(this);
		m_oSettings.setChatSettingsChangeListener(new ChatSettingsChangeListener() {
			
			@Override
			public void onChange() {
				// if the settings change, close and reopen the connections / sockets
				closeConnections();
				setupConnections();
			}
		});
		
		ctx = new ZContext();
		
		m_oChatHandler = new ChatHandler(ctx, m_oSettings, uiHandler);
		m_oChatHandler.setChatRoom(m_strChannel);
		
		m_oVideoHandler = new VideoHandler(ctx, m_oSettings, uiHandler);
		m_oVideoHandler.setDebug(m_bDebug);
		
		m_oEventHandler = new EventHandler(ctx, m_oSettings, uiHandler);
		
		if (m_oSettings.checkSettings()) {
			// if the settings are valid, open the connections
			setupConnections();
		} else {
			Utils.showToast("Connection Settings not valid! Please edit settings.", Toast.LENGTH_LONG);
		}

		setProperties();
	}
	
	@Override
	protected void onStop() {
		closeConnections();

		super.onStop();
	}
	
	@Override
	protected void onResume() {
		if (!m_bConnected && m_oSettings.isValid()) {
			setupConnections();
		}
		
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		closeConnections();
		
		super.onDestroy();
	}
	
	private void setProperties() {
		
		m_edtMessage = (EditText) findViewById(R.id.edtMessage);
		
		m_lvChat = (ListView) findViewById(R.id.lvChatView);
		chatMessages = new ArrayAdapter<String>(this, R.layout.message);
		m_lvChat.setAdapter(chatMessages);
		
		m_lvChat.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		m_lvChat.setStackFromBottom(false);
		
		m_btnSend = (Button) findViewById(R.id.btnSend);
		m_btnSend.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (m_oSettings.isValid()) {
					// only attempt to send a message if the connection settings are valid
					String strMessage = m_edtMessage.getText().toString();
					m_oChatHandler.sendMessage(strMessage);
					m_edtMessage.getText().clear();
				} else {
					Utils.showToast("Connection Settings not valid! Please edit settings.", Toast.LENGTH_LONG);
				}
			}

		});
		
		m_svCameraUser = (CameraPreview) findViewById(R.id.svCameraUser);
		m_svCameraUser.setFrameListener(m_oVideoHandler);
		
		lblFPSUser = (TextView) findViewById(R.id.lblFPSUser);
		lblFPSPartner = (TextView) findViewById(R.id.lblFPSPartner);
		
		m_svCameraPartner = (SurfaceView) findViewById(R.id.svCameraPartner);
		m_svCameraPartner.setClickable(true);
		registerForContextMenu(m_svCameraPartner);
		
		m_lvClients = (ListView) findViewById(R.id.lvUsers);
		chatClients = new ArrayAdapter<String>(this, R.layout.clients);
		m_lvClients.setAdapter(chatClients);
		m_lvClients.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				String strClient = chatClients.getItem(paramInt);
				
				if (m_oVideoHandler.getPartner().equals(strClient)) {
					// by clicking a second time on the same user we unsubscribe again
					// from his video feed and set our partner to nobody
					m_oVideoHandler.setPartner("nobody");
					
					// then clear the video image
					Utils.runAsyncTask(new Runnable() {

						@Override
						public void run() {
							Canvas canvas = null;
			                try {
			                    canvas = m_svCameraPartner.getHolder().lockCanvas(null);
			                    Paint paint = new Paint();
			                    paint.setColor(Color.BLACK);
			                    canvas.drawPaint(paint);
			                } finally {
			                    if (canvas != null) {
			                    	m_svCameraPartner.getHolder().unlockCanvasAndPost(canvas);
			                    }
			                }
						}
					});
				} else {
					m_oVideoHandler.setPartner(strClient);
				}
			}
			
		});
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.svCameraPartner) {
			menu.setHeaderTitle(String.format("Adjust Video"));
			menu.add(0, SCALE_ID, SCALE_ID, "Scale Image")
				.setCheckable(true)
				.setChecked(m_bScaleReceivedVideo);
			menu.add(0, ROTATE_LEFT_ID, ROTATE_LEFT_ID, "Rotate Left 90°");
			menu.add(0, ROTATE_RIGHT_ID, ROTATE_RIGHT_ID, "Rotate Right 90°");
		}
	}
	
	private void closeConnections() {
		m_oChatHandler.closeConnections();
		m_oVideoHandler.closeConnections();
		m_oEventHandler.closeConnections();
		m_bConnected = false;
	}
	
	private void setupConnections() {
		m_oChatHandler.setupConnections();
		m_oVideoHandler.setupConnections();
		m_oEventHandler.setupConnections();
		m_bConnected = true;
	}
	
	public static Activity getContext() {
		return INSTANCE;
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, SETTINGS_ID, 0, "Settings");
		menu.add(0, REFRESH, REFRESH, "Refresh");
		
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case SETTINGS_ID:
    		m_oSettings.showDialog();
    		break;
		case REFRESH:
			m_oEventHandler.refresh();
			break;
		case SCALE_ID:
			m_bScaleReceivedVideo = !m_bScaleReceivedVideo;
			break;
		case ROTATE_RIGHT_ID:
			nRotation = (nRotation + 90) % 360;
			break;
		case ROTATE_LEFT_ID:
			nRotation = (nRotation - 90) % 360;
			break;
		}
		return true;
	}

	@Override
    public Dialog onCreateDialog(int id) {
    	return m_oSettings.onCreateDialog(id);
    }
    
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		m_oSettings.onPrepareDialog(id, dialog);
	}

	private Handler uiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case ChatTypes.INCOMING_CHAT_MSG:
				final String message = (String) msg.obj;
				
				// incoming chat message, update the array and notify the
				// list view that the data set has changed
				Utils.runAsyncUiTask(new Runnable() {
					
					@Override
					public void run() {
						chatMessages.add(message);
						chatMessages.notifyDataSetChanged();
					}
				});
				break;
			case ChatTypes.INCOMING_VIDEO_MSG:
				Bitmap bmp = (Bitmap) msg.obj;
				
                Canvas canvas = null;
                try {
                    canvas = m_svCameraPartner.getHolder().lockCanvas(null);

                    Matrix matrix = new Matrix();

                    int dstWidth = m_svCameraPartner.getWidth();
                    int dstHeight = m_svCameraPartner.getHeight();
                    
                    if (m_bScaleReceivedVideo) {
                    	// if the video should be scaled, determine the scaling factors
                    	int srcWidth = bmp.getWidth();
                    	int srcHeight = bmp.getHeight();
                        if ((srcWidth != dstWidth) || (srcHeight != dstHeight)) {
	                        matrix.preScale((float) dstWidth / srcWidth, (float) dstHeight / srcHeight);
                        }
                    }

                    // set the rotation
                    matrix.postRotate(nRotation, dstWidth/2, dstHeight/2);
                    
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    // clear the canvas
                    canvas.drawPaint(paint);
                    // then draw the bitmap with the matrix (scale and rotation) to apply
                    canvas.drawBitmap(bmp, matrix, paint);
                } finally
                {
                    if (canvas != null) {
                    	m_svCameraPartner.getHolder().unlockCanvasAndPost(canvas);
                    }
                }
                break;
			case ChatTypes.SET_USER_FPS:
				if (m_bDebug) {
					final int fps_user = (Integer) msg.obj;
					
					// update the user's FPS
					Utils.runAsyncUiTask(new Runnable() {
						
		    			@Override
		    			public void run() {
		    				lblFPSUser.setText("FPS: " + String.valueOf(fps_user));
		    			}
		            	
		            });
				}
				break;
			case ChatTypes.SET_PARTNER_FPS:
				if (m_bDebug) {
					final int fps_partner = (Integer) msg.obj;
					
					// update the partner's fps
					Utils.runAsyncUiTask(new Runnable() {
						
            			@Override
            			public void run() {
            				lblFPSPartner.setText("FPS: " + String.valueOf(fps_partner));
            			}
                    	
                    });
				}
				break;
			case ChatTypes.UPDATE_CLIENTS:
				final String clients = (String) msg.obj;
				
				// update the list of registered clients and notify the list view
				// that the data set has changed
				Utils.runAsyncUiTask(new Runnable() {
					
        			@Override
        			public void run() {
        				chatClients.clear();
        				for (String client : clients.split(";")) {
        					chatClients.add(client);
        				}
        				chatClients.notifyDataSetChanged();
        			}
                	
                });
				break;
			default:
				super.handleMessage(msg);
				break;
			}
			
		}
	};
	
}
