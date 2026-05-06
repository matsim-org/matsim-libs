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

	private double scaleFactor = 10.;

	private AccidentsComputationMethod accidentsComputationMethod = AccidentsComputationMethod.BVWP;

	@StringGetter( "enableAccidentsModule" )
	public boolean isEnableAccidentsModule() {
		return enableAccidentsModule;
	}

	@StringSetter( "enableAccidentsModule" )
	public void setEnableAccidentsModule(boolean enableAccidentsModule) {
		this.enableAccidentsModule = enableAccidentsModule;
	}

	@StringGetter( "scaleFactor" )
	public double getScaleFactor() {
		return scaleFactor;
	}

	@StringSetter( "scaleFactor" )
	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	// ---

	public enum AccidentsComputationMethod {BVWP}
	@StringGetter( "accidentsComputationMethod" )
	public AccidentsComputationMethod getAccidentsComputationMethod() {
		return accidentsComputationMethod;
	}

	@StringSetter( "accidentsComputationMethod" )
	public void AccidentsComputationMethod(AccidentsComputationMethod accidentsComputationMethod) {
		this.accidentsComputationMethod = accidentsComputationMethod;
	}

	// ---
	public enum ErrorHandling{ abort, returnZeroAccidentCost, returnFallbackAccidentCost}

	private ErrorHandling errorHandling = ErrorHandling.abort;

	@StringSetter("errorHandling")
	public void setErrorHandling ( ErrorHandling errorHandling ) {
		this.errorHandling = errorHandling;
	}
	@StringGetter( "errorHandling" )
	public ErrorHandling getErrorHandling( ) {
		return this.errorHandling;
	}


}

