package com.brs.connect_arduino_bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
 
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;
 
public class MainActivity extends Activity implements OnClickListener {
 
    Button Connect;
    ToggleButton OnOff;
    TextView Result;
    private String dataToSend;
    private int BT_STATE = 0;
    public static final int NO_CONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int CONNECT_SUSPEND = 2;
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_READ_END = 3;
    public static final int MESSAGE_WRITE = 4;

    private static final String TAG = "ABT";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static String address = "00:13:04:03:11:92";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectedThread mConnectedThread;
    Handler handler = new Handler();
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];
    
    void updateTextMsg(String uStr) {
        String buf = Result.getText().toString();
        buf = uStr + buf;
        Result.setText(buf);
    }
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
 
		Connect = (Button) findViewById(R.id.connect);
		OnOff = (ToggleButton) findViewById(R.id.tgOnOff);
		Result = (TextView) findViewById(R.id.msg_arduino);
 
		Connect.setOnClickListener(this);
		OnOff.setOnClickListener(this); 
	}
	
    @Override
    public void onStart() {
    	super.onStart();
        if (BT_STATE != CONNECTED)
        {
        	CheckBt();
			if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        }
		
		//Toast.makeText(getApplicationContext(), "MainActivity onStart",
		//		Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();

		//Toast.makeText(getApplicationContext(), "MainActivity onResume",
		//		Toast.LENGTH_SHORT).show();
        if (BT_STATE == CONNECT_SUSPEND)
        {
    		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    		Toast.makeText(getApplicationContext(), "MainActivity BT Connection suspend",
    				Toast.LENGTH_SHORT).show();
    		try {
				Thread.sleep(5);
        		Connect();
    		} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
		//Toast.makeText(getApplicationContext(), "MainActivity onPause",
		//		Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onStop() {
        super.onStop();
		//Toast.makeText(getApplicationContext(), "MainActivity onStop",
		//		Toast.LENGTH_SHORT).show();
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
       
		try {
			btSocket.close();
		} catch (IOException e) {

		}
	}
 
	@Override
	public void onClick(View control) {
		switch (control.getId()) {
			case R.id.connect:
				if (BT_STATE != CONNECTED)
				  Connect();
			break;
			case R.id.tgOnOff:
				if (BT_STATE != CONNECTED)
					break;
				if (OnOff.isChecked()) {
					dataToSend = "1";
					writeData(dataToSend);
				} else if (!OnOff.isChecked()) {
					dataToSend = "0";
					writeData(dataToSend);
				}
			break;
		}
	}
 
	private void CheckBt() {
		if (mBluetoothAdapter == null) {
			//Toast.makeText(getApplicationContext(),
			//			"Bluetooth null !", Toast.LENGTH_SHORT).show();
        	//updateTextMsg("\nBluetooth null !\n");
        	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}

		if (!mBluetoothAdapter.isEnabled()) {
			//Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",
			//			Toast.LENGTH_SHORT).show();
        	updateTextMsg("\nBluetooth Disable\n");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
		}
 

	}

	public void Connect() {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    	//Log.d(TAG, address);
        //Log.d(TAG, "Connecting to ... " + device);
        {
	    	String pStr = "\nConnecting to ... " + device + "\n";
	    	updateTextMsg(pStr);
        }
        try {
        	btSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
        	updateTextMsg("\nCreate Connection Service ..\n");
        	if (mBluetoothAdapter.isDiscovering()) {
              mBluetoothAdapter.cancelDiscovery();
        	}
        	if (!btSocket.isConnected())
        	  btSocket.connect();
        	Log.d(TAG, "Connection made.");
        	String pStr = "\nConnect To Device" + device + "\n";
        	updateTextMsg(pStr);
        	
        } catch (IOException e) {
        	try {
        		btSocket.close();
        	} catch (IOException e2) {
        		Log.d(TAG, "Unable to end the connection");
            	updateTextMsg("\nUnable to end the connection\n");
        	}
        	String pStr = "\nBluetooth Device Creation Failed : " + String.valueOf(e) + "\n";
        	updateTextMsg(pStr);
        	BT_STATE = NO_CONNECTED;
        	Log.d(TAG, "Socket creation failed");
        	return;
        }
        BT_STATE = CONNECTED;

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
	}
       
	private void writeData(String data) {
		try {
			outStream = btSocket.getOutputStream();
		} catch (IOException e) {
			Log.d(TAG, "Bug BEFORE Sending stuff", e);
		}
 
		String message = data;
		byte[] msgBuffer = message.getBytes();
    	String pStr = "\nWrite : " + data + "\n";
    	updateTextMsg(pStr);
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Log.d(TAG, "Bug while sending stuff", e);
		}
	}

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[512];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);                     

                    // Send the obtained bytes to the UI Activity
                   	mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                   						.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    void RawData_parsing(String rwData) {
    	updateTextMsg("Parsing");
    	updateTextMsg(rwData);

    	if(rwData.contentEquals("1"))
    	{
    		updateTextMsg("\nshould show string\n");
    		TextView strV = (TextView) findViewById(R.id.textView1);
    		if (!strV.isShown())
    			strV.setVisibility(View.VISIBLE);
    		else
    			strV.setVisibility(View.INVISIBLE);
    	}
    	else if(rwData.contentEquals("2"))
    	{
    		updateTextMsg("\nshould show voice\n");

    		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 1);
    		alertId = soundPool.load(this, R.raw.a0061, 0); 

    		aHandler = new Handler();
    		aHandler.post(runnable); 
    	}
    	else if (rwData.contentEquals("3"))
    	{
    		updateTextMsg("\nshould show video\n");
    		VideoView video = (VideoView) findViewById(R.id.video);
    		video = (VideoView) findViewById(R.id.video);

    	    // Load and start the movie
    		if (!video.isShown())
    			video.setVisibility(View.VISIBLE);
    		video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.boskb));
    		
    	    video.start();
            
    	}

    }
    private int alertId;
    private SoundPool soundPool;
    Handler aHandler;
    final Runnable runnable = new Runnable() {
    	public void run() {
    	try{
    		Thread.sleep(100);
    	}catch(InterruptedException e){
    		e.printStackTrace();
    	}
    	soundPool.play(alertId, 1, 1, 0, 0, 1);
    }};
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
            	RawData_parsing(readMessage);
                break;
            case MESSAGE_READ_END:
            	updateTextMsg("Read end\n");
            	break;
            }
        }
    };
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.Exit:
        	BT_STATE = 0;
        	finish();
        	return false;
        }
        return false;
    }
}
