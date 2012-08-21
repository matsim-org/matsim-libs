/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalysisTransitSchedule
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
package air.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * @author dgrether
 *
 */
public class DgAnalysisTransitSchedule {

	private static final Logger log = Logger.getLogger(DgAnalysisTransitSchedule.class);

	private TransitSchedule schedule;

	public DgAnalysisTransitSchedule (TransitSchedule schedule){
		this.schedule = schedule;

	}
	
	public double getScheduledArrival(Id vehicleId, Id stopId){
		/*
		 * <event time="56.0" type="VehicleArrivesAtFacility" vehicle="A90704" facility="DXB" delay="0.0"  />
        <event time="56.0" type="VehicleDepartsAtFacility" vehicle="A90704" facility="DXB" delay="0.0"  />

		 */

		return 0.0;
	}
	
	public double getScheduledDeparture(Id vehicleId, Id stopId){
		return 0.0;
	}
	
	
}
