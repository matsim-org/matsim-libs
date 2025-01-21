/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.pt.stop2stop;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import jakarta.inject.Inject;

/**
 * ControlerListener that executes PtStop2StopAnalysis in the last iteration
 *
 * @author vsp-gleich
 */
public class PtStop2StopAnalysisControlerListener implements IterationStartsListener, IterationEndsListener {

    private final EventsManager eventsManager;
    private final OutputDirectoryHierarchy controlerIO;
    private final PtStop2StopAnalysis ptStop2StopAnalysis;
    private final String sep;
    private final String sep2;

    @Inject
    PtStop2StopAnalysisControlerListener(Scenario scenario, EventsManager eventsManager, OutputDirectoryHierarchy controlerIO) {
        this.eventsManager = eventsManager;
        this.controlerIO = controlerIO;
		// TODO: Sample size is only available in simwrapper config group which is not available here. Setting to 1, needs to be upscaled later.
        ptStop2StopAnalysis = new PtStop2StopAnalysis(scenario.getTransitVehicles(), 1.0);
        sep = scenario.getConfig().global().getDefaultDelimiter();
        sep2 = sep.equals(";") ? "_" : ";"; // TODO: move sep2 to global config
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.isLastIteration()) {
            // the above hopefully points to TerminationCriterion, if not working should be fixed in IterationStartsEvent.isLastIteration()
            // registering this handler here hopefully avoids having it running in previous iterations, when we prefer saving computation time over having this analysis output
            eventsManager.addHandler(ptStop2StopAnalysis);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.isLastIteration()) {
            String outputCsvByDeparture = controlerIO.getOutputFilename("pt_stop2stop_departures.csv.gz");
            ptStop2StopAnalysis.writeStop2StopEntriesByDepartureCsv(outputCsvByDeparture, sep, sep2);
        }
    }


}
