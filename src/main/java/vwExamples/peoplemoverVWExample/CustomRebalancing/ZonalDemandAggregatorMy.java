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

package vwExamples.peoplemoverVWExample.CustomRebalancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.contrib.drt.passenger.events.*;
import org.matsim.contrib.drt.data.DrtRequest;



/**
 * @author  axer
 *
 */
/**
 *
 */
public class ZonalDemandAggregatorMy implements DrtRequestSubmittedEventHandler, DrtRequestRejectedEventHandler  {
	private final DrtZonalSystem zonalSystem;
	private final int binsize = 600; //Time resolution to store demand (departures)
	private Map<Double,Map<String,MutableInt>> departures = new HashMap<>();
	private Map<Double,Map<String,MutableInt>> previousIterationDepartures = new HashMap<>();
	
	private Map<Id<Request>,DrtRequestSubmittedEvent> submittedRequestsMap = new HashMap<>();
	private Map<Id<Request>,DrtRequestRejectedEvent> rejectedRequestsMap = new HashMap<>();
	
	private int nOfIterStarts;
	//private final String mode;

	
	
	
	/**
	 * 
	 */
	@Inject
	public ZonalDemandAggregatorMy(EventsManager events, DrtZonalSystem zonalSystem, Config config) {
		this.zonalSystem= zonalSystem;
		events.addHandler(this);
		//DvrpConfigGroup dvrpconfig = DvrpConfigGroup.get(config);
		//this.mode = dvrpconfig.getMode();
		
	}
	
	@Override
	public void reset(int iteration){
		nOfIterStarts = iteration;
		
		//Clear this every iteration:
		submittedRequestsMap.clear();
		rejectedRequestsMap.clear(); //Each reject is looked up in submittedRequestsMap in order to get it's coordinates. 
		
		//Clear all previousIterationDepartures
		//previousIterationDepartures is needed in order to get an estimate of the rejectedRequests in the simulation
		
		//At iteration 0 (iteration 1 for this class), the simulation needs to learn traffic conditions
		//At iteration 1 (iteration 2 for this class), the simulation learns the first time where we have rejected trips
		if (nOfIterStarts > 1 && nOfIterStarts <= 2 ) 
			{
			previousIterationDepartures.clear(); //Clean previousIterationDepartures
			previousIterationDepartures.putAll(departures); //Fill previousIterationDepartures with knowledge of current iteration
			}
		
		//At iteration 2 (iteration 3 for this class) or higher, we keep the knowledge of rejected trips over each iteration,
		//Otherwise the results would oscillate between a good iteration with small rejects and a iteration with high 
		if (nOfIterStarts >= 3)  {
		
			//Update previousIterationDepartures by comparing current departures and previousIterationDepartures
			//previousIterationDepartures is current (remaining departures) + previousIterationDepartures
			
			
			//Loop over all bins
			for (Entry<Double, Map<String, MutableInt>> bin : departures.entrySet())
			{
				Double binKey = bin.getKey();
				
				//Each bin contains multiple zones
				Map<String, MutableInt> zones = bin.getValue();
				
				//Loop over all zones within a bin
					for (Entry<String, MutableInt> zone : zones.entrySet()) {
						String zoneKey = zone.getKey();
						
						//Get currentDeparture in this iteration
						MutableInt currentDeparture = zone.getValue();
						
						//Get previousIterationDepartures
						MutableInt prevDeparture = previousIterationDepartures.get(binKey).get(zoneKey);
						
						int predictedDemand = (prevDeparture.getValue()+currentDeparture.getValue());
						
						
//						if ((currentDeparture != null) && (prevDeparture != null))
//						{
//							if (currentDeparture.getValue() != 0)
//							{
//							factor = predictedDemand/currentDeparture.getValue();
//							}
//
//							
//						}
//						
						//Set departures that are used predict the number of rejected trips per zone
						previousIterationDepartures.get(binKey).get(zoneKey).setValue(predictedDemand);
					}
				
				
			}
			
		}
		
		departures.clear(); //Clear departures to store new rejectedRequests of current iteration
		prepareZones(); //Initialize zones again in order to store new rejectedRequests of current iteration

	}
	
	
	private void prepareZones(){
		for (int i = 0;i<(3600/binsize)*36;i++){
			//Slot is a times slot
			//For each slot, we have a Zone map
			Map<String,MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()){
				zonesPerSlot.put(zone, new MutableInt());
			}
			departures.put(Double.valueOf(i),zonesPerSlot);

		}
	}
	
	private Double getBinForTime(double time){
		
		return Math.floor(time/binsize); 
	}
	
	public Map<String,MutableInt> getExpectedDemandForTimeBin(double time){
		Double bin = getBinForTime(time);
		return previousIterationDepartures.get(bin);
		
	}
	
	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		//System.out.println("DrtRequestSubmittedEvent");
		DrtRequestSubmittedEvent submittedRequest = event;
		
		//Add a submitted request to map
		submittedRequestsMap.put(submittedRequest.getRequestId(), submittedRequest);
	}

	@Override
	public void handleEvent(DrtRequestRejectedEvent event) {
		//System.out.println("DrtRequestScheduledEvent");
		DrtRequestRejectedEvent rejectedRequest = event;
		
		//Store each rejectedRequest in rejectedRequestsMap
		rejectedRequestsMap.put(rejectedRequest.getRequestId(), rejectedRequest);
		
		DrtRequestSubmittedEvent originaleRquest = submittedRequestsMap.get(rejectedRequest.getRequestId());
		
		
		Double bin = getBinForTime(originaleRquest.getTime());
		String zoneId = zonalSystem.getZoneForLinkId(originaleRquest.getFromLinkId());

		if (zoneId == null) {
			Logger.getLogger(getClass()).error("No zone found for linkId "+ originaleRquest.getFromLinkId().toString());
			return;
		}
		if (departures.containsKey(bin)){
			this.departures.get(bin).get(zoneId).increment();
			}
	
		
		
	}
	
	
	public int getOpenRequests() {
		return rejectedRequestsMap.size();
	}
	
	public int getnOfIterStarts() {
		return nOfIterStarts;
	}


	




	


//	@Override
//	public void handleEvent(PersonDepartureEvent event) {
//		if (event.getLegMode().equals(mode)){
//			Double bin = getBinForTime(event.getTime());
//			
//			String zoneId = zonalSystem.getZoneForLinkId(event.getLinkId());
//			if (zoneId == null) {
//				Logger.getLogger(getClass()).error("No zone found for linkId "+ event.getLinkId().toString());
//				return;
//			}
//			if (departures.containsKey(bin)){
//			this.departures.get(bin).get(zoneId).increment();
//			}
//			else Logger.getLogger(getClass()).error("Time "+Time.writeTime(event.getTime())+" / bin "+bin+" is out of boundary"); 
//			}
//}
	 




	
}

