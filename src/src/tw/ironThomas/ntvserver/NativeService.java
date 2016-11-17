package tw.ironThomas.ntvserver;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import com.mstar.android.camera.MCamera;

 
public class NativeService extends Service {
	private final static String tag = "NTVService";

	
	static final String HOST_IP = "127.0.0.1";
	  
    private void SetKey(){
    	SharedPreferences  settings = PreferenceManager.getDefaultSharedPreferences(NativeService.this);
        String LoginKey = settings.getString("login_key", "");
        if(LoginKey.equals("")) {
        	Log.i(tag, "key is empty");
        }
        else {
        	Log.i(tag, LoginKey);

    		try {
    			Thread.sleep(2000);
    			Socket socket;
    			socket = new Socket(HOST_IP, Util.GetLocalhostPort());
    			byte[] data = LoginKey.getBytes("UTF-8");
    			OutputStream os = socket.getOutputStream();
    			os.write(data);	
    			socket.close();
    		} catch (UnknownHostException e) {
    			// TODO Auto-generated catch block
    			Log.i(tag, "UnknownHostException");
    			e.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			Log.i(tag, "IOException");
    			e.printStackTrace();
    		} catch (InterruptedException e) {
				// TODO Auto-generated catch block
    			Log.i(tag, "InterruptedException");
				e.printStackTrace();
			}

        }

    }
    
    
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(tag, "start NTVService");
		Util.StartNativeService();
		Thread t=new Thread(new Runnable(){        
            @Override
            public void run() {
            	SetKey();
            }
		});
		t.start();		
	} 

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
