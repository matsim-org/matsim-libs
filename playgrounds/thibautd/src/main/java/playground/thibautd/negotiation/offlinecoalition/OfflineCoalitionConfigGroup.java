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
package playground.thibautd.negotiation.offlinecoalition;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class OfflineCoalitionConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "offlineCoalition";

	private int iterations = 10;
	// slow and causes problems when some agents get all their plans removed
	private int removalPeriod = Integer.MAX_VALUE;

	public OfflineCoalitionConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter("iterations")
	public int getIterations() {
		return iterations;
	}

	@StringSetter("iterations")
	public void setIterations( final int iterations ) {
		this.iterations = iterations;
	}

	@StringGetter("removalPeriod")
	public int getRemovalPeriod() {
		return removalPeriod;
	}

	@StringSetter("removalPeriod")
	public void setRemovalPeriod( final int removalPeriod ) {
		this.removalPeriod = removalPeriod;
	}
}
