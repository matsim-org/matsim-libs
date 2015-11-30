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

package tutorial.programming.planStrategyForRemoval;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

/**
* @author ikaddoura
*/
public class MyExpBetaPlanChangerForRemoval implements Provider<ExpBetaPlanChanger<Plan, Person>> {

	private Config config;

    @Inject
    MyExpBetaPlanChangerForRemoval(Config config) {
        this.config = config;
    }

    @Override
    public ExpBetaPlanChanger<Plan, Person> get() {
        return new ExpBetaPlanChanger<>( - config.planCalcScore().getBrainExpBeta());
    }
	
}

