package com.dantsu.thermalprinter;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.data.Comment;
import com.google.gson.Gson;


import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {


    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("https://www.ttliveprint.com/");
        } catch (URISyntaxException e) {}
    }
    private final List<Comment> comments = new ArrayList<>();
    private RecyclerView recyclerView;

    private ItemAdapter adapter;

    private EditText editText;

    private Button btnSelectDevice;

    private  Button btnScrollDown;
    private Button btnConnectServer;

    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)this.findViewById(R.id.edtUniqueId);
        btnSelectDevice = (Button) this.findViewById(R.id.button_bluetooth_browse);
        btnSelectDevice.setOnClickListener(view -> browseBluetoothDevice());
        btnConnectServer = (Button) findViewById(R.id.connect_to_server);
        btnConnectServer.setOnClickListener(view -> connectToServer());
        btnScrollDown = (Button)this.findViewById(R.id.btn_scroll_down);
        btnScrollDown.setOnClickListener(view -> scrollDown());
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this));
        adapter = new ItemAdapter(comments);
        adapter.setOnItemClickListener(this::printBluetooth);
        recyclerView.setAdapter(adapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);

    }
    private void connectToServer(){
        progressDialog.show();
        hideKeyboard(this);
        String uniqueId = "huyentuixach";
        if(!editText.getText().toString().equals("")){
            uniqueId = editText.getText().toString();
        }

        mSocket.connect();
        mSocket.on("connect",onNewMessage);
        mSocket.emit("setUniqueId",uniqueId);
        mSocket.on("chat",onChat);
        mSocket.on("disconnect", onDisconnected);
        mSocket.on("streamEnd", onStreamEnded);
    }
    Emitter.Listener onChat = args -> runOnUiThread(() -> {
        progressDialog.hide();
        try{
            editText.setText("");
            editText.setVisibility(View.GONE);
            btnConnectServer.setClickable(false);
            btnSelectDevice.setClickable(false);
            Comment comment = new Gson().fromJson(args[0].toString(),Comment.class);
            comments.add(comment);
            adapter.notifyItemInserted(comments.size()-1);
            recyclerView.smoothScrollToPosition(comments.size()-1);
        } catch (Exception e){
            e.printStackTrace();
        }
    });

    Emitter.Listener onStreamEnded = args -> runOnUiThread(()->{
        try{
            mSocket.emit("disconnect", "Hello" );
            mSocket.disconnect();
            mSocket.close();
            btnConnectServer.setClickable(true);
            btnSelectDevice.setClickable(true);
            editText.setVisibility(View.VISIBLE);
            comments.clear();
            adapter.notifyDataSetChanged();
        } catch (Exception e){
            e.printStackTrace();
        }
    });


    private Emitter.Listener onNewMessage = args -> runOnUiThread(() -> Log.d("Main Activity", Arrays.toString(args)));


    private Emitter.Listener onDisconnected = args -> runOnUiThread(()->{
        btnSelectDevice.setClickable(true);
        btnConnectServer.setClickable(true);
        editText.setVisibility(View.VISIBLE);
        mSocket.off("new message", onNewMessage);
        mSocket.close();
    });


    private void scrollDown(){

    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("new message", onNewMessage);
    }
    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(() -> {
            final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

            if (bluetoothDevicesList != null) {
                final String[] items = new String[bluetoothDevicesList.length + 1];
                items[0] = "Default printer";
                int i = 0;
                for (BluetoothConnection device : bluetoothDevicesList) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    items[++i] = device.getDevice().getName();

                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Bluetooth printer selection");
                alertDialog.setItems(
                    items,
                    (dialogInterface, i1) -> {
                        int index = i1 - 1;
                        if (index == -1) {
                            selectedDevice = null;
                        } else {
                            selectedDevice = bluetoothDevicesList[index];
                        }
                        Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                        button.setText(items[i1]);
                    }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);
                alert.show();
            }
        });

    }

    public void printBluetooth(int position) {
        Toast.makeText(this,"item click:"+comments.get(position).nickname, Toast.LENGTH_LONG).show();
        this.checkBluetoothPermissions(() -> new AsyncBluetoothEscPosPrint(
            this,
            new AsyncEscPosPrint.OnPrintFinished() {
                @Override
                public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                    Log.e("MainActivity", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                }

                @Override
                public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                    Log.i("MainActivity", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                }
            }
        )
            .execute(this.getAsyncEscPosPrinter(selectedDevice, position)));
    }


    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection, int position) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 58f, 47);
        return printer.addTextToPrint(
                    "[L]\n" +
                    "[L]<u>HUYEN TUI XACH</u>\n" +
                    "[L]\n" +
                    "[L]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[L]======================\n" +
                    "[L]<b>Comment:</b>"+ comments.get(position).comment+"\n" +
                    "[L]<b>Nick name:</b>"+comments.get(position).nickname+"\n" +
                    "[L]<b>Unique Id:</b>"+comments.get(position).uniqueId+"\n" +
                    "[L]======================\n" +
                    "[L]<qrcode size='20'>"+ "https://www.tiktok.com/@"+comments.get(position).uniqueId+"</qrcode>\n"+ "[L]\n" + "[L]\n"
        );
    }
}
