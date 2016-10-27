/* *********************************************************************** *
 * project: org.matsim.*
 * ElevationEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.coord3D;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author jwjoubert
 *
 */
public class ElevationEventHandler implements LinkEnterEventHandler {
	Map<Id<Link>, Integer[]> linkMap;

	@Inject
	private Scenario sc;
	
	@Inject
	public ElevationEventHandler() {
		setupLinkMap();
	}
	
	@Override
	public void reset(int iteration) {
		setupLinkMap();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Link> lid = event.getLinkId();
		if(linkMap.containsKey(lid)){
			/* Identify the associated vehicle type. */
			Id<Vehicle> vid = event.getVehicleId();
			
			Vehicle vehicle = sc.getVehicles().getVehicles().get(vid);
			String vehicleType = vehicle.getType().getId().toString();
			int index;
			if(vehicleType.equalsIgnoreCase("A")){
				index = 0;
			} else if(vehicleType.equalsIgnoreCase("B")){
				index = 1;
			} else{
				throw new RuntimeException("Don't know what to do with vehicle of type '" + vehicleType + "'");
			}
			
			Integer[] ia = linkMap.get(lid);
			int oldValue = ia[index];
			ia[index] = oldValue + 1;
		}
	}
	
	private void setupLinkMap(){
		this.linkMap = new TreeMap<Id<Link>, Integer[]>();
		linkMap.put(Id.createLinkId("2"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("3"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("4"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("5"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("6"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("7"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("8"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("9"), new Integer[]{0,0});
		linkMap.put(Id.createLinkId("10"), new Integer[]{0,0});
	}


}
