/* *********************************************************************** *
 * project: org.matsim.*
 * JobsObject.java
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.utils.helperObjects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * @author thomas
 *
 */
public class JobsObject {
	
	private Id jobID = null;
	private Id parcelID = null;
	private Id zoneID = null;
	private Coord coordinate = null;
	
	/**
	 * constructor
	 * 
	 * @param jobID
	 * @param parcelID
	 * @param zoneID
	 * @param coord
	 */
	public JobsObject(final Id jobID, final Id parcelID, final Id zoneID, final Coord coord){
		this.jobID 		= jobID;
		this.parcelID 	= parcelID;
		this.zoneID 	= zoneID;
		this.coordinate = coord;
	}
	
	// getter methods
	
	public Id getJobID(){
		return this.jobID;
	}

	public Id getParcelID(){
		return this.parcelID;
	}
	
	public Id getZoneID(){
		return this.zoneID;
	}
	
	public Coord getCoord(){
		return this.coordinate;
	}
	
}

