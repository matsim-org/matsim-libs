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

package playground.benjamin.utils.spatialAvg;

import org.matsim.api.core.v01.Coord;

public class Cell {
	
	public Cell(int x, int y, Coord cellCentroid) {
		this.xCell=x;
		this.yCell=y;
		this.cellCentroid=cellCentroid;
		this.weight=0.0;
		this.weightedValue=0.0;
	}
	private int xCell;
	private int yCell;
	private Coord cellCentroid;
	private Double weightedValue;
	private Double weight;
	
	public Coord getCentroid() {
		return cellCentroid;
	}
	public Double getWeightedValue() {
		return this.weightedValue;
	}
	public Double getWeight() {
		return this.weight;
	}
	public void addWeight(Double weight) {
		this.weight+= weight;
	}
	public void addWeightedValue(Double weightedValue) {
		this.weightedValue+=weightedValue;
		
	}
	public Double getAverageValue() {
		if(weight>0.0)return weightedValue/weight;
		return 0.0;
	}
	public void multiplyAllValues(Double normalizationFactor) {
		this.weight=this.weight*normalizationFactor;
		this.weightedValue=this.weightedValue*normalizationFactor;
	}
	public int getXNumber() {
		return this.xCell;
	}
	
	public int getYNumber(){
		return this.yCell;
	}
	

}
