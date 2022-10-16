package com.live.location.main

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.live.location.service.LiveLocationService

@ExperimentalPermissionsApi
@Composable
fun MainScreen() {

    val context = LocalContext.current

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val backgroundPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (locationPermissionState.allPermissionsGranted && backgroundPermissionState.status.isGranted) {

            Text(
                text = "🌍 Location Tracker Background Service.!!",
                fontSize = MaterialTheme.typography.subtitle1.fontSize,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                if (LiveLocationService.liveLocationServiceIsRunning) {
                    return@Button
                }
                Intent(context.applicationContext, LiveLocationService::class.java).apply {
                    action = LiveLocationService.ACTION_START
                    context.startService(this)
                }
            }) {
                Text(
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                    text = if (LiveLocationService.liveLocationServiceIsRunning) "Service Started" else "Start 😇"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (!LiveLocationService.liveLocationServiceIsRunning) {
                    return@Button
                }
                Intent(context.applicationContext, LiveLocationService::class.java).apply {
                    action = LiveLocationService.ACTION_STOP
                    context.startService(this)
                }
            }) {
                Text(fontSize = MaterialTheme.typography.subtitle1.fontSize, text = "Stop 😅")
            }
        } else {
            Button(onClick = {
                if (locationPermissionState.allPermissionsGranted) {
                    backgroundPermissionState.launchPermissionRequest()
                } else {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
            }) {
                Text(
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                    text = "Grant Permission"
                )
            }
        }
    }
}
