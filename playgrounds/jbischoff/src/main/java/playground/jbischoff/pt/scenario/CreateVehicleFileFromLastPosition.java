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

/**
 * 
 */
package playground.jbischoff.pt.scenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 */
public class CreateVehicleFileFromLastPosition {
public static void main(String[] args) {
	String eventsFile = "D:/runs-svn/intermodal/4400/4400.output_events.xml.gz";
	String networkFile = "D:/runs-svn/intermodal/4400/4400.output_network.xml.gz";
	String vehiclesFile = "D:/runs-svn/intermodal/4400/v4400_lastPosition.xml";
	final int capacity = 4;
	final int t0 = 0;
	final int t1 = 36*3600; 
	final List<Vehicle> vehicles = new ArrayList<Vehicle>();  
	
	EventsManager events = EventsUtils.createEventsManager();
		 
		 final Network network = NetworkUtils.createNetwork();
		 new MatsimNetworkReader(network).readFile(networkFile);
		 events.addHandler(new ActivityStartEventHandler() {
			
			@Override
			public void reset(int iteration) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void handleEvent(ActivityStartEvent event) {
				if (event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)){
					Link l = network.getLinks().get(event.getLinkId()); 
					Vehicle v = new VehicleImpl(Id.create(event.getPersonId().toString(),Vehicle.class), l , capacity , t0, t1);
					vehicles.add(v);
				}
			}
		});
	new MatsimEventsReader(events).readFile(eventsFile);
	new VehicleWriter(vehicles).write(vehiclesFile)	;
	
}
}
