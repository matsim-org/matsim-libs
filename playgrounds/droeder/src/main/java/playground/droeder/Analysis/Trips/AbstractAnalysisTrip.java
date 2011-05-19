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
package playground.droeder.Analysis.Trips;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author droeder
 *
 */
public abstract class AbstractAnalysisTrip {
	private Coordinate start;
	private Coordinate end;
	private String mode = null;
	
	
	public String getMode(){
		return this.mode;
	}
	
	public void setMode(String mode){
		this.mode = mode;
	}

	public Point getStart() {
		return new GeometryFactory().createPoint(start);
	}

	public void setStart(Coordinate start) {
		this.start = start;
	}

	public Point getEnd() {
		return new GeometryFactory().createPoint(end);
	}

	public void setEnd(Coordinate end) {
		this.end = end;
	}
	
	
}
