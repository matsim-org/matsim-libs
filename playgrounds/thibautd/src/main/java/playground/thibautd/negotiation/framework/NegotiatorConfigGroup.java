/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class NegotiatorConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "negotiator";

	private int rollingAverageWindow = 100;
	private double improvingFractionThreshold = 0.01;
	private double maxRoundsPerAgent = 100;
	private boolean logStopwatch = false;

	public NegotiatorConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter("rollingAverageWindow")
	public int getRollingAverageWindow() {
		return rollingAverageWindow;
	}

	@StringSetter("rollingAverageWindow")
	public void setRollingAverageWindow( final int rollingAverageWindow ) {
		if ( rollingAverageWindow < 1 ) throw new IllegalArgumentException( "rolling average window must be strictly positive, got "+rollingAverageWindow );
		this.rollingAverageWindow = rollingAverageWindow;
	}

	@StringGetter("improvingFractionThreshold")
	public double getImprovingFractionThreshold() {
		return improvingFractionThreshold;
	}

	@StringSetter("improvingFractionThreshold")
	public void setImprovingFractionThreshold( final double improvingFractionThreshold ) {
		if ( improvingFractionThreshold < 0 || improvingFractionThreshold > 1 ) throw new IllegalArgumentException( "improving fraction threshold must be in [0,1], got "+improvingFractionThreshold );
		this.improvingFractionThreshold = improvingFractionThreshold;
	}

	@StringGetter("logStopwatch")
	public boolean isLogStopwatch() {
		return logStopwatch;
	}

	@StringSetter("logStopwatch")
	public void setLogStopwatch( final boolean logStopwatch ) {
		this.logStopwatch = logStopwatch;
	}

	@StringGetter("maxRoundsPerAgent")
	public double getMaxRoundsPerAgent() {
		return maxRoundsPerAgent;
	}

	@StringSetter("maxRoundsPerAgent")
	public void setMaxRoundsPerAgent( final double maxRoundsPerAgent ) {
		this.maxRoundsPerAgent = maxRoundsPerAgent;
	}
}
