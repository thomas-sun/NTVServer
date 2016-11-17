package tw.ironThomas.ntvserver;


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


public class BTBrowser extends Activity {
	final static int REQUEST_ENABLE = 1;
	private ListView mList;
	private ArrayAdapter<String> mAdapter;
	private String [] rcName;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.btbrowser);

		
		
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		mList = (ListView) this.findViewById(R.id.files);
		mList.setAdapter(mAdapter);
		
		
		
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
			Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enabler, REQUEST_ENABLE);
        }
        
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        
        if(pairedDevices.size() > 0)
        {
        	int idx = 0;
        	rcName = new String[pairedDevices.size()];
            for(BluetoothDevice device : pairedDevices)
            {
            	rcName[idx++] = device.getName();
            	mAdapter.add(device.getName());
            }
        }
		
		
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				Bundle bundle = new Bundle();
				bundle.putString("rcName", rcName[position]);
				
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