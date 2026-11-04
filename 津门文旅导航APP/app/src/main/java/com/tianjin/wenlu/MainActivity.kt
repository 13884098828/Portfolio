package com.tianjin.wenlu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tianjin.wenlu.ui.WenluApp
import com.tianjin.wenlu.ui.theme.WenluTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WenluTheme {
                WenluApp()
            }
        }
    }
}
