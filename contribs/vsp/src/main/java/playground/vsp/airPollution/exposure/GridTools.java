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

package playground.vsp.airPollution.exposure;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class GridTools {

	private final Map<Id<Link>, ? extends Link> links;
	private final  Double xMin;
	private final  Double xMax;
	private final  Double yMin;
	private final  Double yMax;
	private final  Integer noOfXCells;
	private final  Integer noOfYCells;
	private final Map<Id<Link>, Integer> link2ybin = new HashMap<>();
	private final Map<Id<Link>, Integer> link2xbin = new HashMap<>();

	public GridTools(Map<Id<Link>, ? extends Link> links, Double xMin, Double xMax,
			Double yMin, Double yMax, Integer noOfXCells, Integer noOfYCells) {
		this.links=links;
		this.xMin=xMin;
		this.xMax=xMax;
		this.yMin=yMin;
		this.yMax=yMax;
		this.noOfXCells = noOfXCells;
		this.noOfYCells = noOfYCells;
		mapLinks2Xcells();
		mapLinks2Ycells();
	}

	// TODO maybe store x and y values only if in area of interest now x might be null but y not
	private void mapLinks2Xcells() {
		for(Id<Link> linkId: this.links.keySet()){
			link2xbin.put(linkId, mapXCoordToBin(this.links.get(linkId).getCoord().getX()));
		}
	}

	private void mapLinks2Ycells() {
		for(Id<Link> linkId: this.links.keySet()){
			link2ybin.put(linkId, mapYCoordToBin(this.links.get(linkId).getCoord().getY()));
		}
	}
	
	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin  || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXCells); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}
	
	private Integer mapYCoordToBin(double yCoord) {

		if (yCoord <= yMin  || yCoord >= yMax) return null; // xCorrd is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYCells); // gives the relative position along the x-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	public Map<Id<Link>, Integer> getLink2XBins(){
		return this.link2xbin;
	}

	public Map<Id<Link>, Integer> getLink2YBins(){
		return this.link2ybin;
	}

	public Integer getNoOfXCells(){
		return this.noOfXCells;
	}

	public Integer getNoOfYCells(){
		return this.noOfYCells;
	}
}

