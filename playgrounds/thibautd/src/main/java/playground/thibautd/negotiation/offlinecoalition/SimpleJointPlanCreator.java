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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import playground.thibautd.negotiation.framework.Proposition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class SimpleJointPlanCreator<P extends Proposition> implements CoalitionChoiceIterator.JointPlanCreator<P> {
	private final JointPlanFactory factory = new JointPlanFactory();

	@Override
	public JointPlan apply( final P proposition ) {
		final Map<Id<Person>,Plan> plans = new HashMap<>();

		for ( Person person : proposition.getGroup() ) {
			final Plan plan = getPlan( person ).getCustomAttributes().containsKey( "proposition" ) ?
					PlanUtils.createCopy( getPlan( person ) ) :
					getPlan( person );
			if ( !person.getPlans().contains( plan ) ) person.addPlan( plan );
			plans.put( person.getId() , plan );

			plan.getCustomAttributes().put( "proposition" , proposition );
		}

		return factory.createJointPlan( plans );
	}

	private Plan getPlan( final Person person ) {
		return person.getPlans().get( 0 );
	}
}
