/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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

package org.matsim.core.controler;

import org.matsim.analysis.LegTimesModule;
import org.matsim.analysis.ScoreStatsModule;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.corelisteners.LegHistogramModule;
import org.matsim.core.controler.corelisteners.LinkStatsModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioElementsModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.counts.CountsModule;
import org.matsim.population.VspPlansCleanerModule;
import org.matsim.pt.counts.PtCountsModule;

public class ControlerDefaultsModule extends AbstractModule {
    @Override
    public void install() {
        if (getConfig().controler().getMobsim().equals(ControlerConfigGroup.MobsimType.qsim.toString())) {
            bindToProvider(Mobsim.class, QSimProvider.class);
        } else if (getConfig().controler().getMobsim().equals(ControlerConfigGroup.MobsimType.JDEQSim.toString())) {
            bindTo(Mobsim.class, JDEQSimulation.class);
        }
        include(new ScenarioElementsModule());
        include(new TravelTimeCalculatorModule());
        include(new TravelDisutilityModule());
        include(new CharyparNagelScoringFunctionModule());
        include(new TripRouterModule());
        include(new StrategyManagerModule());
        include(new LinkStatsModule());
        include(new VolumesAnalyzerModule());
        include(new LegHistogramModule());
        include(new LegTimesModule());
        include(new ScoreStatsModule());
        include(new CountsModule());
        include(new PtCountsModule());
        include(new VspPlansCleanerModule());
    }
}
