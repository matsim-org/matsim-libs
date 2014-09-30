/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.distribution;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class GridTools {

	private Map<Id<Link>, ? extends Link> links;
	private Double xMin;
	private Double xMax;
	private Double yMin;
	private Double yMax;

	public GridTools(Map<Id<Link>, ? extends Link> links, Double xMin, Double xMax,
			Double yMin, Double yMax) {
		this.links=links;
		this.xMin=xMin;
		this.xMax=xMax;
		this.yMin=yMin;
		this.yMax=yMax;
	}

	// TODO maybe store x and y values only if in area of interest now x might be null but y not
	public Map<Id<Link>, Integer> mapLinks2Xcells(Integer noOfXCells) {
		Map<Id<Link>, Integer> link2xbin = new HashMap<>();
		for(Id<Link> linkId: this.links.keySet()){
			link2xbin.put(linkId, mapXCoordToBin(this.links.get(linkId).getCoord().getX(), noOfXCells));
		}
		return link2xbin;
	}

	public Map<Id<Link>, Integer> mapLinks2Ycells(Integer noOfYCells) {
		Map<Id<Link>, Integer> link2ybin = new HashMap<Id<Link>, Integer>();
		for(Id<Link> linkId: this.links.keySet()){
			link2ybin.put(linkId, mapYCoordToBin(this.links.get(linkId).getCoord().getY(), noOfYCells));
		}
		return link2ybin;
	}
	
	private Integer mapXCoordToBin(double xCoord, Integer noOfXCells) {

		if (xCoord <= xMin  || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXCells); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}
	
	private Integer mapYCoordToBin(double yCoord, Integer noOfYCells) {

		if (yCoord <= yMin  || yCoord >= yMax) return null; // xCorrd is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYCells); // gives the relative position along the x-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}	
}

