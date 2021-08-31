package com.example.test_hty711;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test_hty711.databinding.FragmentFirstBinding;
import com.whty.comm.inter.ICommunication;
import com.whty.tymposapi.DeviceApi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static androidx.core.content.ContextCompat.getSystemService;
import com.whty.TYMPosEMVdemo.utils.UIMessage;

public class FirstFragment extends Fragment{

    private FragmentFirstBinding binding;
    private final String TAG = "BLUETOOTH_FRAGMENT";

    private TextView deviceNameText;
    private TextView responseText;
    private Button scanBtn;
    private Button stopScanBtn;
    private Button readCardBtn;
    private DeviceListViewAdapter deviceListViewAdapter;
    ArrayList<String> deviceDisplayList = new ArrayList<>();
    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    //bluetooth ble variables
    private int languageID = 2;
    private boolean deviceConnected = false;
    private DeviceApi deviceApi;
    private DeviceDelegate delegate;
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;
    MyScanCallback myScanCallback;
    private CardInfo cardInfo = new CardInfo();
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    //fragment variables
    Context context;
    Button btn_start, btn_stop;
    TextView logger;
    private OnFragmentInteractionListener mListener;

    public interface OnFragmentInteractionListener {
        /**
         * Callback for when an item has been selected.
         */
        public void onDeviceSelected(BluetoothDevice d);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkpermissions();

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    deviceNameText.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        Log.d(TAG, "conecting status 1 - device = "+msg.obj);
                        deviceNameText.setText(""+msg.obj);
                    }else
                        responseText.setText("Ninguno aún..");
                }
            }

            @Override
            public void dispatchMessage(Message msg) {
                super.dispatchMessage(msg);
                String show_msg = "";
                switch (msg.what) {

                    case SharedMSG.SHOW_MSG:
                        show_msg = (String) msg.obj;
                        responseText.setText(show_msg);
                        if (show_msg.equals(UIMessage.connected_device_success)) {
                            deviceConnected = true;
                        }
                        break;

                    case SharedMSG.SHOW_STATUS:
                        show_msg = (String) msg.obj;
                        responseText.setText(show_msg);
                        break;

                    default:
                        break;
                }
            }
        };

        //Config
        Resources resources = getResources();// ���res��Դ����
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        Log.d(TAG, "Language is " + config.getLocales().get(0).getLanguage());
        Log.d(TAG, "language ID is " + languageID);
        resources.updateConfiguration(config, dm);
        UIMessage.setMessage(languageID);
        PrintMessage.setMessage(languageID);

        //DEVICE API INITIALIZATION
        delegate = new DeviceDelegate(mHandler);
        deviceApi = new DeviceApi(getContext());
        deviceApi.setDelegate(delegate);
        deviceApi.initDevice(ICommunication.BLUETOOTH_DEVICE);

        //UI
        this.deviceNameText = binding.deviceNameText;
        this.responseText = binding.responseText;
        this.scanBtn = binding.scanBtn;
        this.stopScanBtn = binding.stopScanBtn;
        this.readCardBtn = binding.readCardBtn;

        // set up the RecyclerView
        RecyclerView deviceRecyclerView = binding.deviceRecyclerView;
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        deviceListViewAdapter = new DeviceListViewAdapter(context, deviceDisplayList);
        deviceRecyclerView.setAdapter(deviceListViewAdapter);

        deviceListViewAdapter.setClickListener(new DeviceListViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // Do stuff here...
                Log.d(TAG, "Item clicked in position "+position);

                if(!adapter.isEnabled()) {
                    Toast.makeText(getContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    return;
                }

                responseText.setText("Connecting...");
                // Get the device MAC address, which is the last 17 chars in the View
//                String info = ((TextView) view).getText().toString();
//                final String address = info.substring(info.length() - 17);
//                final String name = info.substring(0,info.length() - 17);
                BluetoothDevice device = deviceList.get(position);

                connectDevice(device);
/*
                // Spawn a new thread to avoid blocking the GUI one
                new Thread()
                {
                    @Override
                    public void run() {
                        boolean fail = false;

                        try {
                            mBTSocket = createBluetoothSocket(device);
                        } catch (IOException e) {
                            fail = true;
                            Toast.makeText(getContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                        // Establish the Bluetooth socket connection.
                        try {
                            mBTSocket.connect();
                        } catch (IOException e) {
                            try {
                                fail = true;
                                mBTSocket.close();
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                Toast.makeText(getContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        if(!fail) {
                            responseText.setText("conectado a "+device.getName());
                            mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                            mConnectedThread.start();

                            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, device.getName())
                                    .sendToTarget();
                        }
                    }
                }.start();*/
            }
        });

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    deviceNameText.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        Log.d(TAG, "conecting status 1 - device = "+msg.obj);
                        deviceNameText.setText(""+msg.obj);
                    }else
                        responseText.setText("Ninguno aún..");
                }
            }

            @Override
            public void dispatchMessage(Message msg) {
                super.dispatchMessage(msg);
                String show_msg = "";
                switch (msg.what) {

                    case SharedMSG.SHOW_MSG:
                        show_msg = (String) msg.obj;
                        responseText.setText(show_msg);
                        if (show_msg.equals(UIMessage.connected_device_success)) {
                            deviceConnected = true;
                        }
                        break;

                    case SharedMSG.SHOW_STATUS:
                        show_msg = (String) msg.obj;
                        responseText.setText(show_msg);
                        break;

                    default:
                        break;
                }
            }
        };

        readCardBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){ readCard();}
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanBtn.setVisibility(View.INVISIBLE);

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            //Handle this issue. Report to the user that the device does not support BLE
            Log.d(TAG, "no bluetooth, shouldn't be here");
        } else {
            adapter = bluetoothManager.getAdapter();
        }
    }

    private void connectDevice(BluetoothDevice device){
        if (responseText.getText().toString()
                .equals(UIMessage.connecting_device)) {
            Toast.makeText(context,
                    UIMessage.connecting_device, Toast.LENGTH_SHORT)
                    .show();
        } else {
            if (deviceConnected) {
                Toast.makeText(context,
                        UIMessage.connected_device + " " + device.getName(),
                        Toast.LENGTH_SHORT).show();
            } else {
                responseText.setText(UIMessage.connecting_device);
                adapter.cancelDiscovery();
                if (device.getAddress() != null
                        && device.getAddress().length() > 0) {
                    new Thread() {
                        public void run() {
                            Looper.prepare();
                            deviceApi.connectDevice(device.getAddress());
                            // System.out.println("��ӡ������:"
                            // + deviceApi.getPrinterParams());
                            // // ����ʱ��
                            // deviceApi.setPrinterParams((byte) 0x01,
                            // 2);
                            // // �������ʱ��
                            // deviceApi.setPrinterParams((byte) 0x02,
                            // 5);
                            // // �о�
                            // deviceApi.setPrinterParams((byte) 0x03,
                            // 3);
                        }
                    }.start();
                } else {
                    responseText.setText(UIMessage.donot_select_device);
                }
            }
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Dispositivo encontrado");

            deviceDisplayList.add("Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            responseText.setText("Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            deviceListViewAdapter.notifyDataSetChanged();
        }
    };

    public void startScanning() {
        Log.d(TAG,"start scanning");
        deviceDisplayList.clear();
        deviceList.clear();
        deviceListViewAdapter.notifyDataSetChanged();
        responseText.setText("Start scanning...");
        scanBtn.setVisibility(View.INVISIBLE);
        stopScanBtn.setVisibility(View.VISIBLE);
        scanner = adapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        List<ScanFilter> scanFilters = Arrays.asList(
                new ScanFilter.Builder()
                        //.setServiceUuid(ParcelUuid.fromString("some uuid"))
                        .build());
        myScanCallback = new MyScanCallback();
        scanner.startScan(scanFilters, scanSettings, myScanCallback);
    }

    public void stopScanning() {
        Log.d(TAG,"stopping scanning");
        deviceDisplayList.clear();
        deviceList.clear();
        deviceListViewAdapter.notifyDataSetChanged();
        responseText.setText("Stop scanning...");
        scanBtn.setVisibility(View.VISIBLE);
        stopScanBtn.setVisibility(View.INVISIBLE);
        if (scanner != null) {
            scanner.stopScan(myScanCallback);
        }
    }

    public boolean deviceExists(BluetoothDevice device){
        for(int i = 0; i<deviceList.size(); i++){
            if(device.getAddress().equals(deviceList.get(i).getAddress())){
                return true;
            }
        }
        return false;
    }

    public class MyScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            //Do something with results
            BluetoothDevice device = adapter.getRemoteDevice(result.getDevice().getAddress());
            Log.d(TAG,device.getName() + " " + device.getAddress());
            if (!deviceExists(device)) {
                deviceList.add(device);
                deviceDisplayList.add(device.getName() + " rssi: " + result.getRssi() + "\n");
                deviceListViewAdapter.notifyDataSetChanged();
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //Do something with batch of results
            Log.d(TAG, "batch scan results");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "failed with error code "+errorCode);
            //Handle error
        }
    }

    void checkpermissions() {
        //first check to see if I have permissions (marshmallow) if I don't then ask, otherwise start up the demo.
        if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)  ||
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PRECISE_PHONE_STATE) != PackageManager.PERMISSION_GRANTED))   {
            //I'm on not explaining why, just asking for permission.
            Log.d(TAG, "asking for permissions");
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MainActivity.REQUEST_ACCESS_COURSE_LOCATION);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_PRECISE_PHONE_STATE},
                    2);
            Log.d(TAG,"We don't have permission to course location");
        } else {
            Log.d(TAG,"We have permission to course location");
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    private void readCard(){
        new Thread() {
            public void run() {
                // The parameter is��line1��line2��
                // inputType��0x00:have".",0x01:no"."���� time-out
                HashMap<String, String> amountData = deviceApi
                        .deviceCustomInput("hello",
                                "please input amount:",
                                (byte) 0x01, 20); //0x01 => sin punto desimal | 0x00 punto desimal
                if (amountData != null*-
                        && amountData.get("errorCode").equals(
                        "9000")) { // success
                    String amount = amountData.get("amount");
                    if (Integer.parseInt(amount) == 0) {
                        // enter is null
                        return;
                    }
                    setName("swipeCardThread");
                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyyMMddHHmmss", Locale.getDefault());
                    String terminalTime = format.format(new Date());
                    Log.e(TAG, "terminalTime:" + terminalTime);
                    // ����2014-12-03 16:20:55
                    // ��terminalTime����"141203162055";
                    // �������ʱ��ע�ⲻҪ��С���㣬�����Ҫ��1.50��д��"150";
                    // ���뽻������ (byte)0x00�������ѣ�(byte)0x31������ѯ���
                    // deviceApi.readCard("150",
                    // terminalTime.substring(2), (byte) 0x00,
                    // (byte) 0x64, (byte) 0x00);
                    Map<String, String> result = deviceApi
                            .readCard("1500",
                                    terminalTime.substring(2),
                                    (byte) 0x00, (byte) 0x64, (byte) 0x07);
                    Log.d(TAG, "readCard done!");
                    if(result != null){
                        for (Map.Entry<String, String> entry : result.entrySet()) {
                            Log.d(TAG,entry.getKey() + "=" + entry.getValue());
                        }
                    }else{
                        Log.d(TAG, "result is null");
                    }
                    if (result != null
                            && "9000".equals(result
                            .get("errorCode"))) {
                        // ˢ���ɹ�������ˢ������
                        cardInfo = new CardInfo();
                        cardInfo.setCardNo(result.get("cardNumber"));
                        cardInfo.setAmount("150");
                        cardInfo.setSwipeCardDate(terminalTime
                                .substring(0, 8));
                        cardInfo.setSwipeCardTime(terminalTime
                                .substring(8));
                        cardInfo.setValidThru(result
                                .get("expiryDate"));
                        cardInfo.setIcData55(result.get("icData"));
                        Log.d(TAG, "ˢ����Ϣ�ѱ���");
                        deviceApi.confirmTransaction("Se confirma la transa");
                    }
                }
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        if (deviceConnected) {
            new Thread() {
                public void run() {
                    deviceApi.disconnectDevice();
                }
            }.start();
        }
        Log.e(TAG, "onDetach() function is involked");
    }
}