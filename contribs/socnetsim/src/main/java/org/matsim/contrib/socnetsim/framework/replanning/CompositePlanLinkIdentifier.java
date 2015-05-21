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
package org.matsim.contrib.socnetsim.framework.replanning;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;

/**
 * Allows to define complex rules about which plans should be associated in a joint plan.
 * It accepts two kind of rules: "AND" and "OR" rules.
 * For two plans to be linked, they need to be linked by all "AND" rules and by at least
 * one "OR" rule.
 * <b>
 * Note that this implies that a coumpond without "OR" rule will
 * never link any pair of plans!
 * </b>
 * @author thibautd
 */
public final class CompositePlanLinkIdentifier implements PlanLinkIdentifier {
	private static final Logger log = Logger.getLogger( CompositePlanLinkIdentifier.class );

	private final Collection<PlanLinkIdentifier> orDelegates = new ArrayList<PlanLinkIdentifier>();
	private final Collection<PlanLinkIdentifier> andDelegates = new ArrayList<PlanLinkIdentifier>();

	private int nWarns = 0;
	private boolean locked = false;

	public CompositePlanLinkIdentifier(final PlanLinkIdentifier... orDelegates) {
		for ( PlanLinkIdentifier d : orDelegates ) addOrComponent( d );
	}

	/**
	 * if one "or" component returns true, the plans are considered as linked.
	 */
	public void addOrComponent( final PlanLinkIdentifier delegate ) {
		if ( locked ) throw new IllegalStateException( "cannot modify a "+getClass().getSimpleName()+" after its areLinked() method has been called" );
		this.orDelegates.add( delegate );
	}

	/**
	 * if one "and" component returns false, the plans are considered not being linked.
	 * Can be used for instance to forbid linking plans of persons not
	 * linked by a social tie.
	 */
	public void addAndComponent( final PlanLinkIdentifier delegate ) {
		if ( locked ) throw new IllegalStateException( "cannot modify a "+getClass().getSimpleName()+" after its areLinked() method has been called" );
		this.andDelegates.add( delegate );
	}

	@Override
	public boolean areLinked(final Plan p1, final Plan p2) {
		locked = true;

		for ( PlanLinkIdentifier delegate : andDelegates ) {
			if ( !delegate.areLinked( p1 , p2 ) ) return false;
		}

		if ( orDelegates.isEmpty() && nWarns++ == 0 ) {
			log.warn( "plan link identifier has no OR component, and will thus reject all couples." );
			log.warn( "make sure this is what you want." );
		}

		for ( PlanLinkIdentifier delegate : orDelegates ) {
			if ( delegate.areLinked( p1 , p2 ) ) return true;
		}

		return false;
	}
}

