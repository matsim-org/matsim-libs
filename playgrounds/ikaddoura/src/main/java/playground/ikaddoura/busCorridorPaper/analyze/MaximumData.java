/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.analyze;

/**
 * @author ikaddoura
 *
 */
public class MaximumData {

	private int runNr;
	private int maxNumberOfBuses;
	private double maxFare;
	private double maximumValue;
	
	public int getMaxNumberOfBuses() {
		return maxNumberOfBuses;
	}
	public void setMaxNumberOfBuses(int maxWelfareNumberOfBuses) {
		this.maxNumberOfBuses = maxWelfareNumberOfBuses;
	}
	public double getMaxFare() {
		return maxFare;
	}
	public void setMaxFare(double value) {
		this.maxFare = value;
	}
	public double getMaximumValue() {
		return maximumValue;
	}
	public void setMaximumValue(double value) {
		this.maximumValue = value;
	}
	public int getRunNr() {
		return runNr;
	}
	public void setRunNr(int runNr) {
		this.runNr = runNr;
	}

}
