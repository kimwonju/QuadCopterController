package com.quadcoptercontroller;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private String mConnectedDeviceName = null;
    
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    
	ImageButton mMoveForwardButton;
	ImageButton mMoveBackButton;
	ImageButton mMoveLeftButton;
	ImageButton mMoveRightButton;
	
	ImageButton mLandingButton;
	ImageButton mTakeOffButton;
	ImageButton mRotateLeftButton;
	ImageButton mRotateRightButton;
	
	ImageButton mBluetoothButton;
	ImageButton mPowerButton;
	
	TextView mLogTextView;
	TextView mBluetoothState;
	TextView mState;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
		mMoveForwardButton = (ImageButton)findViewById(R.id.moveforwardButton);
		mMoveBackButton = (ImageButton)findViewById(R.id.movebackwordButton);
		mMoveLeftButton = (ImageButton)findViewById(R.id.moveleftButton);
		mMoveRightButton = (ImageButton)findViewById(R.id.moverightButton);
		
		mLandingButton = (ImageButton)findViewById(R.id.landingButton);
		mTakeOffButton = (ImageButton)findViewById(R.id.takeoffButton);
		mRotateLeftButton = (ImageButton)findViewById(R.id.rotateleftButton);
		mRotateRightButton = (ImageButton)findViewById(R.id.rotaterightButton);
		
		mBluetoothButton = (ImageButton)findViewById(R.id.bluetoothButton);
		mPowerButton = (ImageButton)findViewById(R.id.powerButton);
		
		mBluetoothState = (TextView)findViewById(R.id.bluetooth_state);
		mLogTextView = (TextView)findViewById(R.id.log);
		mState = (TextView)findViewById(R.id.state);
		
		mMoveForwardButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Move Forward : X"); }
		});
		
		mMoveBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Move Back : x"); }
		});
		
		mMoveLeftButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Move Left : Y"); }
		});
		
		mMoveRightButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Move Right : y"); }
		});
		
		mLandingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Landing : d"); }
		});
		
		mTakeOffButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Take off : u"); }
		});
		
		mRotateLeftButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Rotate Left : l"); }
		});
		
		mRotateRightButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("Rotate Right : r"); }
		});
		
		mBluetoothButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
		        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		});
		
		mPowerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { sign("power : p"); }
		});
		
		mBluetoothState.setText(R.string.title_not_connected);
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		ActionBar actionbar = getActionBar();
		actionbar.hide();
		return true;
	}
	
	private void sign(String func) {
		mLogTextView.setText("Command: " + func);
		String subs = func.substring(func.length()-1);
		Log.i("key", subs);
		sendMessage(subs);
	}
	
	private void setStateText(String state) {
		mState.setText("State: " + state);
	}
	
	
    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();

        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              mChatService.start();
            }
        }
    }


    private void setupChat() {
        mChatService = new BluetoothChatService(this, mHandler);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) mChatService.stop();
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupChat();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void sendMessage(String message) {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mBluetoothState.setText(R.string.title_connected_to);
                    mBluetoothState.append(mConnectedDeviceName);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mBluetoothState.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mBluetoothState.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                setStateText(readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
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


}
