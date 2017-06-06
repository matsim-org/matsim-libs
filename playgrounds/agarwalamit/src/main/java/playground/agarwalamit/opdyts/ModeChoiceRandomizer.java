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

import java.util.*;
import floetteroed.opdyts.DecisionVariableRandomizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.utils.collections.Tuple;

public final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
    private static final Logger log = Logger.getLogger(ModeChoiceRandomizer.class);

    private final Scenario scenario;
    private final Random rnd;
    private final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser;
    private final String subPopName;
    private final OpdytsScenario opdytsScenario;

    private final OpdytsConfigGroup opdytsConfigGroup;
    private final Collection<String> considerdModes ;

    public ModeChoiceRandomizer(final Scenario scenario, final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser,
             final OpdytsScenario opdytsScenario, final String subPopName, final Collection<String> considerdModes) {
        this.scenario = scenario;
        opdytsConfigGroup = (OpdytsConfigGroup) scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME);
        this.rnd = new Random();
        log.warn("The random seed to randomizing decision variable is :"+ opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        //        this.rnd = new Random(4711);
        // this will create an identical sequence of candidate decision variables for each experiment where a new ModeChoiceRandomizer instance is created.
        // That's not good; the parametrized runs are then all conditional on the 4711 random seed.
        // (careful with using matsim-random since it is always the same sequence in one run)
        this.randomizedUtilityParametersChoser = randomizedUtilityParametersChoser;
        this.subPopName = subPopName;
        this.opdytsScenario = opdytsScenario;
        this.considerdModes = considerdModes;
    }

    @Override
    public List<ModeChoiceDecisionVariable> newRandomVariations(ModeChoiceDecisionVariable decisionVariable) {
        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        List<ModeChoiceDecisionVariable> result = new ArrayList<>();
        PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet = oldScoringConfig.getScoringParametersPerSubpopulation().get(this.subPopName);
        Map<String, Tuple<PlanCalcScoreConfigGroup.ModeParams, PlanCalcScoreConfigGroup.ModeParams>> mode2modeParamsSet = new HashMap<>();

        for(String mode : considerdModes) {
            if (mode.equals(TransportMode.car)) continue;
            else {
                PlanCalcScoreConfigGroup.ModeParams oldModeParams = oldParameterSet.getModes().get(mode);
                PlanCalcScoreConfigGroup.ModeParams newModeParamsPos = new PlanCalcScoreConfigGroup.ModeParams(mode) ;
                PlanCalcScoreConfigGroup.ModeParams newModeParamsNeg = new PlanCalcScoreConfigGroup.ModeParams(mode) ;

                double rnd1 = opdytsConfigGroup.getVariationSizeOfRamdomizeDecisionVariable() * rnd.nextDouble();
//                double rnd2 = 1. * rnd.nextDouble();
//                double rnd3 = 1. * rnd.nextDouble();

                switch (this.randomizedUtilityParametersChoser) {
                    case ONLY_ASC:
                        newModeParamsPos.setConstant(oldModeParams.getConstant() + rnd1);
                        newModeParamsNeg.setConstant(oldModeParams.getConstant() - rnd1);

                        Set<PlanCalcScoreConfigGroup.ModeParams> modeParamsSet = new HashSet<>();
                        modeParamsSet.add(newModeParamsPos);
                        modeParamsSet.add(newModeParamsNeg);
                        mode2modeParamsSet.put(mode, new Tuple<>(newModeParamsPos, newModeParamsNeg));
                        break;
                    case ALL_EXCEPT_ASC:
                    case ALL:
                    default:
                            throw new RuntimeException("not implemented yet.");
                }
            }
        }

        // create new decision variables
        // number of cases would be = consideredModes-1 * 2
//        for(int index =0; index < (considerdModes.size()-1) * 2.; index++) {
//            PlanCalcScoreConfigGroup newScoringConfig = new PlanCalcScoreConfigGroup();
//            PlanCalcScoreConfigGroup.ModeParams oldModeParams = oldParameterSet.getModes().get(mode);
//            newScoringConfig.addModeParams(oldModeParams);
//
//
//
//        }






        {
//            PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet = oldScoringConfig.getScoringParametersPerSubpopulation().get(this.subPopName);
            PlanCalcScoreConfigGroup newScoringConfig1 = new PlanCalcScoreConfigGroup();
            PlanCalcScoreConfigGroup newScoringConfig2 = new PlanCalcScoreConfigGroup();
            for (String mode : oldParameterSet.getModes().keySet()) {
                PlanCalcScoreConfigGroup.ModeParams oldModeParams = oldParameterSet.getModes().get(mode);

                if (TransportMode.car.equals(mode)) {// we leave car alone
                    newScoringConfig1.addModeParams(oldModeParams);
                    newScoringConfig2.addModeParams(oldModeParams);
                } else {
                    PlanCalcScoreConfigGroup.ModeParams newModeParams1 = new PlanCalcScoreConfigGroup.ModeParams(mode) ;
                    PlanCalcScoreConfigGroup.ModeParams newModeParams2 = new PlanCalcScoreConfigGroup.ModeParams(mode) ;

                    double rnd1 = opdytsConfigGroup.getVariationSizeOfRamdomizeDecisionVariable() * rnd.nextDouble();
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

                    newScoringConfig1.getOrCreateScoringParameters(this.subPopName).addModeParams(newModeParams1);
                    newScoringConfig2.getOrCreateScoringParameters(this.subPopName).addModeParams(newModeParams2);
                }
            }
            result.add(new ModeChoiceDecisionVariable(newScoringConfig1, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName));
            result.add(new ModeChoiceDecisionVariable(newScoringConfig2, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName));
        }
        log.warn("giving the following to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
//		System.exit(-1);
        return result;
    }
}