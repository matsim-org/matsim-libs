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
import java.util.stream.Collectors;
import floetteroed.opdyts.DecisionVariableRandomizer;
import opdytsintegration.utils.OpdytsConfigGroup;
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
        this.rnd = new Random(opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        log.warn("The random seed to randomizing decision variable is :"+ opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        //        this.rnd = new Random(4711);
        // this will create an identical sequence of candidate decision variables for each experiment where a new ModeChoiceRandomizer instance is created.
        // That's not good; the parametrized runs are then all conditional on the 4711 random seed.
        // (careful with using matsim-random since it is always the same sequence in one createCombinations)
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
        double ascChangeAmount = opdytsConfigGroup.getVariationSizeOfRandomizeDecisionVariable() * rnd.nextDouble();

        int totalNumberOfCombination = (int) Math.pow(2, this.considerdModes.size()-1); // exclude car
        List<PlanCalcScoreConfigGroup> allCombinations = new ArrayList<>(totalNumberOfCombination);
        List<String> remainingModes = new ArrayList<>(this.considerdModes);

        { // create planCalcScoreConfigGroup with car modes
            PlanCalcScoreConfigGroup configGroupWithCarModeParams = new PlanCalcScoreConfigGroup();
            for ( PlanCalcScoreConfigGroup.ModeParams modeParams : oldParameterSet.getModes().values()) {
                configGroupWithCarModeParams.getScoringParametersPerSubpopulation().get(this.subPopName).addModeParams(copyOfModeParam(modeParams) );
            }
            allCombinations.add(configGroupWithCarModeParams);
            remainingModes.remove(TransportMode.car);
        }

        createCombinations(oldParameterSet, allCombinations, remainingModes, ascChangeAmount);

        result = allCombinations.parallelStream().map(e -> new ModeChoiceDecisionVariable(e, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName)).collect(
                Collectors.toList());

        log.warn("input decision variable");
        log.warn(decisionVariable.toString());

        log.warn("giving the following new decision variables to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
        return result;
    }

    private void createCombinations(final PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet, final List<PlanCalcScoreConfigGroup> allCombinations, final List<String> remainingModes, final double ascChangeAmount) {
        // create combinations with one mode and call createCombinations again
        if (remainingModes.isEmpty()) return;
        else {
            String mode = remainingModes.remove(0);
            if (mode.equals(TransportMode.car)) {
                throw new RuntimeException("The parameters of the car remain unchanged. Therefore, car mode should not end up here, it should be removed in the previous step. ");
            } else {
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = copyOfModeParam(oldParameterSet.getModes().get(mode));
                {// positive: since this mode is never called before, update existing one only
                    double newASC =  sourceModeParam.getConstant() + ascChangeAmount;
                    allCombinations.parallelStream().forEach(e -> e.getOrCreateScoringParameters(this.subPopName).getOrCreateModeParams(mode).setConstant(newASC) );
                }
                { // negative: since this mode is already called before, first copy existing ones, update values and then add them to main collection
                    List<PlanCalcScoreConfigGroup> tempCombinations = new ArrayList<>();
                    allCombinations.parallelStream().forEach(e -> {
                        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();
                        for ( String newMode : this.considerdModes) {
                            PlanCalcScoreConfigGroup.ModeParams modeParams = e.getScoringParameters(this.subPopName).getModes().get(newMode);
                            planCalcScoreConfigGroup.getScoringParametersPerSubpopulation().get(this.subPopName).addModeParams(copyOfModeParam(modeParams) );
                        }
                        tempCombinations.add(planCalcScoreConfigGroup);
                    });
                    double newASC =  sourceModeParam.getConstant() - ascChangeAmount;
                    tempCombinations.parallelStream().forEach(e -> e.getOrCreateScoringParameters(this.subPopName).getOrCreateModeParams(mode).setConstant(newASC) );
                    allCombinations.addAll(tempCombinations);
                }
            }
            createCombinations(oldParameterSet, allCombinations, remainingModes, ascChangeAmount);
        }
    }

    private PlanCalcScoreConfigGroup.ModeParams copyOfModeParam(final PlanCalcScoreConfigGroup.ModeParams modeParams) {
        PlanCalcScoreConfigGroup.ModeParams newModeParams = new PlanCalcScoreConfigGroup.ModeParams(modeParams.getMode());
        newModeParams.setConstant(modeParams.getConstant());
        newModeParams.setMarginalUtilityOfDistance(modeParams.getMarginalUtilityOfDistance());
        newModeParams.setMarginalUtilityOfTraveling(modeParams.getMarginalUtilityOfTraveling());
        newModeParams.setMonetaryDistanceRate(modeParams.getMonetaryDistanceRate());
        return newModeParams;
    }
}