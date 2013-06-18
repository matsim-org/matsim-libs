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
package org.matsim.contrib.matsim4urbansim.utils.helperobjects;

/**
 * @author thomas
 *
 */
public class Distances{
	
	/** This stores either the (i) orthogonal distance between 
	 *  a measure point and the nearest link or (ii) the euclidian 
	 *  distance between the measuring point and the nearest node
	 */
	private double distancePoint2Road = 0.;
	/** This stores the (i) distance between the link intersection 
	 * (of a orthogonal projection) and the "nearest" node. 
	 * Otherwise (ii), in case of an euclidian distance measure, this is 0.
	 */
	private double distanceRoad2Node = 0.;
	
	/**
	 * Either an orthogonal distance between 
	 * a measure point and the nearest link or 
	 * the euclidian distance between the measuring 
	 * point and the nearest node
	 * @param value
	 */
	public void setDisatancePoint2Road(double value){
		this.distancePoint2Road = value;
	}
	
	/**
	 * The distance between the link intersection 
	 * (of a orthogonal projection) and the "nearest" 
	 * node. 
	 * @param value
	 */
	public void setDistanceRoad2Node(double value){
		this.distanceRoad2Node = value;
	}
	
	/**
	 * Either an orthogonal distance between 
	 * a measure point and the nearest link or 
	 * the euclidian distance between the measuring 
	 * point and the nearest node
	 * @return
	 */
	public double getDistancePoint2Road(){
		return this.distancePoint2Road;
	}
	
	/**
	 * The distance between the link intersection 
	 * (of a orthogonal projection) and the "nearest" 
	 * node. 
	 * @return
	 */
	public double getDistanceRoad2Node(){
		return this.distanceRoad2Node;
	}
}