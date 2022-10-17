package com.live.location.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.live.location.service.LiveLocationService

@ExperimentalPermissionsApi
@Composable
fun MainScreen() {

    val context = LocalContext.current

    var needCheckLocationSetting by rememberSaveable { mutableStateOf(true) }

    var locationSettingIsEnable by rememberSaveable { mutableStateOf(false) }

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val backgroundPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            needCheckLocationSetting = true
        }
    }

    if (needCheckLocationSetting) {
        checkLocationSetting(
            context = context,
            onDisabled = { intentSenderRequest ->
                settingResultRequest.launch(intentSenderRequest)
            },
            onEnabled = { locationSettingIsEnable = true }
        )
        needCheckLocationSetting = false
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (locationPermissionState.allPermissionsGranted
            && backgroundPermissionState.status.isGranted
            && locationSettingIsEnable
        ) {

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
                if (locationSettingIsEnable) {
                    if (locationPermissionState.allPermissionsGranted) {
                        backgroundPermissionState.launchPermissionRequest()
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                } else {
                    needCheckLocationSetting = true
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

fun checkLocationSetting(
    context: Context,
    onDisabled: (IntentSenderRequest) -> Unit,
    onEnabled: () -> Unit
) {
    val locationRequest = LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 1000
    }

    val client: SettingsClient = LocationServices.getSettingsClient(context)
    val builder: LocationSettingsRequest.Builder = LocationSettingsRequest
        .Builder()
        .addLocationRequest(locationRequest)

    val gpsSettingTask: Task<LocationSettingsResponse> =
        client.checkLocationSettings(builder.build())

    gpsSettingTask.addOnSuccessListener { onEnabled() }
    gpsSettingTask.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest
                    .Builder(exception.resolution)
                    .build()
                onDisabled(intentSenderRequest)
            } catch (sendEx: IntentSender.SendIntentException) {
            }
        }
    }
}
