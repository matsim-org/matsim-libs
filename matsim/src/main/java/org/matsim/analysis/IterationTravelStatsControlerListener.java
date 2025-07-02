
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

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.ExperiencedPlansService;

import jakarta.inject.Inject;

class IterationTravelStatsControlerListener implements IterationEndsListener, ShutdownListener {

    @Inject
    Config config;

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
	PersonWriter personWriter;

	@Inject
	ActivityWriter activityWriter;

	@Inject
	TripsAndLegsWriter tripsAndLegsWriter;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pHbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pkMbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());

		boolean writeGraph = isWriteGraph(event);
		pHbyModeCalculator.writeOutput(writeGraph);
        pkMbyModeCalculator.writeOutput(writeGraph);
		travelDistanceStats.writeOutput(event.getIteration(), writeGraph);

        if (isWriteTripsAndLegs(event)) {
            tripsAndLegsWriter.write(experiencedPlansService.getExperiencedPlans()
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.tripscsv)
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.legscsv));

			activityWriter.writeCsv(event.getIteration());
        }
    }

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();

        if (config.controller().getWriteTripsInterval() > 0) {
            personWriter.writeCsv();
        }
    }

	private boolean isWriteGraph(IterationEndsEvent event){
		return config.controller().getCreateGraphsInterval() > 0 && event.getIteration() % config.controller().getCreateGraphsInterval() == 0;
	}

	private boolean isWriteTripsAndLegs(IterationEndsEvent event) {

		// This uses the same logic as in PlansDumpingImpl
		int writeTripsInterval = config.controller().getWriteTripsInterval();
		final boolean writingTripsAtAll = writeTripsInterval > 0;
		final boolean earlyIteration = event.getIteration()-config.controller().getFirstIteration()==1;

		if (!writingTripsAtAll) {
			return false;
		}

		return earlyIteration || event.isLastIteration() || event.getIteration() % writeTripsInterval == 0;
	}
}
