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

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class EquityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "equityScoring";
	private static final Logger log = Logger.getLogger(EquityConfigGroup.class);

	private double betaStandardDev = -1;
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

	@StringSetter( "betaStandardDev" )
	public void setBetaStandardDev(double betaStandardDev) {
		if ( betaStandardDev > 0 ) {
			log.warn( "setting betaStandardDev to "+betaStandardDev );
			log.warn( "positive values mean people will MAXIMISE inequity!");
		}
		this.betaStandardDev = betaStandardDev;
	}
}
