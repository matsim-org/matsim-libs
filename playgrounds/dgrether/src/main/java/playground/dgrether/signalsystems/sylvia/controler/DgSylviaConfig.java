/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaConfig
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
package playground.dgrether.signalsystems.sylvia.controler;


/**
 * @author dgrether
 *
 */
public class DgSylviaConfig {
	
	/**
	 * limits the total amount of available extension time to the cycle time of the corresponding fixed-time signal plan
	 */
	private boolean useFixedTimeCycleAsMaximalExtension = true;
	/**
	 * The distance from the downstream node where sensors are located on a link.
	 */
	private double sensorDistanceMeter= 10.0;
	/**
	 * The maximal green time for each signal group is determined by multiplying this parameter with the green
	 * time of the signal group in the fixed-time signal plan that belongs to the sylvia signal plan.
	 */
	private double signalGroupMaxGreenScale = 1.5;

	/**
	 * currently not used
	 */
	private double gapSeconds = 5.0;
	
	public boolean isUseFixedTimeCycleAsMaximalExtension() {
		return useFixedTimeCycleAsMaximalExtension;
	}

	public void setUseFixedTimeCycleAsMaximalExtension(boolean useMaximalExtension) {
		this.useFixedTimeCycleAsMaximalExtension = useMaximalExtension;
	}

	
	public double getSensorDistanceMeter() {
		return sensorDistanceMeter;
	}

	
	public void setSensorDistanceMeter(double sensorDistanceMeter) {
		this.sensorDistanceMeter = sensorDistanceMeter;
	}

	
	public double getGapSeconds() {
		return gapSeconds;
	}

	
	public void setGapSeconds(double gapSeconds) {
		this.gapSeconds = gapSeconds;
	}

	
	public double getSignalGroupMaxGreenScale() {
		return signalGroupMaxGreenScale;
	}

	
	public void setSignalGroupMaxGreenScale(double maxGreenScale) {
		this.signalGroupMaxGreenScale = maxGreenScale;
	}
	
	
}
