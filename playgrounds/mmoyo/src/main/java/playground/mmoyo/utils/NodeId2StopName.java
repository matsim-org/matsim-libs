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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
/**
 * replaces the id of nodes to human legible stop names if the are in the count object
 */
public class NodeId2StopName {
	private final Counts counts;
	private final String point = ".";
	private Map <Id, Id> stopNameMap = new HashMap <Id, Id>();
	
	public NodeId2StopName(Counts counts){
		this.counts = counts;
		
		//create map of id - stopNames
		String sep = "_";
		for (Count count : this.counts.getCounts().values()){
			String strCsId = count.getCsId().toString().substring(2, 10);
			Id newCdId = new IdImpl(count.getLocId().toString() + sep + strCsId);
			stopNameMap.put(count.getLocId(), newCdId);
			//System.out.println(count.getLocId() + " " + newCdId);
		}
	}
	
	private Id getNewId (Id oldId){
		Id newId = stopNameMap.get(oldId);
		return  newId==null? oldId: newId;
	}
	
	/** replaces id's in a transitSchedule*/
	public void run(final TransitSchedule schedule){
		
		String q = "\" ";
		for(Count count : this.counts.getCounts().values()){
			TransitStopFacility stop = schedule.getFacilities().get(count.getLocId());
			
			StringBuffer sBuff = new StringBuffer("<stopFacility id=\"" + stop.getId());
			sBuff.append("_" + count.getCsId() + q);
			sBuff.append("x=\"" + stop.getCoord().getX() + q);
			sBuff.append("y=\"" + stop.getCoord().getY() + q);
			sBuff.append("linkRefId=\"" + stop.getLinkId() + q);
			sBuff.append("isBlocking=\"false\"/>");
		
			System.out.println(sBuff.toString());
		}

	}
	
	public static void main(String[] args) {
		String netFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String countsFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44_344.xml"; 
		String scheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String outputScheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/mySchedule.xml";
		
		DataLoader dataLoader = new DataLoader();
		NetworkImpl net = dataLoader.readNetwork(netFile);
		TransitSchedule schedule = dataLoader.readTransitSchedule(net, scheduleFile);
		
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		try {
			reader.parse(countsFile);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		new NodeId2StopName(counts).run(schedule);
	}

}
