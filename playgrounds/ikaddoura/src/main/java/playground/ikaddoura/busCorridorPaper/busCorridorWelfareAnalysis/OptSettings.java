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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

/**
 * TODO: exception if getting a parameter which is not set.
 * 
 * @author ikaddoura
 *
 */
public class OptSettings {
	
	private String configFile;
	private String outputPath;
	private String optimizationParameter1;
	private String optimizationParameter2;
	private int lastExtIt1;
	private int lastExtIt2;
	private int incrBusNumber;
	private double incrFare;
	private int incrCapacity;
	private int startBusNumber;
	private double startFare;
	private int startCapacity;
	private int randomSeed;
	
	public String getConfigFile() {
		return configFile;
	}
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	public String getOutputPath() {
		return outputPath;
	}
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}
	public String getOptimizationParameter1() {
		return optimizationParameter1;
	}
	public void setOptimizationParameter1(String optimizationParameter1) {
		this.optimizationParameter1 = optimizationParameter1;
	}
	public String getOptimizationParameter2() {
		return optimizationParameter2;
	}
	public void setOptimizationParameter2(String optimizationParameter2) {
		this.optimizationParameter2 = optimizationParameter2;
	}
	public int getLastExtIt1() {
		return lastExtIt1;
	}
	public void setLastExtIt1(int lastExtIt1) {
		this.lastExtIt1 = lastExtIt1;
	}
	public int getLastExtIt2() {
		return lastExtIt2;
	}
	public void setLastExtIt2(int lastExtIt2) {
		this.lastExtIt2 = lastExtIt2;
	}
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
	public int getRandomSeed() {
		return randomSeed;
	}
	public void setRandomSeed(int randomSeed) {
		this.randomSeed = randomSeed;
	}

}
