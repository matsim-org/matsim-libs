/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.vsp.airPollution.exposure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	private final SortedMap<Double, Double[][]> duration = new TreeMap<>();
	private final Map<Id<Link>,Integer> link2xbins;
	private final Map<Id<Link>,Integer> link2ybins;
	private final  Double timeBinSize;
	private final  Double simulationEndTime;
	private final  int noOfxCells;
	private final  int noOfyCells;
	private final  Set<Id<Person>> recognisedPersons = new HashSet<>();


	public IntervalHandler(Double timeBinSize, Double simulationEndTime, GridTools gridTools){
		this.timeBinSize=timeBinSize;

		if ( simulationEndTime.isInfinite() ) throw new RuntimeException("Please set the simulation end time to a real value. Aborting...");

		this.simulationEndTime = simulationEndTime;
		this.noOfxCells = gridTools.getNoOfXCells();
		this.noOfyCells = gridTools.getNoOfYCells();
		this.link2xbins = gridTools.getLink2XBins();
		this.link2ybins = gridTools.getLink2YBins();
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		recognisedPersons.clear();
		for(int i=0; i<simulationEndTime/timeBinSize+1; i++){
			duration.put(i*timeBinSize, new Double[noOfxCells][noOfyCells]);
			for(int j=0; j< noOfxCells; j++){
				for(int k=0; k< noOfyCells; k++){
					duration.get(i*timeBinSize)[j][k]=0.0;
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		Id<Link> linkId = event.getLinkId();
		if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
			int xCell = link2xbins.get(linkId); 
			int	yCell = link2ybins.get(linkId);

			Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;

			if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;

			Double timeWithinCurrentInterval = new Double(event.getTime()-currentTimeBin+timeBinSize);
			if(recognisedPersons.contains(event.getPersonId())){	
				// time interval of activity
				double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
				
				double updatedDuration = prevDuration - timeBinSize + timeWithinCurrentInterval;
				duration.get(currentTimeBin)[xCell][yCell] = updatedDuration;
				
//				if (prevDuration>timeBinSize) { // this does not looks correct because prevDuration is sum of actDurations in that time bin for all persons amit Oct'15
//					prevDuration = prevDuration - timeWithinCurrentInterval;
//					duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
//				}
				currentTimeBin += timeBinSize;

				// later time intervals
				while(currentTimeBin <= simulationEndTime){
					
					double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
					duration.get(currentTimeBin)[xCell][yCell] = prevDurationL - timeBinSize;
					
//					if (prevDurationL>timeBinSize) {
//						prevDurationL = prevDurationL - timeBinSize;
//						duration.get(currentTimeBin)[xCell][yCell] = prevDurationL;
//					}
					
					currentTimeBin += timeBinSize;
				}
			}else{ // person not yet recognised
				recognisedPersons.add(event.getPersonId());
				Double tb = new Double(timeBinSize);
				// time bins prior to events time bin
				while(tb < currentTimeBin){
					duration.get(tb)[xCell][yCell] += timeBinSize;
					tb += timeBinSize;
				}
				// time bin of event
				duration.get(currentTimeBin)[xCell][yCell] += timeWithinCurrentInterval;
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		Id<Link> linkId = event.getLinkId();

		if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
			if(!recognisedPersons.contains(event.getPersonId()))recognisedPersons.add(event.getPersonId());

			int	xCell = link2xbins.get(linkId);
			int	yCell = link2ybins.get(linkId);

			Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
			if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
			Double timeWithinCurrentInterval = -event.getTime()+currentTimeBin;

			// time interval of activity
			double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
			prevDuration = prevDuration + timeWithinCurrentInterval;
			duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
			currentTimeBin += timeBinSize;

			// later time intervals
			while(currentTimeBin <= simulationEndTime){
				double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
				prevDurationL = prevDurationL + timeBinSize;
				duration.get(currentTimeBin)[xCell][yCell]=prevDurationL;
				currentTimeBin += timeBinSize;
			}
		}
	}

	public SortedMap<Double, Double[][]> getDuration() {
		return duration;
	}
}
