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
package playground.thibautd.socnetsimusages.traveltimeequity;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Set;

/**
 * @author thibautd
 */
public class EquityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "equityScoring";

	private double betaStandardDev = 1;
	// or not? can come form joint scoring...
	//private Set<String> activityTypes;
	//private boolean importActivityTypesFromScoring;

	public EquityConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "betaStandardDev" )
	public double getBetaStandardDev() {
		return betaStandardDev;
	}

	@StringGetter( "betaStandardDev" )
	public void setBetaStandardDev(double betaStandardDev) {
		this.betaStandardDev = betaStandardDev;
	}
}
