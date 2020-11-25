package world.shanya.serialportutils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.os.IBinder
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.text.method.ReplacementTransformationMethod
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.lang.Exception

/**
 * 更多的关于SerialPort的API
 */
class SerialPort private constructor(private val activity: FragmentActivity): SerialPortCallback(){

    /**
     * 已配对设备列表
     */
    val pairedDevicesList = ArrayList<Device>()

    /**
     * 未配对设备列表
     */
    val unPairedDevicesList = ArrayList<Device>()

    /**
     * 输入框
     */
    private var editText: EditText ?= null

    /**
     * 伴生对象
     */
    companion object{

        /**
         * 接收消息为字符类型
         */
        const val READ_STRING = 1

        /**
         * 发送消息为字符类型
         */
        const val SEND_STRING = 2

        /**
         * 接收消息为十六进制类型
         */
        const val READ_HEX = 3

        /**
         * 发送消息为十六进制类型
         */
        const val SEND_HEX = 4

        /**
         * SPP服务UUID号
         */
        private const val UUID = "00001101-0000-1000-8000-00805F9B34FB"

        /**
         * 蓝牙设配器
         */
        private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        /**
         * 蓝牙Socket
         */
        internal var bluetoothSocket:BluetoothSocket ?= null

        /**
         * 接收用InputStream
         */
        internal var inputStream: InputStream ?= null

        /**
         * 数据接收类型，缺省值为 READ_STRING
         */
        internal var readDataType = READ_STRING

        /**
         * 数据发送类型，缺省值为 SEND_STRING
         */
        internal var sendDataType = SEND_STRING

        /**
         * 获取SerialPort实例，SingleTon
         */
        private var instance: SerialPort ?= null
        fun getInstance(activity: FragmentActivity): SerialPort{
            val temp = instance
            if (temp != null){
                return temp
            }
            return synchronized(this) {
                val temp2 = instance
                if (temp2 != null) {
                    temp2
                } else {
                    val created = SerialPort(activity)
                    instance = created
                    created
                }
            }
        }
    }

    /**
     * 广播接收器
     * 用于处理找到新设备、搜索完成、设备断开连接
     */
    private val serialPortReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    //find device
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null){
                            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                                unPairedDevicesList.add(Device(device.name?:"unknown", device.address))
                                findUnPairedDeviceCallback?.invoke()
                            }
                        }
                    }

                    //discover finished
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanStatusCallback?.invoke(false)
                    }

                    //disconnected
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        bluetoothSocket = null
                        Device(device?.name?:"unknown",device?.address?:"").also {
                            connectedCallback?.invoke(false,it)
                        }
                        Toast.makeText(context,
                            "${device?.name}  ${context?.getString(R.string.disconnect)}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    /**
     * 实例初始化
     */
    init {
        getInvisibleFragment().registerReceiverNow(serialPortReceiver)

        val pairedDevices:Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()){
            for (device in pairedDevices){
                pairedDevicesList.add(Device(device.name,device.address))
            }
        }
    }

    /**
     * 设置接收数据的类型
     * 可选参数
     * @READ_STRING {字符类型}
     * @READ_HEX   {十六进制}
     */
    fun setReceivedDataType(type: Int) {
        readDataType = when (type) {
            READ_HEX ->
                READ_HEX
            else ->
                READ_STRING
        }
    }

    /**
     * EditText的监测输入，用于在十六进制发送模式下输入框每输入两个字符自动追加一个空格
     */
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {

        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        @SuppressLint("SetTextI18n")
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (count == 1){
                if ((s!!.length + 1) % 3 == 0){
                    editText?.setText("$s ")
                    editText?.setSelection(s.length + 1)
                }
            }else if (count == 0){
                if (s!!.isNotEmpty() && s.length % 3 == 0){
                    editText?.setText(s.subSequence(0,s.length - 1))
                    editText?.setSelection(s.length - 1)
                }
            }
        }

    }

    /**
     * 设置发送数据的类型
     * 可选参数
     * @SEND_STRING {字符类型}
     * @SEND_HEX    {十六进制}
     */
    fun setSendDataType(type: Int) {
        when (type) {
            SEND_HEX ->
                sendDataType = SEND_HEX
            else ->{
                sendDataType = SEND_STRING
                editText?.keyListener = DigitsKeyListener.getInstance("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
                editText?.removeTextChangedListener(textWatcher)
            }

        }
    }

    /**
     * 为发送EditText添加十六进制输入限制
     */
    fun setEditTextHexLimit(editText: EditText){
        this.editText = editText
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.keyListener = DigitsKeyListener.getInstance("abcdefABCDEF0123456789")
        editText.transformationMethod = object : ReplacementTransformationMethod() {
            override fun getOriginal(): CharArray {
                return charArrayOf('a','b','c','d','e','f')
            }

            override fun getReplacement(): CharArray {
                return charArrayOf('A','B','C','D','E','F')
            }

        }
        editText.addTextChangedListener(textWatcher)
    }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private fun getInvisibleFragment(): InvisibleFragment {
        val fragmentManager = activity.supportFragmentManager
        val existedFragment = fragmentManager.findFragmentByTag(TAG)
        return if (existedFragment != null) {
            existedFragment as InvisibleFragment
        } else {
            val invisibleFragment = InvisibleFragment()
            fragmentManager.beginTransaction().add(invisibleFragment, TAG).commitNow()
            invisibleFragment
        }
    }

    /**
     * 打开搜索页面
     */
    fun openSearchPage(){
        getInvisibleFragment().startActivity(Intent(activity,SearchActivity::class.java))
    }

    /**
     * 开始搜索
     */
    fun doDiscovery(): Boolean {
        if (!PermissionX.isGranted(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            PermissionX.init(activity)
                .permissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList ->
                    val message = "需要您同意位置权限用于搜索设备，否则搜索功能将不可用。"
                    scope.showRequestReasonDialog(deniedList, message, "确定", "取消")
                }
                .onForwardToSettings { scope, deniedList ->
                    val message = "需要您去设置页面同意位置权限用于搜索设备，否则搜索功能将不可用。"
                    scope.showForwardToSettingsDialog(deniedList, message, "确定", "取消")
                }
                .request { allGranted, grantedList, deniedList ->
                    @Suppress("ControlFlowWithEmptyBody")
                    if (allGranted) {

                    } else {
                        Toast.makeText(activity, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                    }
                }
            return false
        } else {
            if (!bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
                return false
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            unPairedDevicesList.clear()

            scanStatusCallback?.invoke(true)

            bluetoothAdapter.startDiscovery()

            return true
        }
    }

    /**
     * 取消搜索
     */
    internal fun cancelDiscover(){
        bluetoothAdapter.cancelDiscovery()
        scanStatusCallback?.invoke(false)
    }

    /**
     * 接收数据ServiceConnection
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            (service as SerialPortService.SerialPortServiceBinder)
                .serialPortService.receiveLiveData.observe(
                    activity, Observer {
                        receivedDataCallback?.invoke(it)
                    }
                )

        }
    }

    /**
     * 连接设备
     */
    internal fun connectDevice(device: Device){

        Thread{
            val address = device.address
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))
                bluetoothSocket?.connect()

                connectedCallback?.invoke(true, device)
                _connectedCallback?.invoke()
                __connectedCallback?.invoke(true, device)

                SPUtils.putString(activity,device)

                MainScope().launch {
                    Toast.makeText(
                        activity,
                        "${bluetoothDevice.name}  ${activity.getString(R.string.connect_success)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                inputStream = bluetoothSocket?.inputStream

                val intent = Intent(activity, SerialPortService::class.java)

                activity.bindService(intent,serviceConnection,Context.BIND_AUTO_CREATE)

            }catch (e: IOException){
                //设置连接状态为 false ，并弹出连接失败的 Toast 提示
                _connectedCallback?.invoke()
                __connectedCallback?.invoke(false, device)
                MainScope().launch {
                    Toast.makeText(activity,activity.getString(R.string.connection_failed),Toast.LENGTH_SHORT).show()
                }
                //关闭蓝牙 Socket
                try {
                    bluetoothSocket?.close()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }.start()

    }

    /**
     * 断开连接
     */
    fun disconnect(){
        try {
            activity.unbindService(serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 字符串转换成十六进制
     * 参数：
     * str：端转换的字符串
     * 返回值：十六进制数组
     */
    private fun stringToHex(str: String): ArrayList<Byte>? {
        val chars = "0123456789ABCDEF".toCharArray()
        val stingTemp = str.replace(" ","")
        val bs = stingTemp.toCharArray()
        var bit = 0
        var i = 0
        val intArray = ArrayList<Byte>()
        if (stingTemp.length and 0x01 != 0){
            MainScope().launch {
                Toast.makeText(activity,"请输入的十六进制数据保持两位，不足前面补0",Toast.LENGTH_SHORT).show()
            }
            throw  RuntimeException("字符个数不是偶数")
        }
        while (i < bs.size) {
            for (j in chars.indices) {
                if (bs[i] == chars[j]) {
                    bit += (j * 16)
                }
                if (bs[i + 1] == chars[j]) {
                    bit += j
                }
            }
            intArray.add(bit.toByte())
            i += 2
            bit = 0
        }
        return intArray
    }

    /**
     * 发送数据内部异线程调用
     */
    private fun send(data : String){
        try {
            val outputStream = bluetoothSocket?.outputStream
            var n = 0
            val bos: ByteArray = if (sendDataType == SEND_STRING) {
                data.toByteArray()
            }else{
                stringToHex(data)?.toList()!!.toByteArray()
            }

            for (bo in bos) {
                if (bo.toInt() == 0x0a) {
                    n++
                }
            }
            val bosNew = ByteArray(bos.size + n)
            n = 0
            for (bo in bos) {
                if (bo.toInt() == 0x0a) {
                    bosNew[n++] = 0x0d
                    bosNew[n] = 0x0a
                } else {
                    bosNew[n] = bo
                }
                n++
            }
            outputStream?.write(bosNew)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    /**
     * 对外公开的法术数据接口函数
     */
    fun sendData(data:String){
        if (!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
            return
        }
        if (bluetoothSocket == null) {
            MainScope().launch {
                Toast.makeText(activity,activity.getString(R.string.connect_device),Toast.LENGTH_SHORT).show()
            }
            return
        }

        Thread{
            send(data)
        }.start()
    }


    /**
     * 自动连接
     */
    internal fun autoConnection(context: Context): Boolean {
        var isConnect = false
        val name = SPUtils.getSPDeviceName(context)
        val address = SPUtils.getSPDeviceAddress(context)
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(address)) {
            connectDevice(Device(name.toString(), address.toString()))
            __getConnectedStatus { status, device ->
                isConnect = status
            }
        } else {
            isConnect = false
        }

        return isConnect
    }

}