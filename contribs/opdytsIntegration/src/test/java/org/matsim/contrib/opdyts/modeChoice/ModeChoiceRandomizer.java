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

package org.matsim.contrib.opdyts.modeChoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import floetteroed.opdyts.DecisionVariableRandomizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

public final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
    private static final Logger log = Logger.getLogger(ModeChoiceRandomizer.class);

    private final Scenario scenario;
    private final Random rnd;

    private final OpdytsConfigGroup opdytsConfigGroup;
    private final Collection<String> considerdModes ;

    public ModeChoiceRandomizer(final Scenario scenario, final Collection<String> considerdModes) {
        this.scenario = scenario;
        this.opdytsConfigGroup = (OpdytsConfigGroup) scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME);
        this.rnd = new Random(opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        log.warn("The random seed to randomizing decision variable is :"+ opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        this.considerdModes = considerdModes;
    }

    @Override
    public List<ModeChoiceDecisionVariable> newRandomVariations(ModeChoiceDecisionVariable decisionVariable) {
        List<ModeChoiceDecisionVariable> result = new ArrayList<>();

        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet = oldScoringConfig.getScoringParametersPerSubpopulation().get(null);

        int totalNumberOfCombination = (int) Math.pow(2, this.considerdModes.size()-1); // exclude car
        List<PlanCalcScoreConfigGroup> allCombinations = new ArrayList<>(totalNumberOfCombination);
        List<String> remainingModes = new ArrayList<>(this.considerdModes);

        { // create one planCalcScoreConfigGroup with all starting params.
            PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
            allCombinations.add(configGroupWithStartingModeParams);
            remainingModes.remove(TransportMode.car);
        }

        log.warn("creating randomVariation combinations of decision variable as : axial_fixed");
        for(String mode : this.considerdModes) {
            if ( mode.equals(TransportMode.car)) continue;
            { // positive
                PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = configGroupWithStartingModeParams.getModes().get(mode);
                double newASC =  sourceModeParam.getConstant() + opdytsConfigGroup.getVariationSizeOfRandomizeDecisionVariable() ;
                sourceModeParam.setConstant(newASC);
                allCombinations.add(configGroupWithStartingModeParams);
            }
            { // negative
                PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = configGroupWithStartingModeParams.getModes().get(mode);
                double newASC =  sourceModeParam.getConstant() - opdytsConfigGroup.getVariationSizeOfRandomizeDecisionVariable() ;
                sourceModeParam.setConstant(newASC);
                allCombinations.add(configGroupWithStartingModeParams);
            }
        }

        result = allCombinations.parallelStream().map(e -> new ModeChoiceDecisionVariable(e, this.scenario)).collect(
                Collectors.toList());

        log.warn("input decision variable");
        log.warn(decisionVariable.toString());

        log.warn("giving the following new decision variables to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
        return result;
    }

    private PlanCalcScoreConfigGroup.ModeParams copyOfModeParam(final PlanCalcScoreConfigGroup.ModeParams modeParams) {
        PlanCalcScoreConfigGroup.ModeParams newModeParams = new PlanCalcScoreConfigGroup.ModeParams(modeParams.getMode());
        newModeParams.setConstant(modeParams.getConstant());
        newModeParams.setMarginalUtilityOfDistance(modeParams.getMarginalUtilityOfDistance());
        newModeParams.setMarginalUtilityOfTraveling(modeParams.getMarginalUtilityOfTraveling());
        newModeParams.setMonetaryDistanceRate(modeParams.getMonetaryDistanceRate());
        return newModeParams;
    }

    private PlanCalcScoreConfigGroup copyOfPlanCalcScore(final PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet){
        PlanCalcScoreConfigGroup configGroupWithStartingModeParams = new PlanCalcScoreConfigGroup();
        for ( PlanCalcScoreConfigGroup.ModeParams modeParams : oldParameterSet.getModes().values()) {
            configGroupWithStartingModeParams.addModeParams(copyOfModeParam(modeParams) );
        }
        return configGroupWithStartingModeParams;
    }
}