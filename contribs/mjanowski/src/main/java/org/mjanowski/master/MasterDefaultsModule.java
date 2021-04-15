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

package org.mjanowski.master;

import org.matsim.analysis.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.population.VspPlansCleanerModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.counts.CountsModule;
import org.matsim.guice.DependencyGraphModule;
import org.matsim.pt.counts.PtCountsModule;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

public final class MasterDefaultsModule extends AbstractModule {
    @Override
    public void install() {
        install(new MasterEventsManagerModule());
        install(new MasterSimModule());
        install(new TravelTimeCalculatorModule());
        install(new TravelDisutilityModule());
        install(new CharyparNagelScoringFunctionModule());
        install(new TripRouterModule());
        install(new StrategyManagerModule());
    
        // I think that the ones coming here are all for analysis only, and thus not central to the iterations. kai, apr'18
        install(new ExperiencedPlansModule());
        install(new LinkStatsModule());
        install(new VolumesAnalyzerModule());
        install(new LegHistogramModule());
        install(new LegTimesModule());
        install(new IterationTravelStatsModule());
        install(new ScoreStatsModule());
        install(new ModeStatsModule());
        install(new CountsModule());
        install(new PtCountsModule());
        install(new VspPlansCleanerModule());
        install(new SnapshotWritersModule());
        install(new DependencyGraphModule());

    	/* Comment by kai (mz thinks it is not helpful): The framework eventually calls the above method, which calls the include 
        * methods , which (fairly quickly) call their own install methods, etc.  Eventually, everything is resolved down to the
        * "bindTo..." methods, which are the leaves.
    	*/

    }
}
