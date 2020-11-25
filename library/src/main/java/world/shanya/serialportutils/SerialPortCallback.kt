package world.shanya.serialportutils

/**
 * 扫描状态回调
 * 参数：
 * status：扫描状态  true：正在扫描   false：扫描结束
 */
typealias ScanStatusCallback = (status: Boolean) -> Unit

/**
 * 外部使用连接状态回调
 * 参数：
 * status：连接状态  true：已连接    false：未连接
 * device：连接设备（包含设备名和地址）
 */
typealias ConnectedCallback = (status: Boolean,device: Device) -> Unit

/**
 * 内部使用连接状态回调
 * 参数：无
 */
typealias _ConnectedCallback = () -> Unit

/**
 * 接收消息回调
 * 参数：
 * data：收到的消息内容
 */
typealias ReceivedDataCallback = (data: String) -> Unit

/**
 * 找到未配对设置回调
 * 参数：无
 */
typealias FindUnPairedDeviceCallback = () -> Unit

/**
 * SerialPortUtil的所有回调及其接口函数
 */
open class SerialPortCallback {

    /**
     * 扫描状态的回调函数，仅限内部使用
     */
    internal var scanStatusCallback: ScanStatusCallback ?= null
    internal fun _getScanStatus(scanStatusCallback: ScanStatusCallback){
        this.scanStatusCallback = scanStatusCallback
    }

    /**
     * 连接状态的回调函数，分为内部外部两个版本，名字带有"_"的是内部版本
     */
    internal var connectedCallback: ConnectedCallback ?= null
    internal var _connectedCallback: _ConnectedCallback ?= null
    internal var __connectedCallback: ConnectedCallback ?= null
    internal fun _getConnectedStatus(connectedCallback: _ConnectedCallback) {
        this._connectedCallback = connectedCallback
    }
    fun getConnectedStatus(connectedCallback: ConnectedCallback) {
        this.connectedCallback = connectedCallback
    }
    internal fun __getConnectedStatus(connectedCallback: ConnectedCallback) {
        this.__connectedCallback = connectedCallback
    }

    /**
     * 接收消息的回调函数
     */
    internal var receivedDataCallback: ReceivedDataCallback ?= null
    fun getReceivedData(receivedDataCallback: ReceivedDataCallback) {
        this.receivedDataCallback = receivedDataCallback
    }

    /**
     * 发现新的未配对设备的回调函数，仅限内部使用
     */
    internal var findUnPairedDeviceCallback: FindUnPairedDeviceCallback ?= null
    internal fun findUnPairedDevice(findUnPairedDeviceCallback: FindUnPairedDeviceCallback) {
        this.findUnPairedDeviceCallback = findUnPairedDeviceCallback
    }

}