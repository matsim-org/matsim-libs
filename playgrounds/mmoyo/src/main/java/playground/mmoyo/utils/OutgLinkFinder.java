/* *********************************************************************** *
 * project: org.matsim.*
 * OutgLinkFinder.java
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

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkImpl;
/**
 * Finds the outgoing link of each stopFacility
 */
public class OutgLinkFinder {
	TransitSchedule schedule=null;
	NetworkImpl net = null;
	
	public OutgLinkFinder(final String networkFile, final String scheduleFile){
		DataLoader dataLoader= new DataLoader();
		this.net = dataLoader.readNetwork(networkFile);
		this.schedule = dataLoader.readTransitSchedule(net, scheduleFile);
	}

	//This proves that a transit node has many outgoing links
	private void run(){
		String space = " "; 
		String n = "\n";
		for (TransitStopFacility stop : this.schedule.getFacilities().values()){
			System.out.print (n + stop.getId()+ space);
			for (TransitLine line: this.schedule.getTransitLines().values()){
				for (TransitRoute route: line.getRoutes().values()){
					if(route.getStop(stop)!=null){
						Id refLinkId = stop.getLinkId();
						int i = route.getRoute().getLinkIds().indexOf(refLinkId);
						try {
							Id outgLinkId = route.getRoute().getLinkIds().get(i+1);
							System.out.print (outgLinkId + space );
						} catch (IndexOutOfBoundsException e){
							System.err.print("last link of the route: " + route.getId());
						}
					}
				}
			}
		}	
	}
	
	public static void main(String[] args) {
		String netFile= "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String scheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		OutgLinkFinder outgLinkFinder = new OutgLinkFinder(netFile, scheduleFile);
		outgLinkFinder.run();
	}

}
