/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils.counts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

/**
 * The sum of all day counts are assigned to a new counts file in the first hour
 */
public class CountsHr_2_24 {

	private void run (final TransitRoute trRoute, final Counts counts, final String output){
		//get transitRoute stops to calibrate only those stops.
		List<Id> scheduleStopsIds = new ArrayList<Id>();
		for(TransitRouteStop stop : trRoute.getStops()){
			scheduleStopsIds.add(stop.getStopFacility().getId());
		}
		
		//create output counts
		Counts outCounts = new Counts();
		outCounts.setDescription(counts.getDescription());
		outCounts.setName(counts.getName());
		outCounts.setYear(counts.getYear());
		
		char point = '.';
		for(Map.Entry <Id<Link>,Count> entry: counts.getCounts().entrySet()){
			Id countStopId = entry.getKey(); 

			//consider only counts for the given transit route
			if (!scheduleStopsIds.contains(countStopId)){
				continue;
			}
			
			Count count = entry.getValue();
			String strId = countStopId.toString();
			
			///convert to general facility location
			//strId  = strId.substring(0, strId.indexOf(point) );
			//Id newId  = new IdImpl(strId);
			
			//not to convert
			Id newId  = countStopId;
			
			//create counts if it does not exist
			if(!outCounts.getCounts().keySet().contains(newId)){
				Count newCount = outCounts.createAndAddCount(newId, count.getCsId());
				newCount.createVolume(1, 0);
				newCount.setCoord(count.getCoord());
			}
			Count newCount = outCounts.getCount(newId);
			
			//calculate sum of day volume value
			double daySum=0;
			for(Volume volume: count.getVolumes().values()){
				daySum += volume.getValue();
			}
			
			//add volume value
			double newValue = newCount.getVolume(1).getValue() + daySum;
			newCount.getVolumes().get(1).setValue(newValue);
			System.out.println(countStopId + " " + daySum + "\t" + strId + " " + newCount.getVolumes().get(1).getValue() );

			//fill the other 23 hours with zero  :(
			for (int i=2;i<=24;i++){
				//newCount.createVolume(i, 0);
			}
		
		}

		new CountsWriter(outCounts).write(output);
	}
	
	public static void main(String[] args) {
		final String strRoute = "B-M44.101.901.H"; 
		final String strTransitSchedulePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		final String counts24 = "../../berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
		final String outCountsDay= "../../berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_B-M44.101.901.H_allDay.xml";
		
		DataLoader loader = new DataLoader();
		Counts counts = loader.readCounts(counts24);
		TransitSchedule schedule = loader.readTransitSchedule(strTransitSchedulePath);
		TransitRoute route = loader.getTransitRoute(strRoute, schedule);
		new CountsHr_2_24().run(route, counts, outCountsDay);
	}

}
