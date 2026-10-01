package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by fouriep on 12/16/14.
 */
public class VehicleTrackerCollection {
    Map<String, TreeMap<Double, VehicleTracker>> trackers;

    public VehicleTrackerCollection(int numberOfVehicles) {
        this.trackers = new HashMap<>(numberOfVehicles);
    }

    public void put(String vehicleId, double time, VehicleTracker tracker) {
        TreeMap<Double, VehicleTracker> treeMap = trackers.get(vehicleId);
        if (treeMap == null) {
            treeMap = new TreeMap<>();
            trackers.put(vehicleId, treeMap);
        }
        treeMap.put(time, tracker);
    }

    /**
     * Returns the tracker for the vehicle's most recent departure at or before the given time.
     * A vehicle that serves several departures in a day has one tracker per departure, so
     * scanning the map in ascending key order would attribute the whole day to the first one.
     */
    public VehicleTracker get(String vehicleId, double time) {
        TreeMap<Double, VehicleTracker> treeMap = trackers.get(vehicleId);
        if (treeMap == null) return null;
        Map.Entry<Double, VehicleTracker> entry = treeMap.floorEntry(time);
        return entry == null ? null : entry.getValue();
    }
}
