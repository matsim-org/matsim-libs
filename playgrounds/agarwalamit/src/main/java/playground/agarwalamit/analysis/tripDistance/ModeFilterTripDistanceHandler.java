/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.tripDistance;

import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * Created by amit on 11/10/16.
 */


public class ModeFilterTripDistanceHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler{

    private static final Logger LOG = Logger.getLogger(ModeFilterTripDistanceHandler.class);
    private final TripRouteDistanceHandler delegate ;
    private final String mode2Consider;
    private final Map<Id<Person>,String> personId2mode = new HashMap<>(); // person id -- mode
    private final Vehicle2DriverEventHandler vehicle2DriverDelegate; // person id -- vehicle id


    public ModeFilterTripDistanceHandler(final Network network, final double simulationEndTime, final int noOfTimeBins, final String mode2Consider){
        this.delegate = new TripRouteDistanceHandler(network, simulationEndTime, noOfTimeBins);
        this.vehicle2DriverDelegate = new Vehicle2DriverEventHandler();
        this.mode2Consider = mode2Consider;
        LOG.info("Only "+this.mode2Consider +" will be considerd to calculate trip distances");
    }

    /*
    * By default, this will analyze only car trips.
    */
    public ModeFilterTripDistanceHandler(final Network network, final double simulationEndTime, final int noOfTimeBins){
        this(network,simulationEndTime,noOfTimeBins,"car");
    }

    @Override
    public void reset(int iteration) {
        delegate.reset(iteration);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = this.vehicle2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
        if(personId == null ) return;
        String mode = personId2mode.get(personId);
        if(mode2Consider.contains(mode)) {
            delegate.handleEvent(event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(mode2Consider.contains(event.getLegMode())) {
            delegate.handleEvent(event);
            personId2mode.remove(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(mode2Consider.contains(event.getLegMode())) {
            delegate.handleEvent(event);
            personId2mode.put(event.getPersonId(),event.getLegMode());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        String mode = event.getNetworkMode();
        if(mode==null) mode = personId2mode.get(event.getPersonId());

        if(mode2Consider.contains(mode)) {
            delegate.handleEvent(event);
            vehicle2DriverDelegate.handleEvent(event);
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        String mode = event.getNetworkMode();
        if(mode==null) mode = personId2mode.get(event.getPersonId());

        if(mode2Consider.contains(mode)) {
            delegate.handleEvent(event);
            vehicle2DriverDelegate.handleEvent(event);
        }
    }

    public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
        return delegate.getTimeBin2Person2TripsCount();
    }

    public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripsDistance() {
        return delegate.getTimeBin2Person2TripsDistance();
    }
}