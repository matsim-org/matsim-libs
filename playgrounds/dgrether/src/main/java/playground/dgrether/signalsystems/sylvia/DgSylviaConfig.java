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
package playground.dgrether.signalsystems.sylvia;


/**
 * @author dgrether
 *
 */
public class DgSylviaConfig {
	
	private boolean useMaximalExtension = true;
	
	private double sensorDistanceMeter= 10.0;
	
	private double gapSeconds = 5.0;
	
	private double signalGroupMaxGreenScale = 1.5;

	
//	public boolean isUseMaximalExtension() {
//		return useMaximalExtension;
//	}
//
//	public void setUseMaximalExtension(boolean useMaximalExtension) {
//		this.useMaximalExtension = useMaximalExtension;
//	}

	
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

	
	public double getMaxGreenScale() {
		return signalGroupMaxGreenScale;
	}

	
	public void setMaxGreenScale(double maxGreenScale) {
		this.signalGroupMaxGreenScale = maxGreenScale;
	}
	
	
}
