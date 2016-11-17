/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tw.ironThomas.ntvserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import tw.ironThomas.ntvserver.R;
 

public class Install extends Activity 
{  
	 
	private String repositoryPath;
	private int need_reboot;
	private Context mContex;
	private final String tag = "NTVService";

    protected void createDirectory(String dirName) {
        File file = new File(repositoryPath + "/"+ dirName);
        if (!file.isDirectory()) file.mkdirs();
    }
     
    
    
    protected String unzip() {
        boolean isSuccess = true;

        ZipInputStream zipInputStream = null;
        
        createDirectory("");
        
        
        try {
            zipInputStream = new ZipInputStream(getAssets().open("data.zip"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        ZipEntry zipEntry;  
        
        try { 
            FileOutputStream fout;
            
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            	
                if (zipEntry.isDirectory()) {
                    createDirectory(zipEntry.getName());
                } else {
                	
                    fout = new FileOutputStream(repositoryPath + "/" + zipEntry.getName());
                    byte[] buffer = new byte[4096 * 10];
                    int length;
                    while ((length = zipInputStream.read(buffer)) != -1) {
                        fout.write(buffer, 0, length);
                    }
                    zipInputStream.closeEntry();
                    fout.close();
                }
            }
            
            zipInputStream.close();
        } catch (Exception e) {
            isSuccess = false;
            e.printStackTrace();
        }

        return null;
    }
    
    Handler mHandler; 
	 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.install);
		mContex =  this;
		repositoryPath = Util.getWorkingFolder();
		    
		 
	     mHandler = new Handler() {
	            public void handleMessage(Message msg) {
	                switch(msg.what){
	                    case 0:
	                    	Toast.makeText(mContex, "you need restart your tv box and reinstall this app.",Toast.LENGTH_LONG).show();
                        break;
	                }
	                super.handleMessage(msg);
	            }
	        };

	        
		
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				need_reboot = 1;
				
				for(int x = 0; x < 10; x++) {
					if(Util.CheckServiceExist() == false) {
						Log.i(tag, "The Service is Shutdown");
						need_reboot = 0;
						break;
					}
					Log.i(tag, "The Service is Running");
					
					if(x == 0) {
						Util.SendShutdownCommand();
					}
					 
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if(need_reboot == 1) {
					Message m = new Message();

			    	m.what = 0;
			    	mHandler.sendMessage(m);
			    	try {
			    		Thread.sleep(2000);
			    	} catch (InterruptedException e) {
					// TODO Auto-generated catch block
						e.printStackTrace();
			    	}
				} else {
					unzip();
					File f = new File("repositoryPath"+"/NTVService");
					if(f.exists()) {
						f.setExecutable(true);
					} 
					
					Intent i = new Intent();
	                setResult(RESULT_OK, i);
	                
					Util.StartNativeService();
				}
		       
	           finish();
		        
			}
			
		});
		t.start();
	}

}
