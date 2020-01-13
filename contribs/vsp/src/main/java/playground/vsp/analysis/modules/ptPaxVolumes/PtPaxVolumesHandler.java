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

package playground.vsp.analysis.modules.ptPaxVolumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

/** 
 * Counts the number of passenger of public transport vehicles per link and time slice
 * 
 * @author droeder
 *
 */
public class PtPaxVolumesHandler implements LinkEnterEventHandler, 
									PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
									TransitDriverStartsEventHandler{
	
	private static final Logger log = Logger.getLogger(PtPaxVolumesHandler.class);
	
	private HashMap<Id, Counts> linkId2LineCounts;

	private List<Id> drivers;

	private Map<Id, CountVehicle> transitVehicles;

	private Integer maxSlice = 24;;

	private Double interval;

	public PtPaxVolumesHandler(Double interval) {
		this.linkId2LineCounts = new HashMap<Id, Counts>();
		this.drivers = new ArrayList<Id>();
		this.transitVehicles = new HashMap<Id, PtPaxVolumesHandler.CountVehicle>();
		this.interval = interval;
	}
	

	public double getPaxCountForLinkId(Id linkId){
		// check if we have a count for the link
		Counts<Link> counts = this.linkId2LineCounts.get(linkId);
		if(counts == null) return 0;
		int count = 0;
		// sum up all count-values
		for(Count<Link> c: counts.getCounts().values()){
			for(Volume v: c.getVolumes().values()){
				count += v.getValue();
			}
		}
		return count;
	}
	
	public double getPaxCountForLinkId(Id linkId, int interval){
		// check if we have a count for the link
		Counts<Link> counts = this.linkId2LineCounts.get(linkId);
		if(counts == null) return 0;
		int count = 0;
		// sum up all count-values
		for(Count c: counts.getCounts().values()){
			if(c.getVolumes().containsKey(interval)){
				count += c.getVolume(interval).getValue();
			}
		}
		return count;
	}
	
	public double getPaxCountForLinkId(Id linkId, Id lineId){
		// check if we have a count for the link
		Counts counts = this.linkId2LineCounts.get(linkId);
		if(counts == null) return 0;
		// check if ther is a count for the line
		Count<Link> c = counts.getCount(lineId);
		if(c == null) return 0;
		// sum up all volumes
		int count = 0;
		for(Volume v: c.getVolumes().values()){
			count += v.getValue();
		}
		return count;
	}
	
	public double getPaxCountForLinkId(Id linkId, Id lineId, int interval){
		// check if we have a count for the link
		Counts counts = this.linkId2LineCounts.get(linkId);
		if(counts == null) return 0;
		// check if there is a count for the line
		Count c = counts.getCount(lineId);
		if(c == null) return 0;
		// check if there is a volume
		Volume v = c.getVolume(interval);
		if(v == null) return 0;
		return (int) v.getValue();
	}

	@Override
	public void reset(int iteration) {
		//do nothing
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.transitVehicles.keySet().contains(event.getVehicleId())){
			//get the count Vehicle
			CountVehicle v = this.transitVehicles.get(event.getVehicleId());
			// check if we already have a count for this link
			Counts counts = this.linkId2LineCounts.get(event.getLinkId());
			Count c;
			if(counts == null){
				counts = new Counts();
			}
			c = counts.createAndAddCount(v.getLineId(), v.getLineId().toString());
			if(c == null){
				c = counts.getCount(v.getLineId());
			}
			this.increase(c, v, event.getTime());
			this.linkId2LineCounts.put(event.getLinkId(), counts);
		}
	}
	
	private void increase(Count c, CountVehicle cv, Double time){
		Integer slice = (int) (time / this.interval) + 1;
		if(slice > this.maxSlice){
			this.maxSlice = slice;
		}
		Volume v;
		if(c.getVolumes().containsKey(slice)){
			v = c.getVolume(slice);
		}else{
			v = c.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + cv.getCnt());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non pt-vehicle vehicle and every driver
		if(this.transitVehicles.keySet().contains(event.getVehicleId())){
			if(!this.drivers.contains(event.getPersonId())){
				this.transitVehicles.get(event.getVehicleId()).board();
			}
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// subtract a passenger to the vehicle counts data, but ignore every non pt-vehicle vehicle and every driver
		if(this.transitVehicles.keySet().contains(event.getVehicleId())){
			if(!this.drivers.contains(event.getPersonId())){
				this.transitVehicles.get(event.getVehicleId()).alight();
			}
		}		
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.drivers.add(event.getDriverId());
		this.transitVehicles.put(event.getVehicleId(), new CountVehicle(event.getTransitLineId()));
	}
	
	public int getMaxInterval(){
		return this.maxSlice;
	}
	
	private class CountVehicle{
		private int cnt = 0;
		private Id lineId;
		
		public CountVehicle(Id lineId){
			this.lineId = lineId;
		}
		
		public Id getLineId(){
			return this.lineId;
		}
		
		public void board(){
			this.cnt++;
		}
		
		public void alight(){
			this.cnt--;
		}
		
		public int getCnt(){
			return this.cnt;
		}
	}
}