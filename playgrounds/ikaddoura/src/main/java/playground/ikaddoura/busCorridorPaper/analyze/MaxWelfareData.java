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
public class MaxWelfareData {

	private int runNr;
	private int maxWelfareNumberOfBuses;
	private double maxWelfareFare;
	private double maxWelfare;
	
	public int getMaxWelfareNumberOfBuses() {
		return maxWelfareNumberOfBuses;
	}
	public void setMaxWelfareNumberOfBuses(int maxWelfareNumberOfBuses) {
		this.maxWelfareNumberOfBuses = maxWelfareNumberOfBuses;
	}
	public double getMaxWelfareFare() {
		return maxWelfareFare;
	}
	public void setMaxWelfareFare(double maxWelfareFare) {
		this.maxWelfareFare = maxWelfareFare;
	}
	public double getMaxWelfare() {
		return maxWelfare;
	}
	public void setMaxWelfare(double maxWelfare) {
		this.maxWelfare = maxWelfare;
	}
	public int getRunNr() {
		return runNr;
	}
	public void setRunNr(int runNr) {
		this.runNr = runNr;
	}
	
	
}
