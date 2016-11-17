package tw.ironThomas.ntvserver;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import tw.ironThomas.ntvserver.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;
 
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;


public class MainActivity extends Activity {
	final int Result_Install	= 0;
	final int Result_RC_Device	= 1;
	final int Result_RC_Code	= 2;
	private final String tag = "NTVService";
	
	
	String LoginKey = "";
	String mCurrentIPAddress = "";

	LinearLayout mMenu;
	SurfaceView mSurfaceView;
	 
	RadioButton rbtnQuality_fullhd;
	RadioButton rbtnQuality_hd;
	RadioButton rbtnQuality_dvd;
	
	Button btnBackgroud;
	Button btnExit;
	Button btnPlay; 
	Button btnStop;
	Button btnChooseRc;
	Button btnCreateQRcode;

	CheckBox cbCheckPassword;
	CheckBox cbUseRC;
	
	ImageView imgQRcode;
	
	
	TextView mRc_name;
	TextView mRc_file;
	TextView mIPAddress;
	
	
	public boolean bUseRC;
	public boolean bCheckPassword;
	public String rcName="";
	public String rcFile="";

	
	private static final String ACTION_FINISH = 
	           "tw.ironThomas.ntvserver.ACTION_FINISH";
	
	static final String HOST_IP = "127.0.0.1";
	
	private Handler mHandler;
	
     
	private final class FinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_FINISH))
    			if(isStreamingRunning()) {
    				stopStreaming(); 
    				//OnStop();
    			}
            
            finish();
        }
    }
	
	 
	private FinishReceiver finishReceiver;
	
    

	
    public static void changeInputSource(TvOsType.EnumInputSource eis)
    {

        TvCommonManager commonService = TvCommonManager.getInstance();

        if (commonService != null)
        {
            TvOsType.EnumInputSource currentSource = commonService.getCurrentInputSource();
            if (currentSource != null)
            {
                if (currentSource.equals(eis))
                {
                    return;
                }

                commonService.setInputSource(eis);
            }
 
        }

    }
    
    private boolean isStreamingRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("tw.ironThomas.ntvserver.StreamService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    

	
    public static boolean enableHDMI()
    {
        boolean bRet = false;
        try 
        {
            changeInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_STORAGE);
            changeInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_HDMI);
            bRet = TvManager.getInstance().getPlayerManager().isSignalStable();
        } catch (TvCommonException e)
        {
            e.printStackTrace();
        }
        return bRet;
    } 
    
    
    private void startStreaming(){
    	startService(new Intent(this, StreamService.class));
    	
		Socket socket;
		try {
			socket = new Socket(HOST_IP, Util.GetLocalhostPort());
			 
			String cmd;
			
			if(rbtnQuality_fullhd.isChecked() == true) {
				cmd = "quality:1920x1080";
				StreamService.mQuality	= StreamService.Quality_FullHD;
			} else if(rbtnQuality_hd.isChecked() == true) {
				cmd = "quality:1280x720";
				StreamService.mQuality	= StreamService.Quality_HD;
			} else {
				cmd = "quality:720x480";
				StreamService.mQuality	= StreamService.Quality_DVD;
			}  
  
			if(bCheckPassword) {
				cmd = cmd + " " + LoginKey;
			} else { 
				cmd = cmd + " NTV_Key:no_key";
			}


			byte[] data = cmd.getBytes("UTF-8");
			OutputStream os = socket.getOutputStream();
			os.write(data);	
			socket.close();
			
			
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

    }
    
    private void stopStreaming(){
        stopService(new Intent(this, StreamService.class));
    }
    
    private ServiceConnection rc_connection = null;
    
    
    @Override
    protected void onResume(){
        super.onResume();
        enableHDMI();
    }  
            
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
        	case KeyEvent.KEYCODE_1: // full hd
        		if(btnPlay.isEnabled() == true) {
	        		rbtnQuality_fullhd.setChecked(true);
	        		rbtnQuality_hd.setChecked(false);
	        		rbtnQuality_dvd.setChecked(false);
	        		OnPlay();
	        		//finish();
        		}
        		break;
        	case KeyEvent.KEYCODE_2: // hd
        		if(btnPlay.isEnabled() == true) {
	        		rbtnQuality_fullhd.setChecked(false);
	        		rbtnQuality_hd.setChecked(true);
	        		rbtnQuality_dvd.setChecked(false);
	        		OnPlay();
	        		//finish();
        		}
        		break;        		
        	case KeyEvent.KEYCODE_3: // dvd
        		if(btnPlay.isEnabled() == true) {
	        		rbtnQuality_fullhd.setChecked(false);
	        		rbtnQuality_hd.setChecked(false);
	        		rbtnQuality_dvd.setChecked(true);        
	        		OnPlay();
	        		//finish();
        		}
        		break;        		
        	
        	case KeyEvent.KEYCODE_BACK:
        		return true;
            case KeyEvent.KEYCODE_MENU:
                if(mMenu.getVisibility() == View.VISIBLE) {
                	//mSurfaceView.setVisibility(View.VISIBLE);
                	mMenu.setVisibility(View.INVISIBLE);
                	mSurfaceView.bringToFront();
                }
                else {
                	mMenu.setVisibility(View.VISIBLE);
                	//mSurfaceView.setVisibility(View.INVISIBLE);
                	mMenu.bringToFront();
                }
                return true;
            default:
            	break;

        }
        return super.onKeyDown(keyCode, event);
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }
    

    
    void OnStop()
    {
    	stopStreaming();
		btnPlay.setEnabled(true);
		btnBackgroud.setEnabled(false);
		btnStop.setEnabled(false);
		btnCreateQRcode.setEnabled(true);
    }
    
    
    void OnPlay()
    {
		stopStreaming();
		
		btnPlay.setEnabled(false);
		btnBackgroud.setEnabled(true);
		btnStop.setEnabled(true);
		btnCreateQRcode.setEnabled(false);
		
		
		Thread t=new Thread(new Runnable(){        
            @Override
            public void run() {
            	startStreaming();
            }
		});
		t.start();
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
               super.onActivityResult(requestCode, resultCode, data);
               switch(requestCode){
               case Result_RC_Device:
                   if(resultCode == RESULT_OK) {
                	   String rc = data.getExtras().getString("rcName");
                	   if(rc.equals("") == true) {
                		   cbUseRC.setChecked(false); 
                	   }
                	   else
                	   {
                		   cbUseRC.setChecked(true);
                		   bUseRC = true;
                		   rcName = rc;
                		   
              				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            	        	editor.putBoolean("use_rc", true);
            	        	editor.putString("rc_name", rc);
            	    		mRc_name.setText(rc);
            	    		
            	        	
            	        	editor.commit();
                	   }
               	   
                   } else {
                	   cbUseRC.setChecked(false);
                   }
                   break;
                   
               case Result_RC_Code:
            	   if(resultCode == RESULT_OK) {
            		   String rc = data.getExtras().getString("rcFile");
            		   rcFile = rc;
            		   SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            		   editor.putString("rc_file", rc);
            		   editor.commit();
            		   mRc_file.setText(rc);
            		   
            	   }
            	   break;
            	   
               case Result_Install:
            	   break;
               default:
            	   break;
              }
    }
    
    

    
    
    

    
    
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);		
		setContentView(R.layout.activity_main);
		
		
		SharedPreferences  settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        boolean bInstalled = settings.getBoolean("install_service",false);
        


        Log.i(tag, "bInstalled:"+bInstalled);
        if(bInstalled == false) {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, Install.class);
            startActivityForResult(intent, Result_Install);
             
        	SharedPreferences.Editor editor = settings.edit();
        	editor.putBoolean("install_service", true);
        	editor.commit(); 
        }
       
		 
		
		finishReceiver= new FinishReceiver();
	    registerReceiver(finishReceiver, new IntentFilter(ACTION_FINISH));
		
	    
	    imgQRcode = (ImageView) findViewById(R.id.img_qrcode);
	    
        LoginKey = settings.getString("login_key", "");
        if(LoginKey.equals("")) {
        	LoginKey = Util.KeyGenerator(64);
        	
        	SharedPreferences.Editor editor = settings.edit();
        	editor.putString("login_key", LoginKey);
        	editor.commit();         	
        }
        
        
        Bitmap bitmap = Util.CreateQRcode(LoginKey);
		if(bitmap != null) {
			imgQRcode.setImageBitmap(bitmap);
		}
		
	    
	    
		
		mSurfaceView = (SurfaceView)this.findViewById(R.id.surfaceView);
		mMenu = (LinearLayout)this.findViewById(R.id.menu);
		
		
		
		mIPAddress = (TextView)this.findViewById(R.id.IPAddress);
		
		
		mRc_name = (TextView)this.findViewById(R.id.rc_name);
		rcName = settings.getString("rc_name", "");
		mRc_name.setText(rcName);
		
		mRc_file = (TextView)this.findViewById(R.id.rc_code_file);
		rcFile = settings.getString("rc_file", "");
		mRc_file.setText(rcFile);		
		
		
		
		cbCheckPassword = (CheckBox)this.findViewById(R.id.check_password);
		bCheckPassword = settings.getBoolean("check_password",false);
        if(bCheckPassword) {
        	cbCheckPassword.setChecked(true);
        } else {
        	cbCheckPassword.setChecked(false);
        }
        cbCheckPassword.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
				bCheckPassword = arg1;
	        	editor.putBoolean("check_password", bCheckPassword);
	        	editor.commit();
			} 
        	
        } );        
		
		
		
		cbUseRC = (CheckBox)this.findViewById(R.id.use_rc);
        bUseRC = settings.getBoolean("use_rc",false);
        
        if(bUseRC) {
        	cbUseRC.setChecked(true);
        } else {
        	cbUseRC.setChecked(false);
        }
        
        cbUseRC.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == false) {
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
					bUseRC = arg1;
		        	editor.putBoolean("use_rc", bUseRC);
		        	editor.commit();
				} else {
		            Intent intent = new Intent();
		            intent.setClass(MainActivity.this, BTBrowser.class);
		            startActivityForResult(intent, Result_RC_Device);
				}
			} 
        	
        } );
        
		
		
		rbtnQuality_fullhd = (RadioButton)this.findViewById(R.id.quality_fullhd);
		rbtnQuality_hd = (RadioButton)this.findViewById(R.id.quality_hd);
		rbtnQuality_dvd = (RadioButton)this.findViewById(R.id.quality_dvd);
		
		rbtnQuality_fullhd.setChecked(false);
		rbtnQuality_hd.setChecked(false);
		rbtnQuality_dvd.setChecked(true);
		rbtnQuality_dvd.requestFocus();
		
		
		boolean bRestart = false;

		Intent i = getIntent();
		if(i != null) {
			Bundle bundle = i.getExtras();
			if(bundle != null) {
				String dataQuaqlity = bundle.getString("quality");
				if(dataQuaqlity != null) {
					if(dataQuaqlity.equals("fullhd")) {
						rbtnQuality_fullhd.setChecked(true);
						rbtnQuality_hd.setChecked(false);
						rbtnQuality_dvd.setChecked(false);						
					} else if(dataQuaqlity.equals("hd")) {
						rbtnQuality_fullhd.setChecked(false);
						rbtnQuality_hd.setChecked(true);
						rbtnQuality_dvd.setChecked(false);						
					} else if(dataQuaqlity.equals("dvd")) {
						rbtnQuality_fullhd.setChecked(false);
						rbtnQuality_hd.setChecked(false);
						rbtnQuality_dvd.setChecked(true);						
					}
					bRestart = true;
				}
				
			}
		}
		
		btnBackgroud = (Button)this.findViewById(R.id.btnBackgrund);
		btnExit = (Button)this.findViewById(R.id.btnExit);
		btnPlay = (Button)this.findViewById(R.id.btnPlay);
		btnStop = (Button)this.findViewById(R.id.btnStop);
		btnCreateQRcode = (Button)this.findViewById(R.id.btn_create_qrcode);
		
		
		
		
		btnCreateQRcode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LoginKey = Util.KeyGenerator(64);
				
	        	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
	        	editor.putString("login_key", LoginKey);
	        	editor.commit();         	
				
				Bitmap bitmap = Util.CreateQRcode(LoginKey);
				if(bitmap != null) {
					imgQRcode.setImageBitmap(bitmap);
				}
			}
		});
		 
		
		btnChooseRc = (Button)this.findViewById(R.id.btn_choose_rccode);
		btnChooseRc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	            Intent intent = new Intent();
	            intent.setClass(MainActivity.this, RCBrowser.class);
	            startActivityForResult(intent, Result_RC_Code);				
			}
		});
		
		
	
		btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnStop();
			}
		});
		
	
		btnPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnPlay();
			}
		});
		 
		
		btnBackgroud.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		btnExit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopStreaming();
				finish();
			}
		}); 
		
		if(bRestart) {
			if(isStreamingRunning()) {
				stopStreaming();
			}  
			
			Thread t=new Thread(new Runnable(){        
	            @Override
	            public void run() {
	        		btnPlay.setEnabled(false);
	        		btnBackgroud.setEnabled(true);
	        		btnStop.setEnabled(true);
	        		btnCreateQRcode.setEnabled(false);
	        		
	        		
	            	startStreaming();
	            	//finish();
	            }
			});
			t.start();
			

		
			
		}
		else {
			
			mHandler = new Handler() {
	            public void handleMessage(Message msg) {
	            	int s,e;
	                switch(msg.what){
	                    case 0:
	                    	s = Util.strstr(mCurrentIPAddress, "Current IP Address:");
	                    	e = Util.strstr(mCurrentIPAddress, "</body>");
	                    	if(s != -1 && e != -1 && (s + 19) < e)
	                    		mIPAddress.setText(mCurrentIPAddress.substring(s+19, e));
	                        break;
	                }
	                super.handleMessage(msg);
	            }
	        };			 
	        
	        
			
			Thread t=new Thread(new Runnable(){        
	            @Override
	            public void run() {
	            	mCurrentIPAddress = Util.GetIPAddress();
	            	if(!mCurrentIPAddress.equals("")) {
	            		Message m = new Message();
                        m.what = 0;
                        mHandler.sendMessage(m);
	            	}
	            }
			});
			t.start();
			
			
			
			if(isStreamingRunning()) {
				btnPlay.setEnabled(false);
				btnStop.setEnabled(true);
				btnBackgroud.setEnabled(true);
				btnCreateQRcode.setEnabled(false);
			} else {
				btnPlay.setEnabled(true);
				btnStop.setEnabled(false);
				btnBackgroud.setEnabled(false);
				btnCreateQRcode.setEnabled(true);
			}			
		}
		
		
	}


}
