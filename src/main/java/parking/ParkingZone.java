/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package parking;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 20.02.18.
 */

public class ParkingZone implements Identifiable<ParkingZone> {

    private final Id<ParkingZone> parkingZoneId;

    private double zoneParkingCapacity = 0.;

    private final Map<Id<Link>, Double> linkToParkingCapacity = new HashMap<>();

    public ParkingZone(String id) {
        this.parkingZoneId = Id.create(id, ParkingZone.class);
    }

    public double getZoneParkingCapacity() {
        return this.zoneParkingCapacity;
    }

    public boolean isLinkInsideZone(Link link){
        return this.linkToParkingCapacity.containsKey(link.getId());
    }

    /**
     * @param val positive if a car is out and negative if a car is parked.
     */
    public void updateLinkParkingCapacity(Link link, double val) {
        double parkingCap = this.linkToParkingCapacity.getOrDefault(link.getId(), 0.);
        this.linkToParkingCapacity.put(link.getId(), parkingCap + val);
        this.zoneParkingCapacity += val;
    }

//    public Map<Link, Double> getLinkToParkingCapacity() {
//        return linkToParkingCapacity;
//    }
//
//    public Double getParkingCapacityForLink(Link link) {
//        return linkToParkingCapacity.get(link);
//    }

    public SortedMap<Id<Link>,Double> getLinkParkingProbabilities() { // this must be recalculated whenever required.
        SortedMap<Id<Link>, Double> linkToParkingProbs = new TreeMap<>();
        for (Map.Entry<Id<Link>,Double> entry : this.linkToParkingCapacity.entrySet()){
            linkToParkingProbs.put( entry.getKey(), entry.getValue() / this.zoneParkingCapacity);
        }
        return linkToParkingProbs;
    }

    @Override
    public Id<ParkingZone> getId() {
        return this.parkingZoneId;
    }
}
