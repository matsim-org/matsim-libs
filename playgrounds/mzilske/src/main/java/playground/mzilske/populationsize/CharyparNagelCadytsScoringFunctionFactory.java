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

import com.google.inject.name.Named;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

import javax.inject.Inject;

class CharyparNagelCadytsScoringFunctionFactory implements ScoringFunctionFactory {

    @Inject
    Config config;

    @Inject
    Scenario scenario;

//    @Inject
//    AnalyticalCalibrator<Link> cadyts;
//
//    @Inject
//    PlansTranslator<Link> ptStep;

    @Inject @Named("clonefactor")
    double clonefactor;

    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {

        SumScoringFunction sumScoringFunction = new SumScoringFunction();
//        CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, ptStep, cadyts);
//        sumScoringFunction.addScoringFunction(scoringFunction);

        // prior
        if (clonefactor > 1.0) {
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
        }

        final TripLengthDistribution expectedTripLengthDistribution = (TripLengthDistribution) scenario.getScenarioElement("expectedTripLengthDistribution");
        final TripLengthDistribution actualTripLengthDistribution = (TripLengthDistribution) scenario.getScenarioElement("actualTripLengthDistribution");

        sumScoringFunction.addScoringFunction(new SumScoringFunction.LegScoring() {
            public double offsetSum;

            @Override
            public void handleLeg(Leg leg) {
                int bin = CalcLegTimes.getTimeslotIndex(leg.getTravelTime());
                int measured = 0;
                for (int[] bins : actualTripLengthDistribution.getDistribution().values()) {
                    measured += bins[bin];
                }
                int expected = 0;
                for (int[] bins : expectedTripLengthDistribution.getDistribution().values()) {
                    expected += bins[bin];
                }
                double offset = (double) (expected - measured) / (double) expected;
                offsetSum += offset;
            }

            @Override
            public void finish() {}

            @Override
            public double getScore() {
                return offsetSum;
            }
        });

        return sumScoringFunction;
    }

}
