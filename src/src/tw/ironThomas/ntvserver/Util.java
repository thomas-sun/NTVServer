package tw.ironThomas.ntvserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class Util
{
	private final static String tag = "NTVService";

	
	static public int strstr(String haystack, String needle) {
	    int m = haystack.length();
	    int n = needle.length();
	    int i, j;
	    for(i=0; i<=m-n; i++) {  // !!! <=
	        for(j=0; j<n && haystack.charAt(i+j)==needle.charAt(j); j++);
	        if(j==n) return i;
	    }
	    return -1;
	}
	
	
	static public FileDescriptor GetFileDescriptor()
	{
		try{
	        UDPFileDescriptor ufd = new UDPFileDescriptor();
			FileDescriptor fd = ufd.Open("127.0.0.1", 11234, 1024*128);
	        return fd;
        } catch(IOException e){
        	Log.d(tag, "fail to connect 127.0.0.1:11234 ");
        	return null;
        }		
		
	}
	

	static public String GetIPAddress()
	{
		
	    String urlString = "http://checkip.dyndns.org/";
	    HttpURLConnection connection = null;
	    String responseString = "";
	     
	    try {
	        URL url = new URL(urlString);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setReadTimeout(5000);
	        connection.setConnectTimeout(5000);
	        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36");
	        connection.setInstanceFollowRedirects(true);
	     
	        if( connection.getResponseCode() == HttpsURLConnection.HTTP_OK ){
	            InputStream     inputStream     = connection.getInputStream();
	            BufferedReader  bufferedReader  = new BufferedReader( new InputStreamReader(inputStream) );
	     
	            String tempStr;
	            StringBuffer stringBuffer = new StringBuffer();
	     
	            while( ( tempStr = bufferedReader.readLine() ) != null ) {
	                stringBuffer.append( tempStr );
	            }
	     
	            bufferedReader.close();
	            inputStream.close();
	     
	            responseString = stringBuffer.toString();
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    finally {
	        if( connection != null ) {
	            connection.disconnect();
	        }
	    }
	    return responseString;
	}
	
	
	static public String KeyGenerator(int len)
    {
    	Random ran = new Random();
    	byte [] key = new byte[len];
    	for (int x = 0; x < len; x++) {
    		key[x] = (byte) (33 + ran.nextInt(95)); // ascii 0x21 ~ 0x7f
    	}
    	String nk = new String(key);
    	return ("NTV_Key:" + nk);

    }
	
	
	static public void SaveQRcode(Bitmap bm)
	{
	    OutputStream fOutputStream = null;
	    File file = new File(getWorkingFolder() + "/qrcode.png");
 

	    try {
			fOutputStream = new FileOutputStream(file);
			bm.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream);
			fOutputStream.flush();
			fOutputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	}
	
	static public Bitmap CreateQRcode(String QRCodeContent)
    {
        int QRCodeWidth = 200;
        int QRCodeHeight = 200;
        
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
 
        MultiFormatWriter writer = new MultiFormatWriter();
        try
        {
            // L(7%)¡AM(15%)¡AQ(25%)¡AH(30%)
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
 
            BitMatrix result = writer.encode(QRCodeContent, BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeHeight, hints);

            Bitmap bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);

            for (int y = 0; y<QRCodeHeight; y++)
            {
                for (int x = 0;x<QRCodeWidth; x++)
                {
                    bitmap.setPixel(x, y, result.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            SaveQRcode(bitmap);
            return bitmap;
        }
        catch (WriterException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
 
    }
    
	
	static public int GetLocalhostPort()
	{
		return 1234;
	}
	
	
	static public String getWorkingFolder()
	{
		return "/sdcard/NTVServer";
	}
	 
	
	static public void SendShutdownCommand()
	{
		 
		Socket socket; 
		try {
			
			socket = new Socket("127.0.0.1", GetLocalhostPort());
			 
			String cmd = "cmd:shutdown";
			byte[] data = cmd.getBytes("UTF-8");
			
			OutputStream os = socket.getOutputStream();
			
			os.write(data);
			os.flush();  
			
			socket.close();
			 
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	
	static public void StartNativeService()
	{
		try {
			if(Util.CheckServiceExist() == false) {
				Runtime.getRuntime().exec(Util.getWorkingFolder() + "/NTVService");
			}
		} catch (Exception e) {
			Log.i(tag,"fail to start NTVService ");
	        e.printStackTrace();
		}
	}	
	
	
	static public boolean CheckServiceExist()
	{
		try {
			String[] cmd = {
					"sh",
					"-c",
					"ps | grep NTVService"
			};
	
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str = reader.readLine();
	
			if(str != null) {
				return true;
			}
			 
		} catch (Exception e) {
	        e.printStackTrace();
		}
		return false;
			
	}	
	
}