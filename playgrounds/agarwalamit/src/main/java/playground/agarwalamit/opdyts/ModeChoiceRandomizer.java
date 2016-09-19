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

import floetteroed.opdyts.DecisionVariableRandomizer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.List;

final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
    private final Scenario scenario;

    ModeChoiceRandomizer(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override public List<ModeChoiceDecisionVariable> newRandomVariations(ModeChoiceDecisionVariable decisionVariable ) {
        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        List<ModeChoiceDecisionVariable> result = new ArrayList<>() ;
        for ( int ii=0 ; ii<2 ; ii++ ) {
            PlanCalcScoreConfigGroup newScoringConfig = new PlanCalcScoreConfigGroup() ;
            for ( String mode : oldScoringConfig.getModes().keySet() ) {
                if ( ! TransportMode.car.equals(mode ) ) { // we leave car alone
                    PlanCalcScoreConfigGroup.ModeParams oldModeParams = oldScoringConfig.getModes().get(mode) ;
                    PlanCalcScoreConfigGroup.ModeParams newModeParams = new PlanCalcScoreConfigGroup.ModeParams(mode) ;
                    newModeParams.setConstant( oldModeParams.getConstant() + 0.1 * MatsimRandom.getRandom().nextGaussian() );
                    // yyyyyy careful with using matsim-random since it is always the same sequence!!
                    newModeParams.setMarginalUtilityOfDistance( oldModeParams.getMarginalUtilityOfDistance() + 0.1 * MatsimRandom.getRandom().nextGaussian() );
                    newModeParams.setMarginalUtilityOfTraveling( oldModeParams.getMarginalUtilityOfTraveling() + 1. * MatsimRandom.getRandom().nextGaussian() );
                    newModeParams.setMarginalUtilityOfDistance(oldModeParams.getMarginalUtilityOfDistance());
                    newModeParams.setMonetaryDistanceRate(oldModeParams.getMonetaryDistanceRate());
                    newScoringConfig.addModeParams(newModeParams);
                }
            }
            result.add( new ModeChoiceDecisionVariable( newScoringConfig, this.scenario ) ) ;
        }
        return result ;
    }
}