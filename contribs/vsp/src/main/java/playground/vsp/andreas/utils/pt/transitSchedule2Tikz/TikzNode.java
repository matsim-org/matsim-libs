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
package playground.vsp.andreas.utils.pt.transitSchedule2Tikz;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
public class TikzNode {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TikzNode.class);
	private Coord coord;
	private Id<TikzNode> id;

	public TikzNode(TransitStopFacility f, int i) {
		this.coord = f.getCoord();
//		this.id = new IdImpl(f.getId().toString().replaceAll("_", ""));
		this.id = Id.create(i, TikzNode.class);
	}
	
	public Coord getCoord(){
		return this.coord;
	}
	
	public String getTikzString(String styleId){
		return ("\\node [" + styleId + "] (" + this.id.toString() + 
				") at (" + this.coord.getX() + "," + this.coord.getY() +") {};");	
	}

	public Id<TikzNode> getId() {
		return this.id;
	}

	/**
	 * @param xOffset
	 * @param yOffset
	 * @param scale 
	 * @return 
	 */
	public void offset(Double xOffset, Double yOffset, Double scale) {
		this.coord = new Coord((this.coord.getX() + xOffset) * scale, (this.coord.getY() + yOffset) * scale);
	}
}

