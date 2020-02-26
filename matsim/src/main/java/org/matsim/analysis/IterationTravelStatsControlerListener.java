
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
    private PKMbyModeCalculator pkMbyModeCalculator;
    @Inject
    OutputDirectoryHierarchy outputDirectoryHierarchy;
	@Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pkMbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        if (config.controler().getWriteTripsInterval() > 0 && config.controler().getWriteTripsInterval() % event.getIteration() == 0) {
            String end = getEnding();
            new TripsCSVWriter(scenario).write(experiencedPlansService.getExperiencedPlans(), outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "trips.csv" + end));
        }
    }

    private String getEnding() {
        String end = config.controler().getCompressionType().toString();
        if (end != null) {
            end = "." + end;
        } else end = "";
        return end;
    }

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();
        pkMbyModeCalculator.writeOutput();
        if (config.controler().getDumpDataAtEnd()) {
            new TripsCSVWriter(scenario).write(experiencedPlansService.getExperiencedPlans(), outputDirectoryHierarchy.getOutputFilename("trips.csv" + getEnding()));

        }
	}
}
