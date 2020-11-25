package world.shanya.serialportutils

import android.content.Context
import android.text.TextUtils

/**
 * create by ZP
 * on 2020/11/25
 * SharePreferences管理类
 */

const val SPName = "SerialPortUtilSP"
const val DEVICE_NAME = "SP_DEVICE_NAME"
const val DEVICE_ADDRESS = "SP_DEVICE_ADDRESS"

object SPUtils {
    /**
     * 存SP数据(String)
     */
    fun putString(context: Context, device: Device) {
        val sp = context.getSharedPreferences(SPName, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(DEVICE_NAME, device.name)
        editor.putString(DEVICE_ADDRESS, device.address)
        editor.apply()
    }

    /**
     * 获取SP数据(String)
     */
    private fun getString(context: Context, key: String): String? {
        val sp = context.getSharedPreferences(SPName, Context.MODE_PRIVATE)
        return sp.getString(key, "")
    }

    /**
     * 清除数据
     */
    fun clearSp(context: Context) {
        val sp = context.getSharedPreferences(SPName, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.clear()
        editor.apply()
    }

    /**
     * 获取本地存入的设备名称
     */
    fun getSPDeviceName(context: Context): String? {
        return if (TextUtils.isEmpty(getString(context, DEVICE_NAME))) "" else getString(
            context,
            DEVICE_NAME
        )
    }

    /**
     * 获取本地存入的设备地址
     */
    fun getSPDeviceAddress(context: Context): String? {
        return if (TextUtils.isEmpty(getString(context, DEVICE_ADDRESS))) "" else getString(
            context,
            DEVICE_ADDRESS
        )
    }
}