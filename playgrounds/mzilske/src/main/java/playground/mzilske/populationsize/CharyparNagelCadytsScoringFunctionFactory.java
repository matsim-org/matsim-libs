/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CharyparNagelCadytsScoringFunctionFactory.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package playground.mzilske.populationsize;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import playground.mzilske.cadyts.CadytsScoring;

import javax.inject.Inject;

class CharyparNagelCadytsScoringFunctionFactory implements ScoringFunctionFactory {

    @Inject
    Config config;

    @Inject
    Scenario scenario;

    @Inject
    AnalyticalCalibrator<Link> cadyts;

    @Inject
    PlansTranslator<Link> ptStep;

    @Inject @Named("clonefactor")
    double clonefactor;


    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {

        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
        sumScoringFunction.addScoringFunction(scoringFunction);

        // prior
        sumScoringFunction.addScoringFunction(new SumScoringFunction.LegScoring() {
            boolean hasLeg = false;
            @Override
            public void finish() {}
            @Override
            public double getScore() {
                if (hasLeg) {
                    return - Math.log( clonefactor - 1.0 );
                } else {
                    return 0.0;
                }
            }
            @Override
            public void handleLeg(Leg leg) {
                hasLeg=true;
            }
        });
        return sumScoringFunction;
    }

}
