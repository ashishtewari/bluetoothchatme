package com.example.bluetoothchatme;






import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_CONNECT_DEVICE = 1;
	protected static final int MESSAGE_STATE_CHANGE = 1;
	protected static final int MESSAGE_WRITE = 3;
	protected static final int MESSAGE_READ = 2;
	protected static final int MESSAGE_DEVICE_NAME = 4;
	protected static final int MESSAGE_TOAST = 5;
	protected static final String DEVICE_NAME = "device_name";
	protected static final String TOAST = "toast";
	private BluetoothAdapter mBluetoothAdapter=null;
	private BluetoothChatService mChatService=null;
	private ArrayAdapter<String> mConversationArraayAdapter;
	private ListView mConservationView;
	private EditText mOutEditText;
	
	private Button mSendbutton;
	private StringBuffer mOutStringBuffer;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter==null)
		{
			Toast.makeText(this, "Bluetooth is not avilable", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	   
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if(!mBluetoothAdapter.isEnabled())
		{
			Intent enableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
		}
		else
		{
			if(mChatService==null)
			{
				setupchat();
			}
		}
	}
	

@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(mChatService!=null)
		{
			if(mChatService.getState()==BluetoothChatService.STATE_NONE)
			{
			mChatService.start();	
			}
		}
	}


private void setupchat()
{
	mConversationArraayAdapter=new ArrayAdapter<String>(this, R.layout.message);
	mConservationView=(ListView)findViewById(R.id.listView1);
	mConservationView.setAdapter(mConversationArraayAdapter);
	mOutEditText=(EditText)findViewById(R.id.edit_text_out);
	mOutEditText.setOnEditorActionListener(mWriteListener);
	mSendbutton=(Button)findViewById(R.id.button_send);
	mSendbutton.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
		TextView view=(TextView)findViewById(R.id.edit_text_out);
		String message=view.getText().toString();
		sendMessage(message);
		}
	});
	mChatService=new BluetoothChatService(this,mHandler);
	mOutStringBuffer=new StringBuffer("");
}




@Override
protected void onPause() {
	// TODO Auto-generated method stub
	super.onPause();
}


@Override
protected void onStop() {
	// TODO Auto-generated method stub
	super.onStop();
}


@Override
protected void onDestroy() {
	// TODO Auto-generated method stub
	super.onDestroy();
	if(mChatService!=null)
	{
		mChatService.stop();
	}
}
private void ensureDiscoverable() {
    if (mBluetoothAdapter.getScanMode() !=
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
}
private void sendMessage(String message) {
	// TODO Auto-generated method stub
	if(mChatService.getState()!=BluetoothChatService.STATE_CONNECTED)
	{
		Toast.makeText(this, "not connected", Toast.LENGTH_LONG).show();
		return;
	}
	if(message.length()>0)
	{
		byte[]send=message.getBytes();
		mChatService.write(send);
		mOutStringBuffer.setLength(0);
		mOutEditText.setText(mOutStringBuffer);
	}
}
private TextView.OnEditorActionListener mWriteListener =new TextView.OnEditorActionListener() {
public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
    // If the action is a key-up event on the return key, send the message
    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
        String message = view.getText().toString();
        sendMessage(message);
    }
	return true;
    
}
};
private final Handler mHandler=new Handler()
{
	private Object mConnectedDeviceName;

	@Override
	public void handleMessage(Message msg)
	{
		switch(msg.what)
		{
		case MESSAGE_STATE_CHANGE:
			switch(msg.arg1)
			{
			case BluetoothChatService.STATE_CONNECTED:
				mConversationArraayAdapter.clear();
				break;
			case BluetoothChatService.STATE_CONNECTING:
				break;
			case BluetoothChatService.STATE_LISTEN:
			case BluetoothChatService.STATE_NONE:
				break;
			}
			break;
		case MESSAGE_WRITE:
			byte[]writeBuf=(byte[])msg.obj;
			String writeMessage=new String (writeBuf);
			mConversationArraayAdapter.add("Me: "+ writeMessage);
			break;
		case MESSAGE_READ:
			byte[]readBuf=(byte[])msg.obj;
			String readMessage=new String (readBuf);
			mConversationArraayAdapter.add(mConnectedDeviceName + ": "+ readMessage);
			break;
		 case MESSAGE_DEVICE_NAME:
             // save the connected device's name
             mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
             Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
             break;
         case MESSAGE_TOAST:
             Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
             break;
			
		}
	}
};
public void onActivityResult(int requestCode, int resultCode, Intent data) {
   
    switch (requestCode) {
    case REQUEST_CONNECT_DEVICE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
            // Get the device MAC address
            String address = data.getExtras()
                                 .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mChatService.connect(device);
        }
        break;
    case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
            // Bluetooth is now enabled, so set up a chat session
            setupchat();
        } else {
            // User did not enable Bluetooth or an error occured
           
            Toast.makeText(this,"bt_not_enabled_leaving", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId())
		{
		case R.id.scan:
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
		case R.id.discoverable:
			 ensureDiscoverable();
	            return true;
			
		}
		return false;
	}

}
