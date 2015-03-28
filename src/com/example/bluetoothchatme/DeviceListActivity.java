package com.example.bluetoothchatme;

import java.util.Set;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity{

	protected static final String EXTRA_DEVICE_ADDRESS = "device_address";
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private BluetoothAdapter mBtAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.devicelist);
		setResult(Activity.RESULT_CANCELED);
		Button scanbutton=(Button)findViewById(R.id.button_scan);
		scanbutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				doDiscovery();
				v.setVisibility(View.GONE);
			}

			
		});
		mPairedDevicesArrayAdapter=new ArrayAdapter<String>(this , R.layout.devicename);
		mNewDevicesArrayAdapter=new ArrayAdapter<String>(this, R.layout.devicename);
		ListView pairedlistview=(ListView)findViewById(R.id.listView1);
		pairedlistview.setAdapter(mPairedDevicesArrayAdapter);
		pairedlistview.setOnItemClickListener(mDeviceClickListener);
		
		ListView newdevicelistview=(ListView)findViewById(R.id.listView2);
		newdevicelistview.setAdapter(mNewDevicesArrayAdapter);
		newdevicelistview.setOnItemClickListener(mDeviceClickListener);
		IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice>PairedDevices=mBtAdapter.getBondedDevices();
        if(PairedDevices.size()>0)
        {
        	findViewById(R.id.textView1).setVisibility(View.VISIBLE);
        	for (BluetoothDevice device : PairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
       
        }
		
	 @Override
     protected void onDestroy() {
         super.onDestroy();
         // Make sure we're not doing discovery anymore
         if (mBtAdapter != null) {
             mBtAdapter.cancelDiscovery();
         }
         // Unregister broadcast listeners
         this.unregisterReceiver(mReceiver);
     }
	private void doDiscovery() {
		// TODO Auto-generated method stub
		setProgressBarIndeterminateVisibility(true);
		setTitle("scanning");
		findViewById(R.id.textView2).setVisibility(View.VISIBLE);
		if(mBtAdapter.isDiscovering())
		{
			mBtAdapter.cancelDiscovery();
		}
		mBtAdapter.startDiscovery();
	}
	
	private OnItemClickListener mDeviceClickListener=new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v , int arg2,long arg3) {
			// TODO Auto-generated method stub
			mBtAdapter.cancelDiscovery();
			String info=((TextView )v).getText().toString();
			String address=info.substring(info.length()-17);
			Intent intent=new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			setResult(Activity.RESULT_OK,intent);
			finish();
		}
	};
private final BroadcastReceiver mReceiver=new BroadcastReceiver(){

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action=intent.getAction();
		if(BluetoothDevice.ACTION_FOUND.equals(action)){
			BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if(device.getBondState()!=BluetoothDevice.BOND_BONDED){
				mNewDevicesArrayAdapter.add(device.getName()+ " \n" +device.getAddress());
			}
		}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
			setProgressBarIndeterminateVisibility(false);
			setTitle("select device" );
			if(mNewDevicesArrayAdapter.getCount()==0){
				String nodevice=getResources().getText(R.string.none_found).toString();
				mNewDevicesArrayAdapter.add(nodevice);
			}
		}
	}
	
};
}
