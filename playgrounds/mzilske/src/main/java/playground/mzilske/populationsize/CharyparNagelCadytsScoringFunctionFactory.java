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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;
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

    @Inject
    Network network;

    @Inject @Named("clonefactor")
    double clonefactor;


    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {

        final double maxabs = Math.log(10.0 * clonefactor);
        CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
        SumScoringFunction sumScoringFunction = new SumScoringFunction();
//        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
//        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
//        sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
        CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
//        scoringFunction.setWeight(10.0);

        sumScoringFunction.addScoringFunction(scoringFunction);
        sumScoringFunction.addScoringFunction(new SumScoringFunction.LegScoring() {
            boolean hasLeg = false;
            @Override
            public void finish() {}
            @Override
            public double getScore() {
                if (hasLeg) {
                    final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
                    Double signal = (Double) personAttributes.getAttribute(person.getId().toString(), "PID");
                    if (signal == null) signal = 0.0;
                    cadyts.demand.Plan<Link> currentPlanSteps = ptStep.getPlanSteps(person.getSelectedPlan());
                    double currentPlanCadytsCorrection = cadyts.calcLinearPlanEffect(currentPlanSteps);
                    signal = signal + currentPlanCadytsCorrection;
                    if (signal < -maxabs) signal = - maxabs;
                    if (signal > maxabs) signal = maxabs;
                    personAttributes.putAttribute(person.getId().toString(), "PID", signal);
                    return signal;
                } else {
                    return 0.0;
                }
            }
            @Override
            public void handleLeg(Leg leg) {
                hasLeg=true;
            }
        });

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
