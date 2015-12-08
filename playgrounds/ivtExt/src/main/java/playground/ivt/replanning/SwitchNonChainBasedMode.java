/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchNonChainBasedMode.java
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
package playground.ivt.replanning;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author thibautd
 */
public class SwitchNonChainBasedMode implements PlanStrategy {
	final PlanStrategyImpl delegate;

	@Inject
	public SwitchNonChainBasedMode(final Scenario sc) {
		delegate = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );

		delegate.addStrategyModule( new SwitchNonChainBasedModeModule( sc.getConfig() ) );
		delegate.addStrategyModule( new ReRoute( sc ) );
	}

	@Override
	public void run(final HasPlansAndId<Plan, Person> person) {
		delegate.run( person );
	}

	@Override
	public void init(final ReplanningContext replanningContext) {
		delegate.init( replanningContext );
	}

	@Override
	public void finish() {
		delegate.finish();
	}
}

