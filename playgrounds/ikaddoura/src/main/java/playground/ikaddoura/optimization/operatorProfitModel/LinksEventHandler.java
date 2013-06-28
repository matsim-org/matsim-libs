/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.optimization.operatorProfitModel;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author Ihab
 *
 */
public class LinksEventHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	private double vehicleKm;
	private final Network network;
//	private final Map<Id, List<Double>> linkId2linkLeaveTime = new HashMap<Id, List<Double>>();
	
	public LinksEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleKm = 0.0;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id vehicleId = event.getVehicleId();
		if (vehicleId.toString().contains("bus")){
			// vehicleKm
			this.vehicleKm = this.vehicleKm + network.getLinks().get(event.getLinkId()).getLength()/1000;
			
//			// takt
//			if (this.linkId2linkLeaveTime.containsKey(event.getLinkId())){
//				List<Double> depTimes = this.linkId2linkLeaveTime.get(event.getLinkId());
//				depTimes.add(event.getTime());
//				this.linkId2linkLeaveTime.put(event.getLinkId(), depTimes);
//			}
//			else {
//				List<Double> depTimes = new ArrayList<Double>();
//				depTimes.add(event.getTime());
//				this.linkId2linkLeaveTime.put(event.getLinkId(), depTimes);
//			}
		}
		else {}		
	}

	public double getVehicleKm() {
		return this.vehicleKm;
	}

//	public void setTakt(Map<Integer, TimePeriod> day) {		
//		for (TimePeriod period : day.values()){
//			Map<Id, List<Double>> linkId2diff = new HashMap<Id, List<Double>>();
//			double fromTime = period.getFromTime();
//			double toTime = period.getToTime();
//			for (Id linkId : this.linkId2linkLeaveTime.keySet()){
//				List<Double> depTimes = this.linkId2linkLeaveTime.get(linkId);
//				double depTimeBefore = 0;
//				List<Double> diffs = new ArrayList<Double>();
//				
//				for (Double depTime : depTimes){
//					if (depTime >= fromTime && depTime < toTime){
//						if (depTimeBefore > 0){
//							double diff = depTime - depTimeBefore;
//							diffs.add(diff);
//						}
//						depTimeBefore = depTime;
//					}
//				}
//				linkId2diff.put(linkId, diffs);
//			}
//			
//			Map<Id, Double> linkId2takt = new HashMap<Id, Double>();
//			for(Id linkId : linkId2diff.keySet()){
//				double diffSum = 0;
//				List<Double> diffs = linkId2diff.get(linkId);
//				for (Double diff : diffs){
//					diffSum = diffSum + diff;
//				}
//				double linkTakt = diffSum/(double)diffs.size();
//				linkId2takt.put(linkId, linkTakt);
//			}
//			
//			double linkTaktSum = 0;
//			for (Double linkTakt : linkId2takt.values()){
//				linkTaktSum = linkTaktSum + linkTakt;
//			}
//	
//			double takt = linkTaktSum/linkId2takt.size();
//			period.setAverageTaktFromEvents(takt);
//		}
//	}

}
