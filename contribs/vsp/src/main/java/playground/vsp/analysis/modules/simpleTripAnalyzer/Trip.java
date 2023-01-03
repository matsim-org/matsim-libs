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
package playground.vsp.analysis.modules.simpleTripAnalyzer;

import org.matsim.api.core.v01.Coord;

/**
 * @author droeder
 *
 */
public class Trip{
	
	static final String HEADER = "mode;start;end;beeline;dist;fromX;fromY;toX;toY;";
	public Double beeline;
	Double dist = null; 
	Double start = Double.NaN;
	Double end = Double.NaN;
	String mode = null;
	Coord from,to;
	boolean stuck = false;
	
	Trip(){
	}
	
	public double getDuration(){
		return (end - start);
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		b.append(mode + ";");
		b.append(start + ";");
		b.append(end + ";");
		b.append(beeline + ";");
		b.append(dist + ";");
		b.append((from == null) ? null : from.getX() + ";");
		b.append((from == null) ? null : from.getY() + ";");
		b.append((to == null) ? null : to.getX() + ";");
		b.append((to == null) ? null : to.getY() + ";");
		return b.toString();
	}

	/**
	 * @return
	 */
	public final String getMode() {
		return mode;
	}

	/**
	 * @return
	 */
	public final Double getDist() {
		return dist;
	}
	
}

