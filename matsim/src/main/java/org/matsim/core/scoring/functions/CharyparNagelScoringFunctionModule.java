/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CharyparNagelScoringFunctionModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.scoring.functions;

import com.google.inject.Singleton;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.PlansScoringModule;
import org.matsim.core.scoring.StandaloneExperiencedPlansModule;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.util.Map;


/**
 * Needs {@link PlansScoringModule} or {@link StandaloneExperiencedPlansModule} (or something that binds the same interfaces) as pre-requisite.
 */
public class CharyparNagelScoringFunctionModule extends AbstractModule {

    @Override
    public void install() {
        bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);

		Map<String, ScoringConfigGroup.ScoringParameterSet> scoringParameter = getConfig().scoring().getScoringParametersPerSubpopulation();

		boolean tasteVariations = scoringParameter.values().stream().anyMatch(s -> s.getTasteVariationsParams() != null);

		// If there are taste variations, the individual scoring parameters are used
		if (tasteVariations) {
			bind(ScoringParametersForPerson.class).to(IndividualPersonScoringParameters.class).in(Singleton.class);
			addControlerListenerBinding().to(IndividualPersonScoringOutputWriter.class).in(jakarta.inject.Singleton.class);

		} else {
			bind(ScoringParametersForPerson.class).to(SubpopulationScoringParameters.class);
		}
    }

}
