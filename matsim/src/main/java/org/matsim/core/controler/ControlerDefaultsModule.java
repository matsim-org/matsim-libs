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

import com.google.inject.Inject;
import org.matsim.analysis.IterationTravelStatsModule;
import org.matsim.analysis.LegHistogramModule;
import org.matsim.analysis.LegTimesModule;
import org.matsim.analysis.LinkStatsModule;
import org.matsim.analysis.ModeStatsModule;
import org.matsim.analysis.ScoreStatsModule;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.population.VspPlansCleanerModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.replanning.annealing.ReplanningAnnealer;
import org.matsim.core.replanning.inheritance.PlanInheritanceModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.counts.CountsModule;
import org.matsim.guice.DependencyGraphModule;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

import javax.imageio.ImageIO;
import java.io.File;

public final class ControlerDefaultsModule extends AbstractModule {

    @Override
    public void install() {
        install(new EventsManagerModule());
        install(new DefaultMobsimModule());
        install(new TravelTimeCalculatorModule());
        install(new TravelDisutilityModule());
        install(new CharyparNagelScoringFunctionModule());
        install(new TripRouterModule());
        install(new StrategyManagerModule());
        install(new TimeInterpretationModule());
        if (getConfig().replanningAnnealer().isActivateAnnealingModule()) {
            addControlerListenerBinding().to(ReplanningAnnealer.class);
        }

        // I think that the ones coming here are all for analysis only, and thus not central to the iterations. kai, apr'18
        install(new LinkStatsModule());
        install(new VolumesAnalyzerModule());
        install(new LegHistogramModule());
        install(new LegTimesModule());
        install(new IterationTravelStatsModule());
        install(new ScoreStatsModule());
        install(new ModeStatsModule());
        install(new CountsModule());
        install(new VspPlansCleanerModule());
        install(new SnapshotWritersModule());
        install(new DependencyGraphModule());
        install(new PlanInheritanceModule());

		// Comment by Tarek Chouaki.
		// To make sure the cache files used under ChartUtils are located in tmp folder in the output directory
		// The ImageIO.setCacheDirectory method checks if the provided directory exists so it needs to be created first
		// Maybe not the best place to but this but since ChartUtils is used by many modules, including default ones,
		//  the cache needs to be always set correctly.
		addControlerListenerBinding().toInstance(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				ImageIO.setCacheDirectory(new File(event.getServices().getControlerIO().getTempPath()));
			}
		});

    	/* Comment by kai (mz thinks it is not helpful): The framework eventually calls the above method, which calls the include
        * methods , which (fairly quickly) call their own install methods, etc.  Eventually, everything is resolved down to the
        * "bindTo..." methods, which are the leaves.
    	*/

    }
}
