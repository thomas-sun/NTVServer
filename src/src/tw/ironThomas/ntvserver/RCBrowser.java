package tw.ironThomas.ntvserver;


import java.io.File;
import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import tw.ironThomas.ntvserver.R;


public class RCBrowser extends Activity {
	final static int REQUEST_ENABLE = 1;
	private ListView mList;
	private ArrayAdapter<String> mAdapter;
	private String [] rcFile;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rcbrowser);

		
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		mList = (ListView) this.findViewById(R.id.files);
		mList.setAdapter(mAdapter);

		
		File file = new File(Util.getWorkingFolder() + "/rcdata/");
		File[] files = file.listFiles();
		
		if(files != null) {
			int idx = 0;
			rcFile = new String[files.length];
			for (File currentFile : files){
				if(currentFile.getName().endsWith(".json") == true) {
					rcFile[idx] = currentFile.getName();
					mAdapter.add(rcFile[idx]);
					idx++;
				}
			}
		}
		
		
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				Bundle bundle = new Bundle();
				bundle.putString("rcFile", rcFile[position]);
				
				Intent i = new Intent();
                i.putExtras(bundle); 
                setResult(RESULT_OK, i);
                finish();

			} 
		});

	}
	
	
	
	
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}	
	
}