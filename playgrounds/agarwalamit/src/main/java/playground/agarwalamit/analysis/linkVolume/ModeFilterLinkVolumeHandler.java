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

package playground.agarwalamit.analysis.linkVolume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 25/09/16.
 */


public class ModeFilterLinkVolumeHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private final LinkVolumeHandler delegate = new LinkVolumeHandler();
    private final List<Id<Vehicle>> vehicleIds = new ArrayList<>();
    private final List<String> modes ;

    /**
     * @param modes
     *
     * This will filter out the modes other than given mode for link volumes.
     */
    public ModeFilterLinkVolumeHandler(final List<String> modes) {
        this.modes = modes;
    }

    public ModeFilterLinkVolumeHandler() { this(null); }

    @Override
    public void reset(int iteration) {
        delegate.reset(iteration);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if( vehicleIds.contains(event.getVehicleId()) ) {
            delegate.handleEvent(event);
        }
    }

    public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkVolumePCU() {
        throw new RuntimeException("no vehicle file is provided to get the PCU of each vehicle type, see delegate method. " +
                "Alternatively, get the vehicle counts and multiply by pcu.");
    }

    public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkCount() {
        return delegate.getLinkId2TimeSlot2LinkCount();
    }

    public Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> getLinkId2TimeSlot2VehicleIds() {
        return delegate.getLinkId2TimeSlot2VehicleIds();
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if(modes.contains(event.getNetworkMode())) {
            delegate.handleEvent(event);
            vehicleIds.remove(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if( modes.contains(event.getNetworkMode()) ) {
            delegate.handleEvent(event);
            vehicleIds.add(event.getVehicleId());
        }
    }
}
