/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public final class AccidentsConfigGroup extends ReflectiveConfigGroup {
	// needs to be public because of reflection-based annotations

	public static final String GROUP_NAME = "accidents" ;
	
	public AccidentsConfigGroup() {
		super(GROUP_NAME);
	}
	
	private boolean enableAccidentsModule = true;
	private final String bvwpRoadTypeAttributeName = "bvwpRoadType";
	private final String accidentsComputationMethodAttributeName = "accidentsComputationMethod";
	
	// e.g. 1 for 100%; 10 for 10%; 100 for 1%
	private double sampleSize = 10.; // TODO: rename to scale factor
		
	private AccidentsComputationMethod accidentsComputationMethod = AccidentsComputationMethod.BVWP;
				
	@StringGetter( "enableAccidentsModule" )
	public boolean isEnableAccidentsModule() {
		return enableAccidentsModule;
	}

	@StringSetter( "enableAccidentsModule" )
	public void setEnableAccidentsModule(boolean enableAccidentsModule) {
		this.enableAccidentsModule = enableAccidentsModule;
	}

	@StringGetter( "sampleSize" )
	public double getSampleSize() {
		return sampleSize;
	}

	@StringSetter( "sampleSize" )
	public void setSampleSize(double sampleSize) {
		this.sampleSize = sampleSize;
	}

	@StringGetter( "accidentsComputationMethod" )
	public AccidentsComputationMethod getAccidentsComputationMethod() {
		return accidentsComputationMethod;
	}

	@StringSetter( "accidentsComputationMethod" )
	public void AccidentsComputationMethod(AccidentsComputationMethod accidentsComputationMethod) {
		this.accidentsComputationMethod = accidentsComputationMethod;
	}

	public String getBvwpRoadTypeAttributeName() {
		return bvwpRoadTypeAttributeName;
	}

	public String getAccidentsComputationMethodAttributeName() {
		return accidentsComputationMethodAttributeName;
	}

	/**
	* @author mmayobre
	*/

	public enum AccidentsComputationMethod {BVWP}
}

