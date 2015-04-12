package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import java.util.HashMap;
import java.util.Iterator;
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

    public VehicleTracker get(String vehicleId, double time) {
        TreeMap<Double, VehicleTracker> treeMap = trackers.get(vehicleId);
        if (treeMap == null) return null;
        Iterator<Map.Entry<Double, VehicleTracker>> iterator = treeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Double, VehicleTracker> entry = iterator.next();
            if (time >= entry.getKey())
                return entry.getValue();
        }
        return null;
    }
}
