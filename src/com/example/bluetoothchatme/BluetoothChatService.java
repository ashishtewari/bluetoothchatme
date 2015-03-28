package com.example.bluetoothchatme;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BluetoothChatService {
	public static final int STATE_NONE = 0;
	public static final int  STATE_LISTEN = 1;
public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED =3;
	
	private final BluetoothAdapter mAdapter;
	private int  mState;
	private final Handler mHandler;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private AcceptThread mAcceptThread;
	private static final String NAME = "bluetoothchatme";
	private static final UUID My_UUID = UUID.fromString("35b542cd-8d51-43d7-b197-be3ced863b2c");

	public BluetoothChatService(Context context,Handler handler)
	{
		mAdapter=BluetoothAdapter.getDefaultAdapter();
		mState=STATE_NONE;
		mHandler=handler;
	}
	private synchronized void setState(int state)
	{
		 mState = state;
	        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}
public synchronized int getState()
{
	return mState;
}
public synchronized void start()
{
	if(mConnectThread!=null)
	{
		mConnectThread.cancel();
		mConnectThread=null;
	}
	if(mConnectedThread!=null)
	{
		mConnectedThread.cancel();
		mConnectedThread=null;
	}
	if(mAcceptThread==null)
	{
		mAcceptThread=new AcceptThread();
		mAcceptThread.start();
	}
	setState(STATE_LISTEN);
}
public synchronized void connect(BluetoothDevice device)
{
	if(mState==STATE_CONNECTING){
	if(mConnectThread!=null)
	{mConnectThread.cancel();mConnectThread=null;}
	}
	if(mConnectedThread!=null)
	{mConnectedThread.cancel();mConnectedThread=null;}
	
mConnectThread=new ConnectThread(device);
mConnectThread.start();
 setState(STATE_CONNECTING);
}

public synchronized void connected(BluetoothSocket socket,BluetoothDevice device)
{
	if(mConnectThread!=null)
	{
		mConnectThread.cancel();mConnectThread=null;
	}
	if(mConnectedThread!=null)
	{
		mConnectedThread.cancel();mConnectedThread=null;
	}
	if(mAcceptThread!=null)
	{
		mAcceptThread.cancel();mAcceptThread=null;
	}
	mConnectedThread=new ConnectedThread(socket);
	mConnectedThread.start();
	 Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
     Bundle bundle = new Bundle();
     bundle.putString(MainActivity.DEVICE_NAME, device.getName());
     msg.setData(bundle);
     mHandler.sendMessage(msg);
     setState(STATE_CONNECTED);
}
public  synchronized void stop()
{
	if(mConnectThread!=null)
	{
		mConnectThread.cancel();mConnectThread=null;
	}
	if(mConnectedThread!=null)
	{
		mConnectedThread.cancel();mConnectedThread=null;
	}
	if(mAcceptThread!=null)
	{
		mAcceptThread.cancel();mAcceptThread=null;
	}
	setState(STATE_NONE);
}
 
public void write(byte[]out)
{
	ConnectedThread r;
	synchronized (this) {
		if(mState!=STATE_CONNECTED)return;
		r=mConnectedThread;
	}
	r.write(out);
}
private void connectionFailed() {
    setState(STATE_LISTEN);
    // Send a failure message back to the Activity
    Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
    Bundle bundle = new Bundle();
    bundle.putString(MainActivity.TOAST, "Unable to connect device");
    msg.setData(bundle);
    mHandler.sendMessage(msg);
}
private void connectionLost() {
    setState(STATE_LISTEN);
    // Send a failure message back to the Activity
    Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
    Bundle bundle = new Bundle();
    bundle.putString(MainActivity.TOAST, "Device connection was lost");
    msg.setData(bundle);
    mHandler.sendMessage(msg);
}
private class AcceptThread extends Thread
{
	
	private final BluetoothServerSocket mmServerSocket;
	public AcceptThread()
	{
		BluetoothServerSocket tem=null;
		try{
			tem=mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, My_UUID);
		}catch (Exception e) {
			// TODO: handle exception
			
		}
		mmServerSocket=tem;
	}
	public void run()
	{
		setName("AcceptThread");
		BluetoothSocket socket=null;
		while(mState!=STATE_CONNECTED)
		{
			try{
				socket=mmServerSocket.accept();
			}catch (Exception e) {
				// TODO: handle exception
				break;
			}
			if(socket!=null)
			{
				synchronized (BluetoothChatService.this) {
					switch(mState)
					{
					case STATE_LISTEN:
					case STATE_CONNECTING:
						connected(socket,socket.getRemoteDevice());
						break;
					case STATE_NONE:
					case STATE_CONNECTED:
						try{
							socket.close();
						}catch (Exception e) {
							// TODO: handle exception
						}
						break;
					}
					
				}
			}
		}
	}
	public void cancel()
	{
		try{
			mmServerSocket.close();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
}

private class ConnectThread extends Thread
{
	private final BluetoothSocket mmSocket;
	private final BluetoothDevice mmDevice;
	public ConnectThread(BluetoothDevice device)
	{
		mmDevice=device;
		BluetoothSocket temp=null;
		try{
			temp=device.createRfcommSocketToServiceRecord(My_UUID);
		}catch (Exception e) {
			// TODO: handle exception
		}
		mmSocket=temp;
	}
	public void run()
	{
		setName("connect thread");
		mAdapter.cancelDiscovery();
		try{
			mmSocket.connect();
		}catch (Exception e) {
			// TODO: handle exception
			connectionFailed();
			try{
				mmSocket.close();
			}catch (Exception e2) {
				// TODO: handle exception
			}
			BluetoothChatService.this.start();
			return;
		}
		synchronized (BluetoothChatService.this) {
			mConnectThread=null;
			
		}
		connected(mmSocket,mmDevice);
	}
	public void cancel()
	{
		try{
			mmSocket.close();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
}
private class ConnectedThread extends Thread
{
	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;
	public ConnectedThread(BluetoothSocket socket)
	{
		mmSocket=socket;
		InputStream tempIn=null;
		OutputStream tempOut=null;
		try{
			tempIn=socket.getInputStream();
			tempOut=socket.getOutputStream();
		}catch (Exception e) {
			// TODO: handle exception
		}
		mmInStream=tempIn;
		mmOutStream=tempOut;
	}
	public void run()
	{
		byte[]buffer=new byte[1024];
		int bytes;
		while(true)
		{
			try{
				bytes=mmInStream.read(buffer);
				mHandler.obtainMessage(MainActivity.MESSAGE_READ,bytes,-1,buffer ).sendToTarget();
			}catch (Exception e) {
				// TODO: handle exception
				connectionLost();
				break;
			}
		}
	}
	public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);
            // Share the sent message back to the UI Activity
            mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                    .sendToTarget();
        } catch (IOException e) {
        }
    }
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
          
        }
    }
}


}
