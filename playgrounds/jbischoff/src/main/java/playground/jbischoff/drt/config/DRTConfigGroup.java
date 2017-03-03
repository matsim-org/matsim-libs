/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.drt.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DRTConfigGroup extends ReflectiveConfigGroup {

	public static final String DRTMODE = "drt";

	
	public static final String GROUPNAME = "drtConfig";
	private static final String SCHEME = "operationalScheme";
	private static final String TRANSITSCHEDULEFILE = "transitStopFile";
	private static final String WALKDISTANCE = "maximumWalkDistance";
	private static final String SPEEDESTIMATE = "estimatedDRTSpeed";
	private static final String BEELINEDISTANCEESTIMATE = "estimatedBeelineDistanceFactor";
	
	
	private DRTOperationalScheme operationalScheme;			
	private String transitStopFile = null;
	private double maximumWalkDistance;
	private double estimatedSpeed = 25/3.6;
	private double estimatedBeelineDistanceFactor = 1.3;

	
	public enum DRTOperationalScheme {
				stationbased,
				door2door
				};
	
	public DRTConfigGroup() {
		super(GROUPNAME);
	}
	
		
	public static DRTConfigGroup get(Config config)
	{
	        return ConfigUtils.addOrGetModule(config, GROUPNAME, DRTConfigGroup.class);
	} 
	
	
	/**
	 * @return the operationalScheme
	 */
	@StringGetter(SCHEME)
	public DRTOperationalScheme getOperationalScheme() {
		return operationalScheme;
	}
	
	
	/**
	 * @param operationalScheme the operationalScheme to set
	 */
	@StringSetter(SCHEME)
	public void setOperationalScheme(String operationalScheme) {
		
		this.operationalScheme = DRTOperationalScheme.valueOf(operationalScheme);
	}
	
	/**
	 * @return the transitStopFile
	 */
	@StringGetter(TRANSITSCHEDULEFILE)
	public String getTransitStopFile() {
		return transitStopFile;
	}
	
	/**
	 * @param transitStopFile the transitStopFile to set
	 */
	@StringSetter(TRANSITSCHEDULEFILE)
	public void setTransitStopFile(String transitStopFile) {
		this.transitStopFile = transitStopFile;
	}
	
	/**
	 * @return the maximumWalkDistance
	 */
	@StringGetter(WALKDISTANCE)
	public double getMaximumWalkDistance() {
		return maximumWalkDistance;
	}
	
	/**
	 * @param maximumWalkDistance the maximumWalkDistance to set
	 */
	@StringSetter(WALKDISTANCE)
	public void setMaximumWalkDistance(double maximumWalkDistance) {
		this.maximumWalkDistance = maximumWalkDistance;
	}

	/**
	 * @return the estimatedSpeed
	 */
	@StringGetter(SPEEDESTIMATE)
	public double getEstimatedSpeed() {
		return estimatedSpeed;
	}
	
	/**
	 * @param estimatedSpeed the estimatedSpeed to set
	 */
	@StringSetter(SPEEDESTIMATE)
	public void setEstimatedSpeed(double estimatedSpeed) {
		this.estimatedSpeed = estimatedSpeed;
	}
	
	/**
	 * @return the estimatedBeelineDistanceFactor
	 */
	@StringGetter(BEELINEDISTANCEESTIMATE)
	public double getEstimatedBeelineDistanceFactor() {
		return estimatedBeelineDistanceFactor;
	}
	
	/**
	 * @param estimatedBeelineDistanceFactor the estimatedBeelineDistanceFactor to set
	 */
	@StringSetter(BEELINEDISTANCEESTIMATE)
	public void setEstimatedBeelineDistanceFactor(double estimatedBeelineDistanceFactor) {
		this.estimatedBeelineDistanceFactor = estimatedBeelineDistanceFactor;
	}
}
