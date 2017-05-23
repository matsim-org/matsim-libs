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

import java.util.Map;
import floetteroed.opdyts.DecisionVariable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * Created by amit on 13/10/16.
 */


@SuppressWarnings("DefaultFileTemplate")
public class ModeChoiceDecisionVariable implements DecisionVariable {

    private final playground.kai.usecases.opdytsintegration.modechoice.ModeChoiceDecisionVariable delegate ;

    private final OpdytsScenario opdytsScenario;
    private final String subPopulation;
    private final PlanCalcScoreConfigGroup newScoreConfig;

    public ModeChoiceDecisionVariable(final PlanCalcScoreConfigGroup neScoreConfig, final Scenario scenario,
                                      final OpdytsScenario opdytsScenario, final String subPopulatioun){
        delegate = new playground.kai.usecases.opdytsintegration.modechoice.ModeChoiceDecisionVariable(neScoreConfig,scenario);
        this.newScoreConfig = neScoreConfig;
        this.opdytsScenario = opdytsScenario;
        this.subPopulation = subPopulatioun;

    }

    public ModeChoiceDecisionVariable(final PlanCalcScoreConfigGroup neScoreConfig, final Scenario scenario,
                               final OpdytsScenario opdytsScenario){
        this (neScoreConfig, scenario, opdytsScenario, null);
    }

    @Override
    public void implementInSimulation() {
        delegate.implementInSimulation();
    }

    @Override
    public String toString(){
        final Map<String, PlanCalcScoreConfigGroup.ModeParams> allModes = this.newScoreConfig.getScoringParameters(this.subPopulation).getModes();
        switch (this.opdytsScenario){
            case EQUIL:
            case EQUIL_MIXEDTRAFFIC:
            case PATNA_1Pct:
            case PATNA_10Pct:
            StringBuilder str = new StringBuilder();
                for(PlanCalcScoreConfigGroup.ModeParams mp : allModes.values()) {
                    if(mp.getMode().equals(TransportMode.other)) continue;
                    str.append(mp.getMode() + ": "+ mp.getConstant() + " + "+
                            mp.getMarginalUtilityOfTraveling()+ " * ttime " +
                            mp.getMarginalUtilityOfDistance() + " * tdist " +
                            mp.getMonetaryDistanceRate() +" * " + this.newScoreConfig.getMarginalUtilityOfMoney() + " * tdist;"
                    );
                }
                return str.toString();
            default:
                throw new RuntimeException("not implemented yet.");
        }
    }

    public PlanCalcScoreConfigGroup getScoreConfig() {
        return this.delegate.getScoreConfig();
    }
}
