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
package playground.vsp.analysis.modules.transitVehicleVolume;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class TransitVehicleVolumeHandler implements TransitDriverStartsEventHandler, LinkEnterEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitVehicleVolumeHandler.class);
	private Map<Id, String> vehId2mode;
	private TransitSchedule sched;
	private HashMap<String, Counts<Link>> mode2Counts;
	private Integer maxSlice = 0;
	private Double interval;

	public TransitVehicleVolumeHandler(TransitSchedule sched, Double interval) {
		this.vehId2mode = new HashMap<Id, String>();
		this.sched = sched;
		this.mode2Counts = new HashMap<>();
		this.interval = interval;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		//handle only pt-Vehicles!
		if(this.vehId2mode.containsKey(event.getVehicleId())){
			//create the count
			Count count = this.mode2Counts.get(vehId2mode.get(event.getVehicleId())).
					createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			if(count == null){
				//or get the old one if there is one
				count = this.mode2Counts.get(vehId2mode.
						get(event.getVehicleId())).getCount(event.getLinkId());
			}else{
				//we always want to start with hour one
				count.createVolume(1, 0);
			}
			this.increase(count, event.getTime());
		}
	}
		
	private void increase(Count count, Double time){
		Integer slice = (int) (time / this.interval) + 1;
		if(slice > this.maxSlice){
			this.maxSlice = slice;
		}
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + 1);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(!this.sched.getTransitLines().containsKey(event.getTransitLineId())) return;
		TransitLine line = this.sched.getTransitLines().get(event.getTransitLineId());
		if(line == null )log.debug(event.getTransitLineId());
		TransitRoute route = line.getRoutes().get(event.getTransitRouteId());
		if(route == null) {
			log.debug("route " + event.getTransitRouteId() + " is null on TransitLine " + event.getTransitLineId()); 
			return;
		}
		String mode = route.getTransportMode();
//		String mode = this.sched.getTransitLines().get(event.getTransitLineId()).
//				getRoutes().get(event.getTransitRouteId()).getTransportMode();
		this.vehId2mode.put(event.getVehicleId(), mode);
		if(!this.mode2Counts.containsKey(mode)){
			this.mode2Counts.put(mode, new Counts());
		}
	}
	
	public Map<String, Counts<Link>> getMode2Counts(){
		return this.mode2Counts;
	}

	/**
	 * @return
	 */
	public int getMaxTimeSlice() {
		return this.maxSlice;
	}
}

