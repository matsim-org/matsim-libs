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

package org.matsim.contrib.socnetsim.framework.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;

import com.google.inject.Inject;

public final class JointPlanCompositionMinimalityChecker implements IterationEndsListener, IterationStartsListener {
	private static final Logger log = Logger.getLogger( JointPlanCompositionMinimalityChecker.class );

	// Fail only on start of next iteration, to let all consistency checkers print their error, if any.
	private boolean gotError = false;
	private final PlanLinkIdentifier linkIdentifier;
	private final Population population;
	private final JointPlans jointPlansStruct;

	@Inject
	public JointPlanCompositionMinimalityChecker(
			final @Strong PlanLinkIdentifier linkIdentifier,
			final Scenario sc ) {
		this.linkIdentifier = linkIdentifier;
		this.population = sc.getPopulation();
		this.jointPlansStruct = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info( "Checking minimality of joint plan composition" );
		final PlanLinkIdentifier links = linkIdentifier;
		final Set<JointPlan> jointPlans = new HashSet<JointPlan>();

		for ( Person person : population.getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			final JointPlan jp = jointPlansStruct.getJointPlan( plan );

			if ( jp != null ) {
				jointPlans.add( jp );
			}
		}

		for ( JointPlan jp : jointPlans ) {
			for ( Plan p : jp.getIndividualPlans().values() ) {
				if ( !hasLinkedPlan( links , p , jp.getIndividualPlans().values() ) ) {
					log.error( "plan "+p+" is in "+jp+" but is not linked with any plan" );
					gotError = true ;
				}
			}
		}
	}

	private boolean hasLinkedPlan(
			final PlanLinkIdentifier links,
			final Plan p,
			final Collection<Plan> plans) {
		for ( Plan p2 : plans ) {
			if ( p == p2 ) continue;
			final boolean l = links.areLinked( p , p2 );
			if ( l != links.areLinked( p2 , p ) ) {
				log.error( "inconsistent plan link identifier revealed by "+p+" and "+p2 );
				log.error( p.getPerson().getId()+" is "+(l ? "" : "NOT")+" linked with "+p2.getPerson().getId() );
				log.error( p2.getPerson().getId()+" is "+(l ? "NOT" : "")+" linked with "+p.getPerson().getId() );
				gotError = true;
			}
			if ( l ) return true;
		}
		return false;
	}

	@Override
	public void notifyIterationStarts( IterationStartsEvent event ) {
		if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
	}
}
