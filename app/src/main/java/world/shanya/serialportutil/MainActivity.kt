package world.shanya.serialportutil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import world.shanya.serialportutils.SerialPort


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        serialPort.setReceivedDataType(SerialPort.READ_HEX)
        serialPort.setSendDataType(SerialPort.SEND_HEX)
        serialPort.setEditTextHexLimit(editText)

        val isConnect = serialPort.autoConnection(this)

        button.setOnClickListener {
            serialPort.openSearchPage()
        }

        button2.setOnClickListener {
            Log.d(TAG, "onCreate: ${editText.text}")
            serialPort.sendData(editText.text.toString())
        }

        button3.setOnClickListener {
            serialPort.setSendDataType(SerialPort.SEND_STRING)
        }

        serialPort.getReceivedData {
            Log.d(TAG, "received Data: $it")
        }

        serialPort.getConnectedStatus { status, device ->
            Log.d(TAG, "status --> $status \nname --> ${device.name}")
        }



    }
}