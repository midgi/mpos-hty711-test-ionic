package cordova.plugin.hty711;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.whty.TYMPosEMVdemo.utils.UIMessage;
import com.whty.audio.manage.util.Utils;
import com.whty.comm.inter.ICommunication;
import com.whty.device.utils.GPMethods;
import com.whty.tymposapi.DeviceApi;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

// Get context
//Context context=this.cordova.getActivity().getApplicationContext();
/**
 * This class echoes a string called from JavaScript.
 */
public class HTY711 extends CordovaPlugin {

    private static final String DEVICE_NAME = "suppay";

    private Context context;
    private String TAG = "HTY711";

    private int languageID = 2;
    //private DeviceListViewAdapter deviceListViewAdapter;
    //ArrayList<String> deviceDisplayList = new ArrayList<>();
    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private boolean deviceConnected = false;
    private DeviceApi deviceApi;
    private DeviceDelegate delegate;
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;
    MyScanCallback myScanCallback;
    private CardInfo cardInfo = null;
    private Handler mHandler; // Our main handler that will receive callback notifications

    BluetoothDevice connectedDevice = null;
    boolean connecting = false;
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private boolean apduResponseReceived = false;
    private byte[] apduResponse = new byte[256];
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private OnFragmentInteractionListener mListener;

    private Queue<String> messageQueue = new LinkedList<>();

    private boolean scanning = false;

    // public HTY711(Context context){
    //     this.context = context;
    // }

    public interface OnFragmentInteractionListener {
        /**
         * Callback for when an item has been selected.
         */
        public void onDeviceSelected(BluetoothDevice d);
    }

    public boolean hasMessages(){
        return !messageQueue.isEmpty();
    }

    public String popMessage(){
        return messageQueue.remove();
    }

    public Context getContext(){
        return context;
    }

    public boolean isConnected(){
        return deviceConnected;
    }

    public boolean isConnecting(){
        return connecting;
    }

    public CardInfo getCardInfo() {
        CardInfo cardInfo_ = cardInfo; cardInfo = null; return cardInfo_;
    }

    public void init(){
        this.context = this.cordova.getActivity().getApplicationContext();
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
                    messageQueue.add(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        Log.d(TAG, "conecting status 1 - device = "+msg.obj);
                        deviceConnected = true;
                        connecting = false;
                    }
                }
            }

            @Override
            public void dispatchMessage(Message msg) {
                super.dispatchMessage(msg);
                String show_msg = "";
                switch (msg.what) {

                    case SharedMSG.SHOW_MSG:
                        show_msg = (String) msg.obj;
                        //responseText.setText(show_msg);
                        if (show_msg.equals(UIMessage.connected_device_success)) {
                            deviceConnected = true;
                            connecting = false;
                        }
                        messageQueue.add((show_msg));
                        break;

                    case SharedMSG.SHOW_STATUS:
                        show_msg = (String) msg.obj;
                        messageQueue.add((show_msg));
                        //responseText.setText(show_msg);
                        break;

                    default:
                        break;
                }
            }
        };

        //Config
        /*Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        Log.d(TAG, "Language is " + config.getLocales().get(0).getLanguage());
        Log.d(TAG, "language ID is " + languageID);
        resources.updateConfiguration(config, dm);*/
        UIMessage.setMessage(languageID);
        PrintMessage.setMessage(languageID);

        //DEVICE API INITIALIZATION
        delegate = new DeviceDelegate(mHandler);
        deviceApi = new DeviceApi(getContext());
        deviceApi.setDelegate(delegate);
        deviceApi.initDevice(ICommunication.BLUETOOTH_DEVICE);

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            //Handle this issue. Report to the user that the device does not support BLE
            Log.d(TAG, "no bluetooth, shouldn't be here");
        } else {
            adapter = bluetoothManager.getAdapter();
        }

        new Thread(){
            public void run(){
                while(true){
                    if(!connecting && !deviceApi.isConnected() && !scanning){
                        deviceConnected = false;
                        startScanning();
                    }
                    try {
                        Thread.sleep(1000);
                        // Do some stuff
                    } catch (Exception e) {
                        e.getLocalizedMessage();
                    }
                }
            }
        }.start();

        Log.d(TAG, "hty711 inited waiting for bt connection");
    }

    public boolean deviceExists(BluetoothDevice device){
        for(int i = 0; i<deviceList.size(); i++){
            if(device.getAddress().equals(deviceList.get(i).getAddress())){
                return true;
            }
        }
        return false;
    }

    public void onDeviceFound(BluetoothDevice device){
        if(device.getName()!=null && device.getName().equalsIgnoreCase(DEVICE_NAME)){
            this.connectDevice(device);
        }
    }

    public class MyScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            //Do something with results
            BluetoothDevice device = adapter.getRemoteDevice(result.getDevice().getAddress());
            Log.d(TAG,device.getName() + " " + device.getAddress());
            if (!deviceExists(device)) {
                deviceList.add(device);

                onDeviceFound(device);
                //deviceDisplayList.add(device.getName() + " rssi: " + result.getRssi() + "\n");
                //deviceListViewAdapter.notifyDataSetChanged();
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



    public boolean hasAPDUResponse(){
        return apduResponseReceived;
    }


    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public void sendAPDUReadIC(){
        new Thread() {
            public void run() {
                apduResponseReceived = false;
                deviceApi.openICChannel();
                byte aid[] = new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x03, 0x10, 0x10};// VISA

                byte[] qpbocCmd = new byte[] { 0x00, (byte) 0xA4,
                        0x04, 0x00, aid[0], aid[1], aid[2],
                        aid[3], aid[4], aid[5], aid[6], 0x00};
                int fcp = deviceApi.transCommand(qpbocCmd,
                        qpbocCmd.length, apduResponse, 10);
                Log.d(TAG, "fcp returned: "+fcp);

                for (byte sfi = 1; sfi <= 31; sfi++){
                    for (byte rec = 1; rec <= 16; rec++){
                        byte[] cmd = new byte[] {0x00, (byte) 0xB2, rec, (byte) ((sfi << 3) | 4), 0x00};
                        byte[] resp = new byte[256];
                        int tlv = deviceApi.transCommand(cmd,
                                cmd.length, resp, 10);
                        String respStr = bytesToHex(resp);
                        Log.d(TAG, "resp: "+respStr);
                    }
                }
                deviceApi.closeICChannel();
                apduResponseReceived = true;
            }
        }.start();
    }

    public void connectDevice(BluetoothDevice device){
        if(!adapter.isEnabled()) {
            return;
        }

        if (connecting) {
            // Toast.makeText(context,
            //         UIMessage.connecting_device, Toast.LENGTH_SHORT)
            //         .show();
        } else {
            if (deviceConnected) {
                connecting = false;
            } else {
                stopScanning();
                if (device.getAddress() != null
                        && device.getAddress().length() > 0) {
                    connecting = true;
                    new Thread() {
                        public void run() {
                            Looper.prepare();
                            deviceApi.connectDevice(device.getAddress());
                            connecting = false;
                        }
                    }.start();
                } else {
                    connecting = false;
                }
            }
        }
    }

    public HashMap<String, String> customInput(String line1, String line2, boolean withDecimal, int timeout){
        byte inputType =  0x01;
        if(withDecimal){
            inputType = 0x00;
        }
        HashMap<String, String> amountData = deviceApi
                .deviceCustomInput(line1,
                        line2,
                        (byte) inputType, timeout); //0x01 => sin punto decimal | 0x00 punto decimal
        return amountData;
    }

    public HashMap<String, String> customInput(String line1, String line2, boolean withDecimal){
        int timeout = 20;
        return this.customInput(line1, line2, withDecimal, timeout);
    }

    public HashMap<String, String> customInput(String line1, String line2){
        boolean withDecimal = false;
        int timeout = 20;
        return this.customInput(line1, line2, withDecimal, timeout);
    }

    public void displayText(String text, int timeout){
        deviceApi.displayTextOnScreen(text, timeout);
    }

    public void clearDisplay(){
        deviceApi.clearScreen();
    }

    public void cancelConnection(){
        if (deviceConnected) {
            new Thread() {
                public void run() {
                    deviceApi.cancel();
                    connecting = false;
                }
            }.start();
        }
    }

    public String getEncPinBlock(String password){
        return deviceApi.getEncPinblock(password);
    }

    public void readGiftCard(int amount, CallbackContext callbackContext){
        new Thread() {
            public void run() {
                Log.d(TAG, "start read gift card thread");
                setName("swipeCardThread");
                SimpleDateFormat format = new SimpleDateFormat(
                        "yyyyMMddHHmmss", Locale.getDefault());
                String terminalTime = format.format(new Date());
                Log.e(TAG, "terminalTime:" + terminalTime);

                Map<String, String> result = deviceApi
                        .readCard(Integer.toString(amount*100),
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
                    cardInfo.setAmount(Integer.toString(amount));
                    cardInfo.setSwipeCardDate(terminalTime
                            .substring(0, 8));
                    cardInfo.setSwipeCardTime(terminalTime
                            .substring(8));
                    cardInfo.setValidThru(result
                            .get("expiryDate"));
                    cardInfo.setIcData55(result.get("icData"));
                    cardInfo.setPin(result.get("pin"));
                    Log.d(TAG, "ˢ����Ϣ�ѱ���");
                    deviceApi.confirmTransaction("Tarjeta leida exitosamente");
                    Log.d(TAG, "ENCODED PIN WITH FUNCTION: "+deviceApi.getEncPinblock("1234"));
                    callbackContext.success(result.get("cardNumber"));
                }else{
                    callbackContext.error("Error al leer la tarjeta")
                }

            }
        }.start();
    }

    public void readCard(){
        new Thread() {
            public void run() {
                // The parameter is��line1��line2��
                // inputType��0x00:have".",0x01:no"."���� time-out
                HashMap<String, String> amountData = customInput("CLERTICKET",
                        "Ingrese el monto");
                if (amountData != null
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
                            .readCard(amount,
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
                        cardInfo.setAmount(amount);
                        cardInfo.setSwipeCardDate(terminalTime
                                .substring(0, 8));
                        cardInfo.setSwipeCardTime(terminalTime
                                .substring(8));
                        cardInfo.setValidThru(result
                                .get("expiryDate"));
                        cardInfo.setIcData55(result.get("icData"));
                        cardInfo.setPin(result.get("pin"));
                        Log.d(TAG, "ˢ����Ϣ�ѱ���");
                        deviceApi.confirmTransaction("Se confirma la transa");
                    }
                }
            }
        }.start();
    }

    public void startScanning() {
        scanning = true;
        Log.d(TAG,"start scanning");

        deviceList.clear();
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
        deviceList.clear();
        if (scanner != null) {
            scanner.stopScan(myScanCallback);
        }
        scanning = false;
    }

    public void disconnect(){
        if (deviceConnected) {
            new Thread() {
                public void run() {
                    deviceApi.disconnectDevice();
                    deviceConnected = false;
                }
            }.start();
        }
    }

    public void updateWorkingKey(String tdk, String pik, String mak){
        if (deviceConnected) {
            new Thread() {
                public void run() {
                    deviceApi.updateWorkingKey(tdk, pik, mak);
                }
            }.start();
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }else if(action.equals("customInput")){
            cordova.getThreadPool().execute(new Runnable() {
                public void run(){
                    HashMap<string, string> resp = customInput(args.getString(0), args.getString(1));
                    if(resp!=null && resp.get("errorcode").equals("9000")){
                        callbackContext.success(resp.get("amount"));
                    }else{
                        callbackContext.error("Hubo un problema al pedir respuesta del usuario");
                    }
                }
            });
        }else if(action.equals("init")){
            init();
            callbackContext.success("hecho");
        }else if(action.equals("readGiftCard")){
            readGiftCard(args.getLong(0), callbackContext);
        }else if(action.equals("clearDisplay")){
            clearDisplay();
            callbackContext.success("hecho");
        }else if(action.equals("displayText")){
            displayText(args.getString(0));
            callbackContext.success("hecho");
        }else if(action.equals("isConnected")){
            callbackContext.success(isConnected());
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
