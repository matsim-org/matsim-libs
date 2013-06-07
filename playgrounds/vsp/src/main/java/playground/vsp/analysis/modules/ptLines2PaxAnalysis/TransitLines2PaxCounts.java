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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * @author sfuerbas (parts taken from droeder)
 *
 */

public class TransitLines2PaxCounts {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitLines2PaxCounts.class);
	private Double interval;
	private Id id;
	private Integer maxSlice;
	private Counts boarding;
	private Counts alighting;
	private Counts capacity;
	private Counts totalPax;
	private Counts occupancy;
	
	public TransitLines2PaxCounts (TransitLine tl, double countsInterval, int maxSlice) {
		this.id = tl.getId();
		this.boarding = new Counts();
		this.alighting = new Counts();
		this.capacity = new Counts();
		this.totalPax = new Counts();
		this.occupancy = new Counts();
		this.interval = countsInterval;
		this.maxSlice = maxSlice;
		for (TransitRoute tr : tl.getRoutes().values()) {
			for (int ii=0; ii < tr.getStops().size(); ii++) {
				TransitRouteStop s = tr.getStops().get(ii);
				Id stopFacilId = s.getStopFacility().getId();
				if (this.boarding.getCounts().get(stopFacilId)==null) {
					this.boarding.createCount(stopFacilId, stopFacilId.toString());
					this.alighting.createCount(stopFacilId, stopFacilId.toString());
					this.capacity.createCount(stopFacilId, stopFacilId.toString());
					this.totalPax.createCount(stopFacilId, stopFacilId.toString());
					this.occupancy.createCount(stopFacilId, stopFacilId.toString());
				}
			}
		}
	}
	
	
	

}
