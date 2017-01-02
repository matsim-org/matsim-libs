/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.linkVolume;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 02/01/2017.
 */


public class FilteredLinkVolumeHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private static final Logger LOGGER = Logger.getLogger(FilteredLinkVolumeHandler.class);

    private final LinkVolumeHandler delegate;
    private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();

    private final String ug;
    private final PersonFilter pf;

    public FilteredLinkVolumeHandler(final String vehiclesFile, final String userGroup, final PersonFilter personFilter) {
        this.ug = userGroup;
        this.pf = personFilter;

        this.delegate = new LinkVolumeHandler(vehiclesFile);

        if ((this.ug == null && this.pf != null) || this.ug != null && this.pf == null) {
            throw new RuntimeException("Either of person filter or user group is null.");
        } else if (this.ug != null) {
            LOGGER.info("User group filtering is used, result will include all links but persons from " + this.ug + " user group only.");
        } else {
            LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
        }
    }

    public FilteredLinkVolumeHandler(final String vehiclesFile) {
        this(vehiclesFile, null, null);
    }

    @Override
    public void reset(int iteration) {
        delegate.reset(iteration);
        veh2DriverDelegate.reset(iteration);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> driverId = veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
        if (this.ug == null || this.pf == null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }

    public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkVolumePCU() {
        return delegate.getLinkId2TimeSlot2LinkVolumePCU();
    }

    public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkCount() {
        return delegate.getLinkId2TimeSlot2LinkCount();
    }

    public Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> getLinkId2TimeSlot2VehicleIds() {
        return delegate.getLinkId2TimeSlot2VehicleIds();
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        veh2DriverDelegate.handleEvent(event);
        if (this.ug == null || this.pf == null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        veh2DriverDelegate.handleEvent(event);
        if (this.ug == null || this.pf == null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }
}
