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

package playground.thibautd.socnetsim.run;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;

import com.google.inject.Inject;

public final class JointPlanSelectionConsistencyChecker implements IterationEndsListener, IterationStartsListener {
	private boolean gotError;
	private final Population population;
	private final JointPlans jointPlans;

	@Inject
	public JointPlanSelectionConsistencyChecker( final Scenario sc ) {
		this.population = sc.getPopulation();
		this.jointPlans = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		RunUtils.log.info( "Checking consistency of joint plan selection" );
		final Map<JointPlan, Set<Plan>> plansOfJointPlans = new HashMap<JointPlan, Set<Plan>>();

		for ( Person person : population.getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			final JointPlan jp = jointPlans.getJointPlan( plan );

			if ( jp != null ) {
				Set<Plan> plans = plansOfJointPlans.get( jp );

				if ( plans == null ) {
					plans = new HashSet<Plan>();
					plansOfJointPlans.put( jp , plans );
				}

				plans.add( plan );
			}
		}

		for ( Map.Entry<JointPlan , Set<Plan>> entry : plansOfJointPlans.entrySet() ) {
			if ( entry.getKey().getIndividualPlans().size() != entry.getValue().size() ) {
				RunUtils.log.error( "joint plan "+entry.getKey()+
						" of size "+entry.getKey().getIndividualPlans().size()+
						" has only the "+entry.getValue().size()+" following plans selected: "+
						entry.getValue() );
				gotError = true;
			}
		}
	}

	@Override
	public void notifyIterationStarts( IterationStartsEvent event ) {
		if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
	}
}
