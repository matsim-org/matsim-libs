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
package playground.ikaddoura.optimization.io;

/**
 * 
 * @author ikaddoura
 *
 */
public class OptSettings {
	
	private int incrBusNumber;
	private double incrFare;
	private int incrCapacity;
	private int startBusNumber;
	private double startFare;
	private int startCapacity;
	private long randomSeed;
	private int stepsBusNumber;
	private int stepsFare;
	private int stepsCapacity;

	public int getIncrBusNumber() {
		return incrBusNumber;
	}
	public void setIncrBusNumber(int incrBusNumber) {
		this.incrBusNumber = incrBusNumber;
	}
	public double getIncrFare() {
		return incrFare;
	}
	public void setIncrFare(double incrFare) {
		this.incrFare = incrFare;
	}
	public int getIncrCapacity() {
		return incrCapacity;
	}
	public void setIncrCapacity(int incrCapacity) {
		this.incrCapacity = incrCapacity;
	}
	public int getStartBusNumber() {
		return startBusNumber;
	}
	public void setStartBusNumber(int startBusNumber) {
		this.startBusNumber = startBusNumber;
	}
	public double getStartFare() {
		return startFare;
	}
	public void setStartFare(double startFare) {
		this.startFare = startFare;
	}
	public int getStartCapacity() {
		return startCapacity;
	}
	public void setStartCapacity(int startCapacity) {
		this.startCapacity = startCapacity;
	}
	public long getRandomSeed() {
		return randomSeed;
	}
	public void setRandomSeed(long l) {
		this.randomSeed = l;
	}
	public int getStepsBusNumber() {
		return stepsBusNumber;
	}
	public void setStepsBusNumber(int stepsBusNumber) {
		this.stepsBusNumber = stepsBusNumber;
	}
	public int getStepsFare() {
		return stepsFare;
	}
	public void setStepsFare(int stepsFare) {
		this.stepsFare = stepsFare;
	}
	public int getStepsCapacity() {
		return stepsCapacity;
	}
	public void setStepsCapacity(int stepsCapacity) {
		this.stepsCapacity = stepsCapacity;
	}

}
