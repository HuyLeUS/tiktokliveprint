package com.dantsu.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint
import com.dantsu.thermalprinter.async.AsyncEscPosPrint.OnPrintFinished
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter
import com.dantsu.thermalprinter.data.Comment
import com.dantsu.thermalprinter.viewmodel.MainViewModel
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date

class MainActivity : AppCompatActivity() {
    private var mSocket: Socket? = null

    init {
        try {
            mSocket = IO.socket("https://www.ttliveprint.com/")
        } catch (ignored: URISyntaxException) {
        }
    }

    var mainViewModel = MainViewModel()
    private val comments: MutableList<Comment> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var adapter: ItemAdapter? = null
    private var editText: EditText? = null
    private lateinit var btnSelectDevice: Button
    private lateinit var btnScrollDown: ImageButton
    private lateinit var btnConnectServer :Button
    private var linearLayoutManager: CustomLinearLayoutManager? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.edtUniqueId)
        btnSelectDevice = findViewById(R.id.button_bluetooth_browse)
        btnScrollDown = findViewById(R.id.btn_scroll_down)
        btnConnectServer = findViewById(R.id.connect_to_server)
        btnSelectDevice.setOnClickListener { browseBluetoothDevice() }
        btnConnectServer.setOnClickListener { connectToServer() }
        btnScrollDown.setOnClickListener { scrollDown() }
        recyclerView = findViewById(R.id.recyclerView)
        linearLayoutManager = CustomLinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        adapter = ItemAdapter(comments)
        adapter!!.setOnItemClickListener(object:ItemAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                printBluetooth(position)
            }
        })
        recyclerView.adapter = adapter
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Connecting...")
        progressDialog!!.setCancelable(false)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0) {
                    // User is scrolling up
                    mainViewModel.setIsOnBottom(false)
                    btnScrollDown.visibility = View.VISIBLE // Show the button
                } else {
                    if (linearLayoutManager!!.findLastVisibleItemPosition() == adapter!!.itemCount - 1) {
                        mainViewModel.setIsOnBottom(true)
                        btnScrollDown.visibility = View.INVISIBLE
                    }
                }
            }
        })
        mainViewModel.isOnBottom.observe(this
        ) {
            if (it == true) {
                btnScrollDown.visibility = View.INVISIBLE
            } else {
                btnScrollDown.visibility = View.VISIBLE
            }
        }
    }

    private fun connectToServer() {
        progressDialog!!.show()
        hideKeyboard(this)
        var uniqueId = "huyentuixach"
        if (editText!!.text.toString() != "") {
            uniqueId = editText!!.text.toString()
        }
        mSocket!!.connect()
        mSocket!!.on("connect", onNewMessage)
        mSocket!!.emit("setUniqueId", uniqueId)
        mSocket!!.on("chat", onChat)
        mSocket!!.on("disconnect", onDisconnected)
        mSocket!!.on("streamEnd", onStreamEnded)
    }

    private var onChat = Emitter.Listener { args: Array<Any> ->
        runOnUiThread {
            progressDialog!!.hide()
            try {
                editText!!.setText("")
                editText!!.visibility = View.GONE
                btnConnectServer.isClickable = false
                btnSelectDevice.isClickable = false
                val comment = Gson().fromJson(args[0].toString(), Comment::class.java)
                comments.add(comment)
                adapter!!.notifyItemInserted(comments.size - 1)
                if (java.lang.Boolean.TRUE == mainViewModel.isOnBottom.value) {
                    recyclerView.smoothScrollToPosition(comments.size - 1)
                    btnScrollDown.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    var onStreamEnded = Emitter.Listener { _: Array<Any?>? ->
        runOnUiThread {
            try {
                mSocket!!.emit("disconnect", "Hello")
                mSocket!!.disconnect()
                mSocket!!.close()
                btnConnectServer.isClickable = true
                btnSelectDevice.isClickable = true
                editText!!.visibility = View.VISIBLE
                comments.clear()
                adapter!!.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private val onNewMessage = Emitter.Listener { args: Array<Any?>? ->
        runOnUiThread {
            Log.d(
                "Main Activity",
                Arrays.toString(args)
            )
        }
    }
    private val onDisconnected = Emitter.Listener { _: Array<Any?>? ->
        runOnUiThread {
            btnSelectDevice.isClickable = true
            btnConnectServer.isClickable = true
            editText!!.visibility = View.VISIBLE
            mSocket!!.off("new message", onNewMessage)
            mSocket!!.close()
        }
    }

    private fun scrollDown() {
        mainViewModel.onScrollDown()
        recyclerView.smoothScrollToPosition(comments.size - 1)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mSocket!!.disconnect()
        mSocket!!.off("new message", onNewMessage)
    }

    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/
    interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    var onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted? = null
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_BLUETOOTH, PERMISSION_BLUETOOTH_ADMIN, PERMISSION_BLUETOOTH_CONNECT, PERMISSION_BLUETOOTH_SCAN -> checkBluetoothPermissions(
                    onBluetoothPermissionsGranted
                )
            }
        }
    }

    private fun checkBluetoothPermissions(onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted?) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                PERMISSION_BLUETOOTH
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            this.onBluetoothPermissionsGranted!!.onPermissionsGranted()
        }
    }

    private var selectedDevice: BluetoothConnection? = null
    private fun browseBluetoothDevice() {
        checkBluetoothPermissions(object:OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                val bluetoothDevicesList = BluetoothPrintersConnections().list
                if (bluetoothDevicesList != null) {
                    val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                    items[0] = "Default printer"
                    for ((i, device) in bluetoothDevicesList.withIndex()) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        items[i + 1] = device.device.name
                    }
                    val alertDialog = AlertDialog.Builder(this@MainActivity)
                    alertDialog.setTitle("Bluetooth printer selection")
                    alertDialog.setItems(
                        items
                    ) { _: DialogInterface?, i1: Int ->
                        val index = i1 - 1
                        selectedDevice = if (index == -1) {
                            null
                        } else {
                            bluetoothDevicesList[index]
                        }
                        val button = findViewById<Button>(R.id.button_bluetooth_browse)
                        button.text = items[i1]
                    }
                    val alert = alertDialog.create()
                    alert.setCanceledOnTouchOutside(false)
                    alert.show()
                }
            }
        })
    }

    private fun printBluetooth(position: Int) {
        checkBluetoothPermissions(object:OnBluetoothPermissionsGranted{
            override fun onPermissionsGranted() {
                    AsyncBluetoothEscPosPrint(
                        this@MainActivity,
                        object : OnPrintFinished() {
                            override fun onError(
                                asyncEscPosPrinter: AsyncEscPosPrinter,
                                codeException: Int
                            ) {
                                Log.e(
                                    "MainActivity",
                                    "AsyncEscPosPrint.OnPrintFinished : An error occurred !"
                                )
                            }

                            override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                                Log.i(
                                    "MainActivity",
                                    "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                                )
                            }
                        }
                    )
                        .execute(getAsyncEscPosPrinter(selectedDevice, position))
            }

        })
    }

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(
        printerConnection: DeviceConnection?,
        position: Int
    ): AsyncEscPosPrinter {
        val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
        val printer = AsyncEscPosPrinter(printerConnection, 203, 58f, 47)
        return printer.addTextToPrint(
            """
                [L]
                [L]<u>HUYEN TUI XACH</u>
                [L]
                [L]<u type='double'>${format.format(Date())}</u>
                [L]======================
                [L]<b>Comment:</b>${comments[position].comment}
                [L]<b>Nick name:</b>${comments[position].nickname}
                [L]<b>Unique Id:</b>${comments[position].uniqueId}
                [L]======================
                [L]<qrcode size='20'>https://www.tiktok.com/@${comments[position].uniqueId}</qrcode>
                [L]
                [L]
                
                """.trimIndent()
        )
    }

    companion object {
        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        const val PERMISSION_BLUETOOTH = 1
        const val PERMISSION_BLUETOOTH_ADMIN = 2
        const val PERMISSION_BLUETOOTH_CONNECT = 3
        const val PERMISSION_BLUETOOTH_SCAN = 4
    }
}