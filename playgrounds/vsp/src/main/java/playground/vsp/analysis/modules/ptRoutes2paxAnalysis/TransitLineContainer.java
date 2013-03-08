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
package playground.vsp.analysis.modules.ptRoutes2paxAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * @author droeder
 *
 */
public class TransitLineContainer {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitLineContainer.class);
	private Id id;
	private Map<Id, TransitRouteContainer> routeContainer;

	public TransitLineContainer(TransitLine l, double countsInterval, int maxSlice) {
		this.id = l.getId();
		this.routeContainer = getRouteContainer(l, countsInterval, maxSlice);
	}
	
	public Id getId(){
		return this.id;
	}

	/**
	 * @param l
	 * @return
	 */
	private Map<Id, TransitRouteContainer> getRouteContainer(TransitLine l, double countsInterval, int maxSlice) {
		Map<Id, TransitRouteContainer> rc = new HashMap<Id, TransitRouteContainer>();
		for(TransitRoute r: l.getRoutes().values()){
			rc.put(r.getId(), new TransitRouteContainer(r, countsInterval, maxSlice));
		}
		return rc;
	}
	
	public void paxBoarding(Id routeId, Id facilityId, double time){
		this.routeContainer.get(routeId).paxBoarding(facilityId, time);
	}
	
	public void paxAlighting(Id routeId, Id facilityId, double time){
		this.routeContainer.get(routeId).paxAlighting(facilityId, time);
	}
	
	public void vehicleDeparts(double time, double vehCapacity, double nrSeatsInUse, Id facilityId, Id routeId){
		this.routeContainer.get(routeId).vehicleDeparts(time, vehCapacity, nrSeatsInUse, facilityId);
	}
	
	public Map<Id, TransitRouteContainer> getTransitRouteContainer(){
		return this.routeContainer;
	}
}

