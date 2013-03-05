package org.dobots.zmqvideochat;

import org.dobots.zmqvideochat.R;
import org.dobots.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ChatSettings {
	
	public interface ChatSettingsChangeListener {
		public void onChange();
	}

	private static final int DIALOG_SETTINGS_ID = 1;

	private Activity context;

	private Dialog m_dlgSettings;

	private String m_strNickName;
	private String m_strAddress;
	private String m_strChatPort;
	private String m_strVideoPort;
	private String m_strEventPort;

	private boolean m_bSettingsValid;

	private ChatSettingsChangeListener m_oChangeListener;

	public ChatSettings(Activity i_oActivity) {
		context = i_oActivity;
	}
	
	public void setChatSettingsChangeListener(ChatSettingsChangeListener i_oListener) {
		m_oChangeListener = i_oListener;
	}
	
	public boolean isValid() {
		return m_bSettingsValid;
	}

	public void showDialog() {
		context.showDialog(DIALOG_SETTINGS_ID);
	}

    public Dialog onCreateDialog(int id) {
    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    	View layout;
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	switch (id) {
    	case DIALOG_SETTINGS_ID:
        	layout = inflater.inflate(R.layout.chat_settings, null);
        	builder.setTitle("Video-Chat Connection Settings");
        	builder.setView(layout);
        	builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface arg0, int arg1) {
    				adjustConnection();
    			}
    		});
        	m_dlgSettings = builder.create();
        	return m_dlgSettings;
    	}
    	return null;
    }

    public void onPrepareDialog(int id, Dialog dialog) {
    	if (id == DIALOG_SETTINGS_ID) {
    		// Pre-fill the text fields with the saved login settings.
    		EditText editText;
    		if (m_strNickName != null) {
    			editText = (EditText) dialog.findViewById(R.id.txtNickName);
    			editText.setText(m_strNickName);
    		}
    		if (m_strAddress != null) {
    			editText = (EditText) dialog.findViewById(R.id.txtAddress);
    			editText.setText(m_strAddress);
    		}
    		if (m_strChatPort != null) {
    			editText = (EditText) dialog.findViewById(R.id.txtChatPort);
    			editText.setText(m_strChatPort);
    		}
    		if (m_strVideoPort != null) {
    			editText = (EditText) dialog.findViewById(R.id.txtVideoPort);
    			editText.setText(m_strVideoPort);
    		}
    		if (m_strEventPort != null) {
    			editText = (EditText) dialog.findViewById(R.id.txtEventPort);
    			editText.setText(m_strEventPort);
    		}
    	}
    }
    
    public boolean checkSettings() {
		// Read the settings from the preferences file.
		SharedPreferences prefs = context.getPreferences(Activity.MODE_PRIVATE);
		m_strAddress = prefs.getString(ChatTypes.CHAT_PREFS_ADDRESS, ChatTypes.CHAT_DEFAULT_ADDRESS);
		m_strChatPort = prefs.getString(ChatTypes.CHAT_PREFS_CHATPORT, ChatTypes.CHAT_DEFAULT_CHATPORT);
		m_strVideoPort = prefs.getString(ChatTypes.CHAT_PREFS_VIDEOPORT, ChatTypes.CHAT_DEFAULT_VIDEOPORT);
		m_strEventPort = prefs.getString(ChatTypes.CHAT_PREFS_EVENTPORT, ChatTypes.CHAT_DEFAULT_EVENTPORT);
		m_strNickName = prefs.getString(ChatTypes.CHAT_PREFS_NICKNAME, ChatTypes.CHAT_DEFAULT_NICKNAME);
		
		// settings only valid if all values assigned
		m_bSettingsValid = (((m_strAddress != "") && (m_strAddress != null)) &&
							(Utils.isInteger(m_strChatPort)) &&
							(Utils.isInteger(m_strVideoPort)) &&
							(Utils.isInteger(m_strEventPort)) &&
							((m_strNickName != "") && (m_strNickName != null)));
		return m_bSettingsValid;
    }

    public void adjustConnection() {
    	// Read the login settings from the text fields.
    	EditText editText = (EditText) m_dlgSettings.findViewById(R.id.txtAddress);
		String strAddress = editText.getText().toString();
    	editText = (EditText) m_dlgSettings.findViewById(R.id.txtChatPort);
		String strChatPort = editText.getText().toString();
    	editText = (EditText) m_dlgSettings.findViewById(R.id.txtVideoPort);
		String strVideoPort = editText.getText().toString();
    	editText = (EditText) m_dlgSettings.findViewById(R.id.txtEventPort);
		String strEventPort = editText.getText().toString();
    	editText = (EditText) m_dlgSettings.findViewById(R.id.txtNickName);
		String strNickName = editText.getText().toString();
		
		// Save the current login settings
		SharedPreferences prefs = context.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(ChatTypes.CHAT_PREFS_ADDRESS, strAddress);
		editor.putString(ChatTypes.CHAT_PREFS_CHATPORT, strChatPort);
		editor.putString(ChatTypes.CHAT_PREFS_VIDEOPORT, strVideoPort);
		editor.putString(ChatTypes.CHAT_PREFS_EVENTPORT, strEventPort);
		editor.putString(ChatTypes.CHAT_PREFS_NICKNAME, strNickName);
		editor.commit();
		
		if (!checkSettings()) {
			Utils.showToast("Connection Settings not valid, please check again!", Toast.LENGTH_LONG);
		} else {
			m_oChangeListener.onChange();
		}
    }

	public String getAddress() {
		return m_strAddress;
	}

	public int getChatPort() {
		return Integer.valueOf(m_strChatPort);
	}

	public int getVideoPort() {
		return Integer.valueOf(m_strVideoPort);
	}
	
	public int getEventPort() {
		return Integer.valueOf(m_strEventPort);
	}
	
	public String getNickName() {
		return m_strNickName;
	}
	
}
