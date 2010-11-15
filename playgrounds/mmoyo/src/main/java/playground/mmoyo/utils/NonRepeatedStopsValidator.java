/* *********************************************************************** *
 * project: org.matsim.*
 * NodeId2StopName.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/** validates that two transit lines do not share any stop**/
public class NonRepeatedStopsValidator {
	
	public static void main(String[] args) {
		String netFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String scheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		
		DataLoader dataLoader = new DataLoader();
		NetworkImpl net = dataLoader.readNetwork(netFile);
		TransitSchedule schedule = dataLoader.readTransitSchedule(net, scheduleFile);
		
		TransitLine line344 = schedule.getTransitLines().get(new IdImpl("B-344"));
		TransitRoute route344H = line344.getRoutes().get(new IdImpl("B-344.101.901.H"));
		TransitRoute route344R = line344.getRoutes().get(new IdImpl("B-344.101.901.R"));
		
		TransitLine lineM44 = schedule.getTransitLines().get(new IdImpl("B-M44"));
		
		for (TransitRoute route: lineM44.getRoutes().values()){
			for (TransitRouteStop stop : route.getStops()){
				Id id  = stop.getStopFacility().getId();

				for (TransitRouteStop stopH : route344H.getStops()){
					if (stopH.getStopFacility().getId().equals(id) || stopH.getStopFacility().getId() == id ){
						System.out.println("stop repetead!");
					}
				}
				
				for (TransitRouteStop stopR : route344R.getStops()){
					if (stopR.getStopFacility().getId().equals(id) || stopR.getStopFacility().getId() == id ){
						System.out.println("stop repetead!");
					}
				}
			}
		}

	}

}
