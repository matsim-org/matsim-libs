/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import floetteroed.opdyts.DecisionVariableRandomizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import playground.kai.usecases.opdytsintegration.modechoice.ModeChoiceDecisionVariable;

public final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
    private static final Logger log = Logger.getLogger(ModeChoiceRandomizer.class);

    private final Scenario scenario;
    private final Random rnd;
    private final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser;

    public ModeChoiceRandomizer(final Scenario scenario, final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser) {
        this.scenario = scenario;
        this.rnd = new Random(4711);
        // (careful with using matsim-random since it is always the same sequence in one run)
        this.randomizedUtilityParametersChoser = randomizedUtilityParametersChoser;
    }

    @Override
    public List<ModeChoiceDecisionVariable> newRandomVariations(ModeChoiceDecisionVariable decisionVariable) {
        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        List<ModeChoiceDecisionVariable> result = new ArrayList<>();
        {
            PlanCalcScoreConfigGroup newScoringConfig1 = new PlanCalcScoreConfigGroup();
            PlanCalcScoreConfigGroup newScoringConfig2 = new PlanCalcScoreConfigGroup();
            for (String mode : oldScoringConfig.getModes().keySet()) {
                PlanCalcScoreConfigGroup.ModeParams oldModeParams = oldScoringConfig.getModes().get(mode);

                if (TransportMode.car.equals(mode)) {// we leave car alone
                    newScoringConfig1.addModeParams(oldModeParams);
                    newScoringConfig2.addModeParams(oldModeParams);
                } else {
                    PlanCalcScoreConfigGroup.ModeParams newModeParams1 = new PlanCalcScoreConfigGroup.ModeParams(mode) ;
                    PlanCalcScoreConfigGroup.ModeParams newModeParams2 = new PlanCalcScoreConfigGroup.ModeParams(mode) ;

                    double rnd1 = 1. * rnd.nextDouble();
                    double rnd2 = 1. * rnd.nextDouble();
                    double rnd3 = 1. * rnd.nextDouble();

                    switch (this.randomizedUtilityParametersChoser) {

                        case ONLY_ASC:
                            rnd2 = 0;
                            rnd3 = 0;
                            break;
                        case ALL_EXCEPT_ASC:
                            rnd1 = 0;
                            break;
                        case ALL:
                            break;
                        default:
                            throw new RuntimeException("not implemented yet.");
                    }

                    newModeParams1.setConstant(oldModeParams.getConstant() + rnd1);
                    newModeParams2.setConstant(oldModeParams.getConstant() - rnd1);

                    newModeParams1.setMarginalUtilityOfDistance(oldModeParams.getMarginalUtilityOfDistance() + rnd2);
                    newModeParams2.setMarginalUtilityOfDistance(oldModeParams.getMarginalUtilityOfDistance() - rnd2);

                    newModeParams1.setMarginalUtilityOfTraveling(oldModeParams.getMarginalUtilityOfTraveling() + rnd3);
                    newModeParams2.setMarginalUtilityOfTraveling(oldModeParams.getMarginalUtilityOfTraveling() - rnd3);

                    newModeParams1.setMonetaryDistanceRate(oldModeParams.getMonetaryDistanceRate());
                    newModeParams2.setMonetaryDistanceRate(oldModeParams.getMonetaryDistanceRate());

                    newScoringConfig1.addModeParams(newModeParams1);
                    newScoringConfig2.addModeParams(newModeParams2);
                }
            }
            result.add(new ModeChoiceDecisionVariable(newScoringConfig1, this.scenario));
            result.add(new ModeChoiceDecisionVariable(newScoringConfig2, this.scenario));
        }
        log.warn("giving the following to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
//		System.exit(-1);
        return result;
    }
}