package com.live.location.domain

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class GetLiveLocationUseCase @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val settingsClient: SettingsClient
) {
    @SuppressLint("MissingPermission")
    operator fun invoke() = callbackFlow {

        val locationRequest = LocationRequest.create().apply {
            this.interval = 5000
            this.fastestInterval = 1000
            this.priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val locationRequestBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                trySend(locationResult.lastLocation).isSuccess
            }
        }

        val task = settingsClient.checkLocationSettings(locationRequestBuilder.build())

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
