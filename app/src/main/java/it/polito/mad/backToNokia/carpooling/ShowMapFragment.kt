package it.polito.mad.backToNokia.carpooling

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.content.ContextCompat
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class ShowMapFragment : Fragment(R.layout.fragment_show_map) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments?.let { ShowMapFragmentArgs.fromBundle(it) }
        val trip = args?.trip

        if (trip != null) {
            val departurePoint = GeoPoint(trip.departure_coordinates!!.latitude, trip.departure_coordinates!!.longitude)
            val arrivalPoint = GeoPoint(trip.arrival_coordinates!!.latitude, trip.arrival_coordinates!!.longitude)

            val map = view.findViewById<MapView>(R.id.trip_route_map)
            map.setTileSource(TileSourceFactory.MAPNIK)
            val mapController = map.controller
            mapController.setZoom(7.0)

            // departure and arrival points
            val startMarker = Marker(map)
            val endMarker = Marker(map)
            startMarker.position = departurePoint
            endMarker.position = arrivalPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.icon =
                context?.let { ContextCompat.getDrawable(it, R.drawable.map_start_marker) }
            endMarker.icon =
                context?.let { ContextCompat.getDrawable(it, R.drawable.map_end_marker) }
            map.overlays.add(startMarker)
            map.overlays.add(endMarker)

            // list of points for the line to be drawn
            val pointsList = mutableListOf<GeoPoint>()
            pointsList.add(departurePoint)
            trip.stopList?.forEach {stopPoint ->
                val position = GeoPoint(stopPoint.location_coordinates!!.latitude, stopPoint.location_coordinates!!.longitude)
                pointsList.add(position)
            }
            pointsList.add(arrivalPoint)

            val line = Polyline()
            line.setPoints(pointsList)
            line.outlinePaint.color = Color.parseColor("#B00000FF")
            map.overlays.add(line)

            // stop points
            trip.stopList?.forEach {stopPoint ->
                val position = GeoPoint(stopPoint.location_coordinates!!.latitude, stopPoint.location_coordinates!!.longitude)
                val stopMarker = Marker(map)
                stopMarker.position = position
                stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                stopMarker.icon =
                    context?.let { ContextCompat.getDrawable(it, R.drawable.map_stop_marker) }
                map.overlays.add(stopMarker)
            }

            // zoom on the points
            map.addOnFirstLayoutListener { v, left, top, right, bottom ->
                val b: BoundingBox =
                    BoundingBox.fromGeoPointsSafe(pointsList)
                map.zoomToBoundingBox(b, false, 120)
                map.invalidate()
            }
        }
}

}