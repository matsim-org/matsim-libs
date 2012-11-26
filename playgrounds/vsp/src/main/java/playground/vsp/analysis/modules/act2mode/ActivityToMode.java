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
package playground.vsp.analysis.modules.act2mode;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

/**
 * @author droeder
 *
 */
public class ActivityToMode {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ActivityToMode.class);
	private Double time;
	private String mode;
	private String actType;
	private Coord coord;

	public ActivityToMode(String actType, String mode, Double time, Coord coord) {
		this.actType = actType;
		this.mode = mode;
		this.time = time;
		this.coord = coord;
	}

	/**
	 * @return the time
	 */
	public Double getTime() {
		return time;
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @return the actType
	 */
	public String getActType() {
		return actType;
	}
	
	/**
	 * 
	 * @return the coord
	 */
	public Coord getCoord(){
		return this.coord;
	}
	
}

