/* *********************************************************************** *
 * project: org.matsim.*
 * MyCell.java
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

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Envelope;

public class MyGridCell extends Envelope{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int id;
	private double count;
	private ArrayList<Double> hourCount;

	public ArrayList<Double> getHourCount() {
		return hourCount;
	}

	public MyGridCell(int id, double xMin, double xMax, double yMin, double yMax, Integer numberOfTimeBins){
		super(xMin, xMax, yMin, yMax);
		this.id = id;
		this.count = 0;
		if(numberOfTimeBins != null){
			this.setHourlyCounts(numberOfTimeBins);
		} else{
			hourCount = null;
		}
	}

	public int getId() {
		return id;
	}

	private void setHourlyCounts(int hourBins){
		hourCount = new ArrayList<Double>(hourBins);
		for(int i = 0; i < hourBins; i++){
			hourCount.add(Double.valueOf(0.0));
		}
	}
	
	public double getCount() {
		return count;
	}
	
	public void addToTotalCount(double value){
		this.count += value;
	}
	
	public void addToHourCount(int hour, double value){
		Double oldValue = this.hourCount.get(hour);
		this.hourCount.set(hour, oldValue + value);
	}
	
	public boolean equals(MyGridCell mgc){
		return super.equals(mgc);
	}

}
