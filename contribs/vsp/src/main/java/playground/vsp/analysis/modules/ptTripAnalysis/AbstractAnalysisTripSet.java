/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptTripAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public abstract class AbstractAnalysisTripSet {
	private static final Logger log = LogManager.getLogger(AbstractAnalysisTripSet.class);
	
	private String mode;
	private Geometry zone;
	

	public AbstractAnalysisTripSet(String mode, Geometry zone){
		this.mode = mode;
		this.zone = zone;
	}
	
	
	public String getMode(){
		return this.mode;
	}
	/**
	 * [0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
	 * @param trip
	 * @return
	 */
	public Integer getTripLocation(AbstractAnalysisTrip trip){
		if(this.zone == null){
			return 0;
		}else if(this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 0;
		}else if(this.zone.contains(trip.getStart()) && !this.zone.contains(trip.getEnd())){
			return 1;
		}else if(!this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 2;
		}else {
			return 3;
		}
	}
	
	
	/**
	 * calls addTripValues()
	 * @param trip
	 */
	public void addTrip(AbstractAnalysisTrip trip) {
		if(trip.getMode().equals(this.mode)){
			this.addTripValues(trip);
		}else{ 
			//can only happen if AnalysisTripSetAllMode is not used
			log.error("wrong tripMode for TripSet");
		}
	}
	
	protected abstract void addTripValues(AbstractAnalysisTrip trip);
	
	/**
	 * appends the given array to the <code> StringBuffer</code> in one line, separates it by semicolon and begins a new Line
	 * @param d
	 * @param b
	 */
	public void println(Number[] d, StringBuffer b){
		for(int i = 0; i< d.length; i++){
			b.append(";" + String.valueOf(d[i]));
		}
		b.append("\n");
	}
}
