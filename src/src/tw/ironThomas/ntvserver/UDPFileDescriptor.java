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

import java.io.FileDescriptor;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.os.PowerManager;

public class UDPFileDescriptor
{ 
	
	FileDescriptor mFd = null;
	
    private native FileDescriptor open(String ip, int port, int cache_size);
    private native void  close(FileDescriptor fd);
    
    public FileDescriptor Open(String ip, int port, int cache_size) throws IOException  
    {
    	mFd = open(ip, port, cache_size);
    	return mFd; 
    }  
     
    public void Close()
    {
    	if(mFd != null) {
    		 close(mFd);
    		 mFd = null;
    	}
    }

    static {
        System.loadLibrary("UDPFileDescriptor");
    }
    
}
