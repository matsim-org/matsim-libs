/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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


package playground.wdoering.grips.evacuationanalysis;

import java.util.List;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;

public class EventData {
	
	private double cellSize;
	private double timeSum;
	private double maxCellTimeSum;
	private int arrivals;
	private QuadTree<Cell> cellTree;
	private List<Tuple<Double, Integer>> arrivalTimes;
	
	public EventData(QuadTree<Cell> cellTree, double cellSize, double timeSum, double maxCellTimeSum, int arrivals, List<Tuple<Double,Integer>> arrivalTimes) {
		this.cellTree = cellTree;
		this.cellSize = cellSize;
		this.timeSum = timeSum;
		this.maxCellTimeSum = maxCellTimeSum;
		this.arrivals = arrivals;
		this.arrivalTimes = arrivalTimes;
	}
	
	
	public double getCellSize() {
		return cellSize;
	}
	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}
	public double getTimeSum() {
		return timeSum;
	}
	public void setTimeSum(double timeSum) {
		this.timeSum = timeSum;
	}
	public int getArrivals() {
		return arrivals;
	}
	public void setArrivals(int arrivals) {
		this.arrivals = arrivals;
	}
	
	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}
	
	public void setCellTree(QuadTree<Cell> cellTree) {
		this.cellTree = cellTree;
	}
	
	public double getMaxCellTimeSum() {
		return maxCellTimeSum;
	}
	
	public void setMaxCellTimeSum(double maxCellTimeSum) {
		this.maxCellTimeSum = maxCellTimeSum;
	}
	
	public List<Tuple<Double, Integer>> getArrivalTimes() {
		return arrivalTimes;
	}
	
	public void setArrivalTimes(List<Tuple<Double, Integer>> arrivalTimes) {
		this.arrivalTimes = arrivalTimes;
	}

}
