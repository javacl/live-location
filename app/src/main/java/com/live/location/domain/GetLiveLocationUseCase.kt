package com.live.location.domain

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class GetLiveLocationUseCase @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val settingsClient: SettingsClient
) {
    @SuppressLint("MissingPermission")
    operator fun invoke() = callbackFlow {

        val interval = 5000L

        val locationRequest = LocationRequest.create().apply {
            this.interval = interval
            this.fastestInterval = interval
        }

        val locationRequestBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                trySend(locationResult.lastLocation).isSuccess
            }
        }

        val task: Task<LocationSettingsResponse> =
            settingsClient.checkLocationSettings(locationRequestBuilder.build())

        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.getMainLooper()
            )
        }

        awaitClose { fusedLocationClient.removeLocationUpdates(locationCallBack) }
    }
}
