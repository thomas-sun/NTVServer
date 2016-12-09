package tw.ironThomas.ntvserver;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Files;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mstar.android.camera.MCamera;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StreamService extends Service {
	private final String tag = "NTVService";

	public static final int Quality_FullHD = 0;
	public static final int Quality_HD = 1;
	public static final int Quality_DVD = 2;
	public static int mQuality;
	
	public static final int RCState_Stoped		= 0;
	public static final int RCState_Running		= 1;
	public static final int RCState_WaitStop	= 1;
	
	String rcName;
	String rcFile;
	boolean bUseRC;
	
	
	
	private static final UUID SerialPort_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	
	
	private int RCState = RCState_Stoped;
	
	Map<String, String> rcMap =  new  HashMap<String, String>();
	
	
	
	
	BluetoothAdapter mBluetoothAdapter = null;
	BluetoothDevice mRC = null;
	DatagramSocket mServer = null;
	
	

	boolean OpenRC()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
             Toast.makeText(this,
                  "Bluetooth is not available.",
                  Toast.LENGTH_LONG).show();
             return false;
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this,
                 "Please enable your BT and re-run this program.",
                 Toast.LENGTH_LONG).show();
            return false;
       }        
        
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals(rcName)) 
                {
                	mRC = device;
                    return true;
                }
            }
        }
        return false;
	
	}
	
	public void stopRC() {
		Log.i(tag, "stop rc");
		if(RCState != RCState_Running)
    		return ;
		
		RCState = RCState_WaitStop;
		
		if(mServer != null)
			mServer.close();
	} 
	
	  
    public boolean startRC() {
    	
    	Log.i(tag, "startRC");
    	if(RCState != RCState_Stoped)
    		return false;
    	
    	
    	Log.i(tag, "openRC");
    	if(OpenRC() == false)
    		return false;
    	
    	RCState = RCState_Running;
    	Log.i(tag, "RC on");
    	
		Thread t=new Thread(new Runnable(){        
	            @Override
	            public void run() {
	            	
	            	BluetoothSocket bs = null;
	            	OutputStream mOutputStream;
	            	InputStream mInputStream;
           
	            	
	            	try {
	            		byte[] recvBuf = new byte[100];
	            		mServer = new DatagramSocket(11235);
	                    DatagramPacket recvPacket = new DatagramPacket(recvBuf,
	                            recvBuf.length);
	                    
	            		
	    				// MY_UUID is the app's UUID string, also used by the server
	    				// code
	    				bs = mRC.createRfcommSocketToServiceRecord(SerialPort_UUID);
    					bs.connect();
    					
    					mOutputStream = bs.getOutputStream();
    					mInputStream = bs.getInputStream();
 
    					String cmd;
    					int nSendCount;
    					while(RCState == RCState_Running) {
    						
    						mServer.receive(recvPacket);
    	                    cmd = new String(recvPacket.getData(), 0,
    	                            recvPacket.getLength());
    	                    
							if(cmd != null) {
    							if(cmd.startsWith("key:")) {
    								String func = rcMap.get(cmd.substring(4));
    								Log.i(tag, "cmd:"+cmd+" func:"+func+ " len:"+(func.length()-2)); // [ ]
    								if(func != null) {
    									nSendCount = 0;
    									for(int i = 0; i < func.length(); i++) {
    										mOutputStream.write(func.charAt(i));
    										nSendCount++;
    										if(nSendCount == 32) {
    											nSendCount = 0;
    											mOutputStream.flush();
    											Thread.sleep(10);
    										}
    									}
    									if(nSendCount != 0) {
											mOutputStream.flush();
										}
    									Thread.sleep(800);
    								}
    							} 
    						}

							
							
       						
    					}
    					
    					if (bs != null && bs.isConnected()) {
    						bs.close();
    					}
   						
	    				
	    			} catch (IOException e) {
	    				try {
	    					if (bs != null && bs.isConnected()) {
	    						bs.close();
	    					}
	    				} catch (IOException closeException) {
	    				}
	    			}
					catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	
    				try {
    					if (bs != null && bs.isConnected()) {
    						bs.close();
    					} 
    					if(mServer != null)
    						mServer = null;
    				} catch (IOException closeException) {
    				}
	            }
	            
	             
		  });
		
		
		if(mRC != null)
			t.start();
		
		return true;

    }   

	 
	
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(5); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    
    
    
    
    void LoadRCCode()
    {
    	if(rcFile.equals("") == true)
    		return;
    		
    	File f = new File(Util.getWorkingFolder() + "/rcdata/"+rcFile);
    	
    	if(f.exists() == false)
    		return;
   	   	
    	StringBuffer sb = new StringBuffer("");

        try {
        	InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");
        	BufferedReader buffreader = new BufferedReader(isr);
        	 
        	
        	
        	String readString;
        	
        	 while ( (readString = buffreader.readLine())   != null ) {
                 sb.append(readString);
             } 
        	     
        	 isr.close(); 
        	            
            JSONObject json = new JSONObject(sb.toString());
            String keys = json.getString("key");
            JSONArray keyArray = new JSONArray(keys);
            
            rcMap.clear();
            
            Log.i(tag, "Identification:"+ json.getString("Identification"));
            Log.i(tag, "Version:"+ json.getString("Version"));
            Log.i(tag, "Description:"+ json.getString("Description"));
            
            for (int i = 0; i < keyArray.length(); i++) {
            	String func = keyArray.getJSONObject(i).getString("function");
            	String code = keyArray.getJSONObject(i).getString("code");
            	rcMap.put(func, code);
            	
            	Log.i(tag, "---------------------------------------------------");
                Log.i(tag, "function:"+func); 
            	Log.i(tag, "code:"+ code);
           	
            }
            

        } catch (IOException e) {
            e.printStackTrace();

        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   	
    	
    }
    
    private void ReleaseService(){
    	stopRC();
        releaseMediaRecorder();
        releaseCamera();
    }    
    
    

    android.hardware.Camera mCamera = null;
    MediaRecorder mMediaRecorder = null;

    @SuppressWarnings("deprecation")
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Util.StartNativeService();
    	
    	SharedPreferences  settings = PreferenceManager.getDefaultSharedPreferences(StreamService.this);
    	rcName = settings.getString("rc_name", "");
    	rcFile = settings.getString("rc_file", "");
    	bUseRC = settings.getBoolean("use_rc",false);
    	
    	
        if(bUseRC) {
        	LoadRCCode();
        	startRC();
        }
        
        // try cleanup first
        releaseMediaRecorder();
        releaseCamera();
        


        int audio_bitrate =  128 * 1024;
        int video_bitrate;
        int video_width;
        int video_height;
        int video_framerate =  30;
   
        int cam_size;
         

        switch (mQuality){
             
            case Quality_FullHD:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_1920_1080;
                video_width = 1920; video_height = 1080;
                video_bitrate =  4500 * 1024;
                break;
            case Quality_HD:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_1280_720;
                video_width = 1280; video_height = 720;
                video_bitrate =  2500 * 1024;
                break;
                
            default:
            case Quality_DVD:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_720_480;
                video_width = 720; video_height = 480;
                video_bitrate =  1500 * 1024;
                break;
        }

        // initialize streaming hardware
        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();
        
         
        Camera.Parameters camParams = mCamera.getParameters();
        camParams.set(MCamera.Parameters.KEY_TRAVELING_RES, cam_size);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MODE, MCamera.Parameters.E_TRAVELING_ALL_VIDEO);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MEM_FORMAT, MCamera.Parameters.E_TRAVELING_MEM_FORMAT_YUV422_YUYV);
        camParams.set(MCamera.Parameters.KEY_MAIN_INPUT_SOURCE, MCamera.Parameters.MAPI_INPUT_SOURCE_HDMI);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_FRAMERATE, video_framerate);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_SPEED, MCamera.Parameters.E_TRAVELING_SPEED_FAST); //.E_TRAVELING_SPEED_FAST
        mCamera.setParameters(camParams);
        
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        // set TS
        mMediaRecorder.setOutputFormat(8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioChannels(2);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setAudioEncodingBitRate(audio_bitrate);

        mMediaRecorder.setVideoSize(video_width, video_height);
        mMediaRecorder.setVideoFrameRate(video_framerate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        
        mMediaRecorder.setVideoEncodingBitRate(video_bitrate);

		mMediaRecorder.setOutputFile(Util.GetFileDescriptor());
		
      
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(tag, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();

        } catch (IOException e) {
            Log.d(tag, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }

        try {
            mMediaRecorder.start();
            Toast.makeText(this, "streaming started",Toast.LENGTH_LONG).show();

        } catch (Exception e){
            Log.d(tag, "streaming failed to start");
            ReleaseService();
            Toast.makeText(this, "Failed to start streaming",Toast.LENGTH_LONG).show();
        }

        return Service.START_NOT_STICKY;

    }



    public void onDestroy() {
    	ReleaseService();
    }


    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
