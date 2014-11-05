/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MyScoringFunctionFactory.java
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

package playground.mzilske.stratum;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import playground.mzilske.cadyts.CadytsScoring;
import playground.mzilske.clones.ClonesConfigGroup;

import javax.inject.Inject;

class MyScoringFunctionFactory implements ScoringFunctionFactory {

    @Inject
    Config config;

    @Inject
    AnalyticalCalibrator cadyts;

    @Inject
    PlansTranslator ptStep;

    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {
        final ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
        sumScoringFunction.addScoringFunction(scoringFunction);

        // prior
        // das funktioniert für genau diese konstellation, mit je zwei plänen und keinem (oder niedrigem)
        // sonstigem utility. keine ahnung, wie das sonst ist.

        sumScoringFunction.addScoringFunction(new SumScoringFunction.LegScoring() {
            boolean hasLeg = false;
            @Override
            public void finish() {}
            @Override
            public double getScore() {
                if (hasLeg) {
                    return - Math.log( clonesConfig.getCloneFactor() - 1.0 );
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
