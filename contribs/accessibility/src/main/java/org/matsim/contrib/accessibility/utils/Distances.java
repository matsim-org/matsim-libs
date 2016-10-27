/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package org.matsim.contrib.accessibility.utils;

/**
 * @author thomas
 */
public final class Distances{
	
	/** This stores either the (i) orthogonal distance between a measure point
	 * and the nearest link or (ii) the Euclidean distance between the measuring
	 * point and the nearest node
	 */
	private double distanceCoord2Intersection = 0.;
	
	/** This stores the (i) distance between the link intersection (of a orthogonal
	 * projection) and the "nearest" node. Otherwise (ii), in case of an Euclidean
	 * distance measure, this is 0.
	 */
	private double distanceIntersection2Node = 0.;
	
	/**
	 * Either an orthogonal distance between a measure point and the nearest link
	 * or the Euclidean distance between the measuring point and the nearest node.
	 * @param value
	 */
	public void setDistanceCoord2Intersection(double value){
		this.distanceCoord2Intersection = value;
	}
	
	/**
	 * The distance between the link intersection (of a orthogonal projection) and
	 * the "nearest" node. 
	 * @param value
	 */
	public void setDistanceIntersetion2Node(double value){
		this.distanceIntersection2Node = value;
	}
	
	/**
	 * Either an orthogonal distance between a measure point and the nearest link or 
	 * the Euclidean distance between the measuring point and the nearest node.
	 * @return
	 */
	public double getDistancePoint2Intersection(){
		return this.distanceCoord2Intersection;
	}
	
	/**
	 * The distance between the link intersection (of a orthogonal projection) and the
	 * "nearest" node. 
	 * @return
	 */
	public double getDistanceIntersection2Node(){
		return this.distanceIntersection2Node;
	}
}