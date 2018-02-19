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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.contrib.drt.passenger.events.*;




public class ZonalDemandAggregatorMy implements DrtRequestSubmittedEventHandler, DrtRequestRejectedEventHandler  {
	private final DrtZonalSystem zonalSystem;
	private final int binsize = 600; //Time resolution to store demand (departures)
	private Map<Double,Map<String,MutableInt>> rejections = new HashMap<>();
	private Map<Double,Map<String,MutableInt>> previousIterationRejections = new HashMap<>();
	
	private Map<Id<Request>,DrtRequestSubmittedEvent> submittedRequestsMap = new HashMap<>();
	private Map<Id<Request>,DrtRequestRejectedEvent> rejectedRequestsMap = new HashMap<>();
	
	
	
	private int nOfIterStarts;
	//private final String mode;
	
	public Map<Id<Vehicle>,Integer> vehicleBlackListMap = new HashMap<>();


	
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
		double alpha = 0.15;
		nOfIterStarts = iteration;
		
		//Clear this every iteration:
		submittedRequestsMap.clear();
		rejectedRequestsMap.clear(); //Each reject is looked up in submittedRequestsMap in order to get it's coordinates. 
		vehicleBlackListMap.clear(); //Clear vehicleBlackListMap at each iteration!
		
		//Clear all previousIterationRejections
		//previousIterationRejections is needed in order to get an estimate of the rejectedRequests in the simulation
		
		//At iteration 0 (iteration 1 for this class), the simulation needs to learn traffic conditions
		//At iteration 1 (iteration 2 for this class), the simulation learns the first time where we have rejected trips
		if (nOfIterStarts > 1 && nOfIterStarts <= 2 ) 
			{
			previousIterationRejections.clear(); //Clean previousIterationRejections
			previousIterationRejections.putAll(rejections); //Fill previousIterationRejections with knowledge of current iteration
			}
		
		//At iteration 2 (iteration 3 for this class) or higher, we keep the knowledge of rejected trips over each iteration,
		//Otherwise the results would oscillate between a good iteration with small rejects and a iteration with high 
		if (nOfIterStarts >= 3)  {
		
			//Update previousIterationRejections --> Expectation for next iteration
			//previousIterationRejections = currentRejections + previousIterationRejections
			
			//rejections contains the information of all rejections measured within last iteration
			//Loop over all bins
			for (Entry<Double, Map<String, MutableInt>> bin : rejections.entrySet())
			{
				Double binKey = bin.getKey();
				
				//Each bin contains multiple zones
				Map<String, MutableInt> zones = bin.getValue();
				
				//Loop over all zones within a bin
					for (Entry<String, MutableInt> zone : zones.entrySet()) {
						String zoneKey = zone.getKey();
						
						//Get currentDeparture in this iteration
						double currentRejections = zone.getValue().doubleValue();
						
						//Get previousIterationDepartures
						double prevRejects = previousIterationRejections.get(binKey).get(zoneKey).doubleValue();
						
						//Exponential smoothing of predictedRejects
						double predictedRejects = (currentRejections*alpha + prevRejects * (1.0-alpha));
								
						//Set departures that are used predict the number of rejected trips per zone
						previousIterationRejections.get(binKey).get(zoneKey).setValue(Math.ceil(predictedRejects));
					}

			}
			
		}
		
		rejections.clear(); //Clear rejections to store new rejectedRequests of current iteration
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
			rejections.put(Double.valueOf(i),zonesPerSlot);

		}
	}
	
	private Double getBinForTime(double time){
		
		return Math.floor(time/binsize); 
	}
	
	public Map<String,MutableInt> getExpectedRejectsForTimeBin(double time){
		Double bin = getBinForTime(time);
		return previousIterationRejections.get(bin);
		
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
		if (rejections.containsKey(bin)){
			this.rejections.get(bin).get(zoneId).increment();
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

