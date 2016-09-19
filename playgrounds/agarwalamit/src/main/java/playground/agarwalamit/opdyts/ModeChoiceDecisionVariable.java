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

import floetteroed.opdyts.DecisionVariable;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;

import java.util.Map;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
final class ModeChoiceDecisionVariable implements DecisionVariable {
    private static final Logger log = Logger.getLogger( ModeChoiceDecisionVariable.class ) ;

    private final Scenario scenario;

    private final PlanCalcScoreConfigGroup newScoreConfig;

    ModeChoiceDecisionVariable(PlanCalcScoreConfigGroup newScoreConfig, Scenario scenario) {
        this.newScoreConfig = newScoreConfig;
        this.scenario = scenario;
    }

    PlanCalcScoreConfigGroup getScoreConfig() {
        return newScoreConfig ;
    }

    @Override public void implementInSimulation() {
        for ( Map.Entry<String, ScoringParameterSet> entry : newScoreConfig.getScoringParametersPerSubpopulation().entrySet() ) {
            String subPopName = entry.getKey() ;
            log.warn( "treating sub-population with name=" + subPopName );
            ScoringParameterSet newParameterSet = entry.getValue() ;
            for ( Map.Entry<String, ModeParams> newModeEntry : newParameterSet.getModes().entrySet() ) {
                String mode = newModeEntry.getKey() ;
                if ( !TransportMode.car.equals(mode) ) { // we leave car alone
                    log.warn( "treating mode with name=" + mode ) ;
                    ModeParams newModeParams = newModeEntry.getValue() ;
                    ModeParams scenarioModeParams = scenario.getConfig().planCalcScore().getScoringParameters( subPopName ).getModes().get(mode) ;

                    scenarioModeParams.setMarginalUtilityOfTraveling( newModeParams.getMarginalUtilityOfTraveling() );

                    log.warn("new mode params:" + scenarioModeParams );
                }
            }
        }
    }

}
