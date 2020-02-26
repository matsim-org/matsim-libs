
/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStatsControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.analysis;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.ExperiencedPlansService;

import javax.inject.Inject;

class IterationTravelStatsControlerListener implements IterationEndsListener, ShutdownListener {

    @Inject
    Config config;

    @Inject
    Scenario scenario;
	@Inject
	private ExperiencedPlansService experiencedPlansService;

	@Inject
	private TravelDistanceStats travelDistanceStats;

	@Inject
    private PHbyModeCalculator pHbyModeCalculator;
	
	@Inject
    private PKMbyModeCalculator pkMbyModeCalculator;
    @Inject
    OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    TripsCSVWriter.CustomTripsWriterExtension customTripsWriterExtension;

	@Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pHbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pkMbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        final boolean writingTripsAtAll = config.controler().getWriteTripsInterval() > 0;
        final boolean regularWriteEvents = writingTripsAtAll && (event.getIteration() > 0 && event.getIteration() % config.controler().getWriteTripsInterval() == 0);
        if (regularWriteEvents || (writingTripsAtAll && event.getIteration() == 0)) {
            new TripsCSVWriter(scenario, customTripsWriterExtension).write(experiencedPlansService.getExperiencedPlans(), outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.tripscsv));
        }
    }


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();
		// TODO: this way the statistics are written only at the end. Better after each iteration?
        pHbyModeCalculator.writeOutput();
        pkMbyModeCalculator.writeOutput();
	}
}
