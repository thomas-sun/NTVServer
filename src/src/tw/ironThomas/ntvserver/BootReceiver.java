package tw.ironThomas.ntvserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
	

	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			context.startService(new Intent(context, NativeService.class));
		}
	} 
	
}
