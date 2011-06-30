/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.P2.schedule;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Own TransitSchedule implementation (delegate) - Should be needless
 * 
 * @author aneumann
 *
 */
public class PTransitSchedule implements TransitSchedule{

	private final static Logger log = Logger.getLogger(PTransitSchedule.class);
	private TransitSchedule transitSchedule;	
	
	public PTransitSchedule(TransitSchedule transitSchedule) {
		log.warn("check");
		this.transitSchedule = transitSchedule;
	}

	@Override
	public void addTransitLine(TransitLine line) {
		this.transitSchedule.addTransitLine(line);		
	}

	@Override
	public void addStopFacility(TransitStopFacility stop) {
		this.transitSchedule.addStopFacility(stop);		
	}

	@Override
	public Map<Id, TransitLine> getTransitLines() {
		return this.transitSchedule.getTransitLines();
	}

	@Override
	public Map<Id, TransitStopFacility> getFacilities() {
		return this.transitSchedule.getFacilities();
	}

	@Override
	public TransitScheduleFactory getFactory() {
		return this.transitSchedule.getFactory();
	}

	// remove paratransit lines from schedule via  clone method
	
	// add all additional stops from network
	
}
