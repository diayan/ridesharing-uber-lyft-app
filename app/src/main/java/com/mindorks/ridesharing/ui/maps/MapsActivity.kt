package com.mindorks.ridesharing.ui.maps

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.MapsUtils
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsActivity"
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var googleMap: GoogleMap
    private val zoomLevel = 15f
    private lateinit var currentLatLng: LatLng
    private lateinit var presenter: MapsPresenter
    private lateinit var locationCallback: LocationCallback
    private val nearbyCabMarkerList = arrayListOf<Marker>()
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        presenter = MapsPresenter(NetworkService())
        presenter.onAttach(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isPermissionGranted(this) -> {
                            setupCurrentLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this, getString(R.string.location_permission_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isPermissionGranted(this) -> {
                when {
                    PermissionUtils.isPermissionGranted(this) -> {
                        setupCurrentLocationListener()
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                }
            }
            else -> {
                PermissionUtils.requestFineLocationPermission(
                    this,
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    private fun moveCamera(latLng: LatLng) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun enableMyLocationOnMap() {
        googleMap.setPadding(0, ViewUtils.dpToPx(48f), 0, 0)
        googleMap.isMyLocationEnabled = true
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(zoomLevel).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(MapsUtils.getCabBitmap(this)))
        )
    }

    private fun setupCurrentLocationListener() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    enableMyLocationOnMap()
                    moveCamera(currentLatLng)
                    animateCamera(currentLatLng)
                    presenter.requestNearbyCabs(currentLatLng)
                }
            }
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    override fun showNearbyCabs(latlngList: List<LatLng>) {
        nearbyCabMarkerList.clear()
        for (latlng in latlngList) {
            val nearbyCab = addCarMarkerAndGet(latlng)
            nearbyCabMarkerList.add(nearbyCab)
        }
    }

    override fun informCabBooked() {
        TODO("Not yet implemented")
    }

    override fun showPath(latlngList: List<LatLng>) {
        TODO("Not yet implemented")
    }

    override fun updateCabLocation(latLng: LatLng) {
        TODO("Not yet implemented")
    }

    override fun informCabIsArriving() {
        TODO("Not yet implemented")
    }

    override fun informCabArrived() {
        TODO("Not yet implemented")
    }

    override fun informTripStart() {
        TODO("Not yet implemented")
    }

    override fun informTripeEnd() {
        TODO("Not yet implemented")
    }

    override fun showRoutesNotAvailable() {
        TODO("Not yet implemented")
    }

    override fun showDirectionApiFailedError(error: String) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        presenter.onDetach()
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}
