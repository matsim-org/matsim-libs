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

package parking.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 02.03.18.
 */

public class ParkingTripEventHandler implements ActivityStartEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private static final String parkSearchStartAct = "car parkingSearch";
    private static final String carInteractionAct = "car interaction";

    private final Map<Id<Vehicle>,Id<Person>> vehicleToDriver = new HashMap<>();
    private Map<Id<Person>, List<ParkingTrip>> person2ParkingTrips = new HashMap<>();
    private final Set<String> mainActivities ;

    private static Network network;

    public ParkingTripEventHandler(Network network, Set<String> mainActivities) {
        this.mainActivities = mainActivities;
        ParkingTripEventHandler.network = network;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(parkSearchStartAct)){
            // start parking trip
            List<ParkingTrip> trips = this.person2ParkingTrips.getOrDefault(event.getPersonId(), new ArrayList<>());
            ParkingTrip parkingTrip = new ParkingTrip(event.getPersonId(), event.getLinkId(), event.getTime(), trips.size());
            trips.add(parkingTrip);
            this.person2ParkingTrips.put(event.getPersonId(), trips);

        } else if ( event.getActType().equals(parkSearchStartAct) ) {

            List<ParkingTrip> trips = this.person2ParkingTrips.get(event.getPersonId());
            if (trips==null) return;

            ParkingTrip lastTrip = trips.get(trips.size()-1);
            if( (!lastTrip.parkTripEnded)  //open park trip
                    &&  lastTrip.parkSearchEndLink==null){ //
                lastTrip.endCarLeg(event.getLinkId(), event.getTime()); // started walking to main activity
            }

        } else if (this.mainActivities.contains(event.getActType())){
            // starting normal activities
            List<ParkingTrip> trips = this.person2ParkingTrips.get(event.getPersonId());
            if (trips==null) return;

            ParkingTrip lastTrip = trips.get(trips.size()-1);

            if ( lastTrip.parkSearchEndLink !=null) {
                lastTrip.endParkTrip(event.getLinkId(), event.getTime());
            }

        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        List<ParkingTrip> trips = this.person2ParkingTrips.get(this.vehicleToDriver.get(event.getVehicleId()));
        if (trips==null) return; // no park act
        else if( ! trips.get(trips.size()-1).parkTripEnded) { // open park trip
            trips.get(trips.size()-1).travelledLinks_duringParkSearch.add(event.getLinkId());
        }
    }

    @Override
    public void reset(int iteration) {
        this.vehicleToDriver.clear();
        this.person2ParkingTrips.clear();
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.vehicleToDriver.put(event.getVehicleId(), event.getPersonId());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicleToDriver.remove(event.getVehicleId());
    }

    public Map<Id<Person>, List<ParkingTrip>> getPerson2ParkingTrips() {
        return person2ParkingTrips;
    }

    public static class ParkingTrip{
        //constructor args
        private final int tripIndex ;
        private final Id<Person> personId;
        private final Id<Link> parkSearchStartLink ;
        private final double parkSearchStartTime;
        //

        private final List<Id<Link>> travelledLinks_duringParkSearch = new ArrayList<>();

        //park search end
        private Id<Link> parkSearchEndLink;
        private double parkSearchEndTime;
        private double parkSearchDistance_car; // car-mileage
        private double parkSearchTime_car; // time in car between 'car parkingSearch' and 'car interaction' activities
        //

        // park trip end (start act like work/home/other)
        private Id<Link> actStartLink;
        private double parkSearchDistance_walk;
        private double parkSearchTime_walk; // time to walk from car parked link to work location
        private boolean parkTripEnded = false;
        //

        ParkingTrip(Id<Person> personId, Id<Link> parkSearchStartLink, double parkSearchStartTime, int tripIndex){
            this.personId = personId;
            this.parkSearchStartLink = parkSearchStartLink;
            this.parkSearchStartTime = parkSearchStartTime;
            this.tripIndex = tripIndex;
        }

        void endCarLeg(Id<Link> parkSearchEndLink, double parkSearchEndTime){
            this.parkSearchEndLink = parkSearchEndLink;
            this.travelledLinks_duringParkSearch.add(parkSearchEndLink);

            this.parkSearchEndTime = parkSearchEndTime;
            this.parkSearchDistance_car = this.travelledLinks_duringParkSearch.stream()
                                                                              .map(l -> network.getLinks()
                                                                                               .get(l)
                                                                                               .getLength())
                                                                              .reduce(0., Double::sum);
            this.parkSearchTime_car = parkSearchEndTime- this.parkSearchStartTime;
        }

        void endParkTrip(Id<Link> actStartLink, double parkTripEndTime){
            this.actStartLink = actStartLink;
            this.parkSearchTime_walk = parkTripEndTime - this.parkSearchEndTime;
            this.parkSearchDistance_walk = NetworkUtils.getEuclideanDistance(network.getLinks()
                                                                                    .get(this.parkSearchEndLink)
                                                                                    .getToNode()
                                                                                    .getCoord(),
                    network.getLinks().get(actStartLink).getToNode().getCoord());
            this.parkTripEnded = true;
        }

        public int getTripIndex() {
            return tripIndex;
        }

        public Id<Link> getParkSearchStartLink() {
            return parkSearchStartLink;
        }

        public double getParkSearchStartTime() {
            return parkSearchStartTime;
        }

        public List<Id<Link>> getTravelledLinks_duringParkSearch() {
            return travelledLinks_duringParkSearch;
        }

        public Id<Link> getParkSearchEndLink() {
            return parkSearchEndLink;
        }

        public double getParkSearchEndTime() {
            return parkSearchEndTime;
        }

        public double getParkSearchTime_car() {
            return parkSearchTime_car;
        }

        public Id<Link> getActStartLink() {
            return actStartLink;
        }

        public double getParkSearchTime_walk() {
            return parkSearchTime_walk;
        }

        public double getParkSearchDistance_car() {
            return parkSearchDistance_car;
        }

        public double getParkSearchDistance_walk() {
            return parkSearchDistance_walk;
        }

        public Id<Person> getPersonId() {
            return personId;
        }

        public boolean isParkTripEnded() {
            return parkTripEnded;
        }
    }
}
