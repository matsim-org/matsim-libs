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

import org.matsim.analysis.*;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.counts.CountsModule;
import org.matsim.population.VspPlansCleanerModule;
import org.matsim.pt.counts.PtCountsModule;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

public class ControlerDefaultsModule extends AbstractModule {
    @Override
    public void install() {
        install(new EventsManagerModule());
        install(new DefaultMobsimModule());
        install(new TravelTimeCalculatorModule());
        install(new TravelDisutilityModule());
        install(new CharyparNagelScoringFunctionModule());
        install(new ExperiencedPlansModule());
        install(new TripRouterModule());
        install(new StrategyManagerModule());
        install(new LinkStatsModule());
        install(new VolumesAnalyzerModule());
        install(new LegHistogramModule());
        install(new LegTimesModule());
        install(new TravelDistanceStatsModule());
        install(new ScoreStatsModule());
        install(new CountsModule());
        install(new PtCountsModule());
        install(new VspPlansCleanerModule());
        install(new SnapshotWritersModule());

    	/* Comment by kai (mz thinks it is not helpful): The framework eventually calls the above method, which calls the include 
        * methods , which (fairly quickly) call their own install methods, etc.  Eventually, everything is resolved down to the
        * "bindTo..." methods, which are the leaves.
    	*/

    }
}
