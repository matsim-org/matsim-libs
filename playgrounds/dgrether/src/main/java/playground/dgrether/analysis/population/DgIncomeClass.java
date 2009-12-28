/* *********************************************************************** *
 * project: org.matsim.*
 * DgIncomeClass
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
package playground.dgrether.analysis.population;


/**
 * @author dgrether
 *
 */
public class DgIncomeClass {
	private double min;
	private double max;
	private String title;
	
	public DgIncomeClass(double min, double max){
		this.min = min; 
		this.max = max;
		this.title = this.min + " - " + this.max;
	}

	
	public String getTitle() {
		return title;
	}

	
	public void setTitle(String title) {
		this.title = title;
	}

	
	public double getMin() {
		return min;
	}

	
	public double getMax() {
		return max;
	}
	
	
	
	
}
