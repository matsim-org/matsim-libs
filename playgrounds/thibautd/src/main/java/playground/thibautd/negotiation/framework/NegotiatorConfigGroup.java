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
	private static final String GROUP_NAME = "negociator";

	private int rollingAverageWindow = 100;
	private double improvingFractionThreshold = 0.01;

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
}
