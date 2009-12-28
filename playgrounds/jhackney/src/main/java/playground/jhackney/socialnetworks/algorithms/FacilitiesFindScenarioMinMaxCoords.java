/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesFindMinMaxCoords.java
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

package playground.jhackney.socialnetworks.algorithms;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.geometry.CoordImpl;

public class FacilitiesFindScenarioMinMaxCoords {

	private Coord minCoord;
	private Coord maxCoord;

	public FacilitiesFindScenarioMinMaxCoords() {
		super();
	}

	public void run(ActivityFacilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
//		System.out.println("  NOTE you could get these limits from world");

		double min_x = Double.MAX_VALUE;
		double min_y = Double.MAX_VALUE;
		double max_x = Double.MIN_VALUE;
		double max_y = Double.MIN_VALUE;

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			if(c.getX()<=min_x){
				min_x=c.getX();
			}
			if(c.getY()<=min_y){
				min_y=c.getY();
			}
			if(c.getX()>=max_x){
				max_x=c.getX();
			}
			if(c.getY()>=max_y){
				max_y=c.getY();
			}
		}
		minCoord = new CoordImpl(min_x, min_y);
		maxCoord = new CoordImpl(max_x, max_y);
	}
	public Coord getMinCoord(){
		return this.minCoord;
	}
	public Coord getMaxCoord(){
		return this.maxCoord;
	}
}
