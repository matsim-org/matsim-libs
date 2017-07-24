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

import java.util.Map;
import floetteroed.opdyts.DecisionVariable;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * Created by amit on 13/10/16.
 */

public class ModeChoiceDecisionVariable implements DecisionVariable {

    private static final Logger log = Logger.getLogger( ModeChoiceDecisionVariable.class ) ;

    private final Scenario scenario;

    private final PlanCalcScoreConfigGroup newScoreConfig;

    public ModeChoiceDecisionVariable(PlanCalcScoreConfigGroup newScoreConfig, Scenario scenario) {
        this.newScoreConfig = newScoreConfig;
        this.scenario = scenario;
    }

    public PlanCalcScoreConfigGroup getScoreConfig() {
        return newScoreConfig ;
    }

    @Override public void implementInSimulation() {
        for ( Map.Entry<String, PlanCalcScoreConfigGroup.ScoringParameterSet> entry : newScoreConfig.getScoringParametersPerSubpopulation().entrySet() ) {
            String subPopName = entry.getKey() ;
            PlanCalcScoreConfigGroup.ScoringParameterSet newParameterSet = entry.getValue() ;
            for ( PlanCalcScoreConfigGroup.ModeParams newModeParams : newParameterSet.getModes().values() ) {
                scenario.getConfig().planCalcScore().getScoringParameters( subPopName ).addModeParams( newModeParams ) ;
            }
        }
    }

    @Override public String toString() {
        final Map<String, PlanCalcScoreConfigGroup.ModeParams> allModes = newScoreConfig.getScoringParameters(null).getModes();

        StringBuilder strb = new StringBuilder() ;
        for ( PlanCalcScoreConfigGroup.ModeParams modeParams : allModes.values() ) {
            final String mode = modeParams.getMode();
            if ( TransportMode.car.equals(mode) || TransportMode.pt.equals(mode) ) {
                strb.append( mode + ": " + modeParams.getConstant() + " + " + modeParams.getMarginalUtilityOfTraveling() + " * ttime ; " ) ;
            }
        }

        return strb.toString() ;
    }
}
