package com.glyphpings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.glyphpings.ping.PingCenter
import com.glyphpings.ui.PingsApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PingCenter.init(this)
        enableEdgeToEdge()
        setContent { PingsApp() }
    }
}
