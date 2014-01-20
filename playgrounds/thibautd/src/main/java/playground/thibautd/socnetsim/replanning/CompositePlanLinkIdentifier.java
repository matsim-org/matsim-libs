/* *********************************************************************** *
 * project: org.matsim.*
 * CompositePlanLinkIdentifier.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public final class CompositePlanLinkIdentifier implements PlanLinkIdentifier {
	private final Collection<PlanLinkIdentifier> delegates;

	public CompositePlanLinkIdentifier() {
		this.delegates = new ArrayList<PlanLinkIdentifier>();
	}

	public CompositePlanLinkIdentifier(final PlanLinkIdentifier... delegates) {
		this.delegates = new ArrayList<PlanLinkIdentifier>( delegates.length );
		for ( PlanLinkIdentifier d : delegates ) addPlanLinkIdentifier( d );
	}

	public void addPlanLinkIdentifier( final PlanLinkIdentifier delegate ) {
		this.delegates.add( delegate );
	}

	@Override
	public boolean areLinked(final Plan p1, final Plan p2) {
		for ( PlanLinkIdentifier delegate : delegates ) {
			if ( delegate.areLinked( p1 , p2 ) ) return true;
		}

		return false;
	}
}

