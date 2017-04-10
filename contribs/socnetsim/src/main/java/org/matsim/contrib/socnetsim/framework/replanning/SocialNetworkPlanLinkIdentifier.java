/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkPlanLinkIdentifier.java
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
package org.matsim.contrib.socnetsim.framework.replanning;

import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;

/**
 * Two plans are considered linked if the agents are linked by a social tie.
 *
 * Thinked to be used as a "AND" condition in the {@link CompositePlanLinkIdentifier}:
 * the plans which fulfil at least one "OR" condition will be linked iif the
 * agents are social contacts.
 * @author thibautd
 */
public class SocialNetworkPlanLinkIdentifier implements PlanLinkIdentifier {
	final SocialNetwork sn;

	public SocialNetworkPlanLinkIdentifier(
			final SocialNetwork sn) {
		if ( sn == null ) throw new NullPointerException( "Social Network is null!");
		this.sn = sn;
	}

	@Override
	public boolean areLinked(
			final Plan p1,
			final Plan p2) {
		return sn.getAlters( p1.getPerson().getId() ) .contains( p2.getPerson().getId() ) ||
			sn.getAlters( p2.getPerson().getId() ) .contains( p1.getPerson().getId() );
	}
}

