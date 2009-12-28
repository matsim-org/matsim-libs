/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityPoint.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.KernelDensityEstimation;

import com.vividsolutions.jts.geom.Point;

public class ActivityPoint{
	
	private Point point;
	private int hour;
	
	public ActivityPoint(Point point, int hour){
		this.point = point;
		this.hour = hour;
	}

	public Point getPoint() {
		return point;
	}

	public int getHour() {
		return hour;
	}

}
