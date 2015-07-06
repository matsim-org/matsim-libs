/* *********************************************************************** *
 * project: org.matsim.*
 * PieceWiseLinearFareConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.scoring;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class StepBasedFareConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "bikeSharingFare";

	private double stepDuration_min = 30;
	private double stepPrice = 2;
	private double maxTime_min = 2 * 60;
	private double overtimePenalty = 18;

	public StepBasedFareConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();

		map.put( "stepDuration_min" ,
				"The duration of a \"step\" of the fare scheme.\n"+
				"The first step is free, each additional step incurrs a fee, until a maximum limit.\n"+
				"When the maximum limit is reached, a fine is applied." );

		map.put( "stepPrice" ,
				"The fixed price of entering a new time step." );

		map.put( "maxTime_min" ,
				"The maximum time before getting additional charges." );

		map.put( "overtimePenalty" ,
				"The cost of getting over the maximum allowed time." );

		return map;
	}

	@StringGetter( "stepDuration_min" )
	public double getStepDuration_min() {
		return this.stepDuration_min;
	}

	public double getStepDuration_sec() {
		return this.stepDuration_min * 60;
	}

	@StringSetter( "stepDuration_min" )
	public void setStepDuration_min(double stepDuration_min) {
		this.stepDuration_min = stepDuration_min;
	}

	public void setStepDuration_sec(double stepDuration_sec) {
		this.stepDuration_min = stepDuration_sec / 60d;
	}

	@StringGetter( "stepPrice" )
	public double getStepPrice() {
		return this.stepPrice;
	}

	@StringSetter( "stepPrice" )
	public void setStepPrice(double stepPrice) {
		this.stepPrice = stepPrice;
	}

	@StringGetter( "maxTime_min" )
	public double getMaxTime_min() {
		return this.maxTime_min;
	}

	public double getMaxTime_sec() {
		return this.maxTime_min * 60;
	}

	@StringSetter( "maxTime_min" )
	public void setMaxTime_min(double maxTime) {
		this.maxTime_min = maxTime;
	}

	public void setMaxTime_sec(double maxTime_sec) {
		this.maxTime_min = maxTime_sec / 60d;
	}

	@StringGetter( "overtimePenalty" )
	public double getOvertimePenalty() {
		return this.overtimePenalty;
	}

	@StringSetter( "overtimePenalty" )
	public void setOvertimePenalty(double overtimePenalty) {
		this.overtimePenalty = overtimePenalty;
	}
}

