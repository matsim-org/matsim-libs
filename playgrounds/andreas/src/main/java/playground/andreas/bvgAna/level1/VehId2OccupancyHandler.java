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

package playground.andreas.bvgAna.level1;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import playground.mrieser.pt.analysis.TransitLoadByTime;

/**
 * Wrapper class, should be replaced by original one or substituted by <code>TransitLoad</code>
 * 
 * @author aneumann
 *
 */
public class VehId2OccupancyHandler extends TransitLoadByTime{
	
	private final Logger log = Logger.getLogger(VehId2OccupancyHandler.class);
	private final Level logLevel = Level.DEBUG;	
	
	public VehId2OccupancyHandler(){
		this.log.setLevel(this.logLevel);
	}
}
