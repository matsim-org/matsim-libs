/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.Validators;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.utils.DataLoader;


/**validates the correct sequence of links in the multimodal net for a given transitSchedule*/
public class TransitLineValidator {

	public TransitLineValidator(ScenarioImpl scenarioImpl) {
		Network network = scenarioImpl.getNetwork();
		
		for (TransitLine transitLine : scenarioImpl.getTransitSchedule().getTransitLines().values()){
			for (TransitRoute transitRoute: transitLine.getRoutes().values()){
				
				int stopIndex=0;
				List <TransitRouteStop> stopList = transitRoute.getStops();
				
				TransitRouteStop firstStop = stopList.get(0);
				
				//validates the sequence of links
				Id lastLinkId = null;
				
				System.out.println("\n transitRoute: " + transitRoute.getId());
				int x=0;
				for (TransitRouteStop stop : stopList ){
					System.out.println("stopFactility: " + stop.getStopFacility().getId() + " link:"  +  stop.getStopFacility().getLinkId());
					x++;
				}

				for (Id inkId : transitRoute.getRoute().getLinkIds() ){
					System.out.println("route link Id:" + inkId);
				}
				
				/*
				for (Id linkId: transitRoute.getRoute().getLinkIds()){
					Link link = network.getLinks().get(linkId);					
					if(lastLinkId!=null){
						if (!link.getFromNode().getInLinks().containsKey(lastLinkId)){
							System.out.println("Error : " + transitRoute.getId() );
						}
					}
					
					System.out.println("LinkId : " + linkId + " " + "StopId : " + stopList.get(stopIndex++).getStopFacility().getId());
					//System.out.println("StopId : " + stopList.get(stopIndex++) );
					
					//validates sequence of stops
					TransitRouteStop stop = stopList.get(stopIndex);
					if(stopList.get(stopIndex).getStopFacility().getLinkId().equals(linkId)){
						stopIndex++;
					}
					lastLinkId = linkId;
				}
				//System.out.println(firstStop.getStopFacility().getLinkId().equals(transitRoute.getRoute().getStartLinkId())) ;
				
				//System.out.println("stopIndex: "+ " " + stopIndex + " stopList.size(): " + stopList.size());
				*/
			}
		}
		
		System.out.println("done.");
		
	}
	
	public static void main(String[] args) {
		String configFile;
		if (args.length>0){
			configFile= args[0]; 
		}else{
			configFile= "../playgrounds/mmoyo/output/config.xml"; 
		}
		
		ScenarioImpl scenarioImpl = new DataLoader().loadScenario(configFile);
		new TransitLineValidator(scenarioImpl);
	}
}
