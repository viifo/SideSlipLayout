package com.viifo.sidesliplayout.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.viffo.sidesliplayout.demo.R
import com.viffo.sidesliplayout.SideSlipLayout
import com.viffo.sidesliplayout.SideSlipLayoutCallback

class DemoActivity : AppCompatActivity() {

    lateinit var btnOpen: Button
    lateinit var btnClose: Button
    lateinit var sideSlipLayout: SideSlipLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        sideSlipLayout = findViewById(R.id.side_slip_layout)
        btnOpen = findViewById(R.id.btn_open)
        btnClose = findViewById(R.id.btn_close)

        btnOpen.setOnClickListener {
            if (!sideSlipLayout.isOpening()) {
                sideSlipLayout.open()
            }
        }
        btnClose.setOnClickListener {
            if (sideSlipLayout.isOpening()) {
                sideSlipLayout.close()
            }
        }
        sideSlipLayout.statusCallback = object: SideSlipLayoutCallback {
            override fun opened(layout: SideSlipLayout) {
                Log.e("tag", "opened...")
            }

            override fun closed(layout: SideSlipLayout) {
                Log.e("tag", "closed...")
            }

            override fun dragging(layout: SideSlipLayout, left: Int) {
                Log.e("tag", "dragging: left = $left")
            }
        }
    }

}