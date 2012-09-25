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

public class Cell<T>
{
	private double timeSum;
	private int count;
	private T data;
	private List<Double> arrivalTimes;
	
	public static String CELLSIZE = "cellsize";
	
	public Cell(T data)
	{
		this.data = data;
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

}
