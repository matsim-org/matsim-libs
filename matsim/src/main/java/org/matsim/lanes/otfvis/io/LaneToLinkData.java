/* *********************************************************************** *
 * project: org.matsim.*
 * LaneToLinkData
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
package org.matsim.lanes.otfvis.io;

import java.awt.geom.Point2D;


/**
 * @author dgrether
 *
 */
public class LaneToLinkData{

	private Point2D.Double startPoint;
	private double numberOfLanes;
	private Point2D.Double normal;

	public LaneToLinkData(Point2D.Double startPoint, Point2D.Double normal, double toLinkNumberOfLanes) {
		this.startPoint = startPoint;
		this.normal = normal;
		this.numberOfLanes = toLinkNumberOfLanes;
	}
	
	public Point2D.Double getStartPoint(){
		return this.startPoint;
	}
	
	public Point2D.Double getNormalVector(){
		return this.normal;
	}
	
	public double getNumberOfLanes(){
		return this.numberOfLanes;
	}

}
