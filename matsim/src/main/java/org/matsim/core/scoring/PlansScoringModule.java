/* *********************************************************************** *
 * project: org.matsim.*
 * PlansScoringModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansScoring;

public final class PlansScoringModule extends AbstractModule {
	@Override
	public void install() {
		bind(EventsToActivities.class).asEagerSingleton();
		bind(EventsToLegs.class).asEagerSingleton();
		bind(EventsToLegsAndActivities.class).asEagerSingleton();
		bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
		bind(PlansScoring.class).to(PlansScoringImpl.class);
		bind(ExperiencedPlansService.class).to(ExperiencedPlansServiceImpl.class).asEagerSingleton();
		bind(NewScoreAssigner.class).to(NewScoreAssignerImpl.class).asEagerSingleton();
	}
}
