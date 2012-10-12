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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class Cell<T>
{
	private double timeSum;
	private int count;
	private T data;
	private List<Double> arrivalTimes;
	private List<Double> linkLeaveTimes;
	private List<Double> linkEnterTimes;
	private CoordImpl coord;
	
	public static String CELLSIZE = "cellsize";
	
	public Cell(T data)
	{
		this.data = data;
		this.linkLeaveTimes = new ArrayList<Double>();
		this.linkEnterTimes = new ArrayList<Double>();
		this.arrivalTimes = new ArrayList<Double>();
	}
	
	
	public double getTimeSum() {
		return timeSum;
	}
	
	public void setTimeSum(double timeSum) {
		this.timeSum = timeSum;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}

	public void incrementCount()
	{
		this.count++;
	}
	
	public T getData() {
		return data;
	}
	
	public void setData(T data) {
		this.data = data;
	}
	
	public void setArrivalTimes(List<Double> arrivalTimes) {
		this.arrivalTimes = arrivalTimes;
	}
	
	public List<Double> getArrivalTimes() {
		return arrivalTimes;
	}
	
	public List<Double> getLinkEnterTimes() {
		return linkEnterTimes;
	}
	
	public List<Double> getLinkLeaveTimes() {
		return linkLeaveTimes;
	}
	
	public void setLinkEnterTimes(List<Double> linkEnterTimes) {
		this.linkEnterTimes = linkEnterTimes;
	}
	
	public void setLinkLeaveTimes(List<Double> linkLeaveTimes) {
		this.linkLeaveTimes = linkLeaveTimes;
	}
	
	public void addLinkEnterTime(Double time)
	{
		if (this.linkEnterTimes==null)
			this.linkEnterTimes = new ArrayList<Double>();
		this.linkEnterTimes.add(time);
	}
	
	public void addLinkLeaveTime(Double time)
	{
		if (this.linkLeaveTimes==null)
			this.linkLeaveTimes = new ArrayList<Double>();
		this.linkLeaveTimes.add(time);
	}
	
	public void setCoord(CoordImpl centroid) {
		this.coord = centroid;
	}
	
	public CoordImpl getCoord() {
		return coord;
	}

}
