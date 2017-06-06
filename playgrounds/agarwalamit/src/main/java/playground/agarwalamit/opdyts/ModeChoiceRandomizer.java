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
import java.util.Collection;
import java.util.List;
import java.util.Random;
import floetteroed.opdyts.DecisionVariableRandomizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

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
        List<ModeChoiceDecisionVariable> result = new ArrayList<>();

        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet = oldScoringConfig.getScoringParametersPerSubpopulation().get(this.subPopName);

        for(String mode : considerdModes) {
            PlanCalcScoreConfigGroup newScoringConfig1 = new PlanCalcScoreConfigGroup();
            PlanCalcScoreConfigGroup newScoringConfig2 = new PlanCalcScoreConfigGroup();

            if (mode.equals(TransportMode.car)) {
                newScoringConfig1.getOrCreateScoringParameters(this.subPopName).addModeParams(oldParameterSet.getModes().get(mode));
                newScoringConfig2.getOrCreateScoringParameters(this.subPopName).addModeParams(oldParameterSet.getModes().get(mode));
            } else {
                double rnd1 = opdytsConfigGroup.getVariationSizeOfRamdomizeDecisionVariable() * rnd.nextDouble();
                switch (this.randomizedUtilityParametersChoser) {
                    case ONLY_ASC:
                        PlanCalcScoreConfigGroup.ModeParams newModeParamsPos = new PlanCalcScoreConfigGroup.ModeParams(mode) ;
                        newModeParamsPos.setConstant(oldParameterSet.getModes().get(mode).getConstant() + rnd1);
                        newScoringConfig1.getOrCreateScoringParameters(this.subPopName).addModeParams(newModeParamsPos);
                        newScoringConfig2.getOrCreateScoringParameters(this.subPopName).addModeParams(newModeParamsPos);

                        for(String anotherMode : considerdModes) {
                            if (anotherMode.equals(TransportMode.car) || mode.equals(anotherMode)) continue;
                            else {
                                PlanCalcScoreConfigGroup.ModeParams newAnotherModeParamsNeg = new PlanCalcScoreConfigGroup.ModeParams(anotherMode);
                                PlanCalcScoreConfigGroup.ModeParams newAnotherModeParamsPos = new PlanCalcScoreConfigGroup.ModeParams(anotherMode);
                                newAnotherModeParamsNeg.setConstant(oldParameterSet.getModes().get(anotherMode).getConstant() - rnd1);
                                newAnotherModeParamsPos.setConstant(oldParameterSet.getModes().get(anotherMode).getConstant() + rnd1);
                                newScoringConfig1.getOrCreateScoringParameters(this.subPopName).addModeParams(newAnotherModeParamsNeg);
                                newScoringConfig2.getOrCreateScoringParameters(this.subPopName).addModeParams(newAnotherModeParamsPos);

                                result.add(new ModeChoiceDecisionVariable(newScoringConfig1, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName));
                                result.add(new ModeChoiceDecisionVariable(newScoringConfig2, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName));
                            }
                        }


                        break;
                    case ALL_EXCEPT_ASC:
                    case ALL:
                    default:
                            throw new RuntimeException("not implemented yet.");
                }
            }
        }
        log.warn("giving the following to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
        return result;
    }
}