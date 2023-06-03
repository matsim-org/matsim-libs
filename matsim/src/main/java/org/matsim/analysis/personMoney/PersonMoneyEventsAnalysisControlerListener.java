/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMoneyEventsControlerListener.java
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

package org.matsim.analysis.personMoney;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import jakarta.inject.Inject;

/**
 * Adds aggregated personMoneyEvent table {@link PersonMoneyEventsAggregator} at each iteration and writes out all
 * {@link org.matsim.api.core.v01.events.PersonMoneyEvent}s of the last iteration {@link PersonMoneyEventsCollector}.
 *
 * @author vsp-gleich
 */
public class PersonMoneyEventsAnalysisControlerListener implements IterationStartsListener, IterationEndsListener, ShutdownListener, StartupListener {

    @Inject
    private PersonMoneyEventsAggregator personMoneyEventsAggregator;

    @Inject
    private PersonMoneyEventsCollector personMoneyEventsCollector;

    @Inject
    private OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    private EventsManager eventsManager;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        personMoneyEventsAggregator.writeOutput(outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "personMoneyEventsSums.tsv"));
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        personMoneyEventsAggregator.writeOutput(outputDirectoryHierarchy.getOutputFilename("output_personMoneyEventsSums.tsv"));
        /* if write extensive output */
        personMoneyEventsCollector.writeAllPersonMoneyEvents(outputDirectoryHierarchy.getOutputFilename("output_personMoneyEvents.tsv.gz"));
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        personMoneyEventsAggregator.reset(event.getIteration());
        personMoneyEventsCollector.reset(event.getIteration());

        if (event.isLastIteration()) {
            //  PersonMoneyEventsCollector might consume many resources, run only at last iteration
            eventsManager.addHandler(personMoneyEventsCollector);
        }
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        eventsManager.addHandler(personMoneyEventsAggregator);
    }
}
