package com.live.location.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.live.location.service.LiveLocationService
import com.live.location.theme.LiveLocationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val coroutineScope = rememberCoroutineScope()

            LiveLocationTheme {

                val locationPermissionState = rememberMultiplePermissionsState(
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )

                val backgroundPermissionState = rememberPermissionState(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )

                if (locationPermissionState.allPermissionsGranted && backgroundPermissionState.status.isGranted) {
                    if (!LiveLocationService.liveLocationServiceIsRunning) {
                        Intent(applicationContext, LiveLocationService::class.java).apply {
                            action = LiveLocationService.ACTION_START
                            startService(this)
                        }
                    }
                } else {
                    coroutineScope.launch {
                        locationPermissionState.launchMultiplePermissionRequest()
                        backgroundPermissionState.launchPermissionRequest()
                    }
                }
            }
        }
    }
}
