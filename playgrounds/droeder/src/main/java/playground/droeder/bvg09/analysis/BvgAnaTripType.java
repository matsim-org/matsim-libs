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
package playground.droeder.bvg09.analysis;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public final class BvgAnaTripType {
	
	private final String b = "Berlin";
	private final String brb = "Brandenburg";
	private final String b2brb = "Berlin-Brandenburg";
	
	private Geometry g;

	public BvgAnaTripType (Geometry g){
		this.g = g;
	}
	
	public String getTripLocation(AnaTrip trip){
		if(g.contains(trip.getStart()) && g.contains(trip.getEnd())){
			return this.b;
		}else if(!g.contains(trip.getStart()) && g.contains(trip.getEnd())){
			return this.b2brb;
		}else if(g.contains(trip.getStart()) && !g.contains(trip.getEnd())){
			return this.b2brb;
		}else {
			return this.brb;
		}
	}
}
