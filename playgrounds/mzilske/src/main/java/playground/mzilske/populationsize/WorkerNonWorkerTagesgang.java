/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WorkerNonWorkerTagesgang.java
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

package playground.mzilske.populationsize;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import playground.mzilske.ant2014.IterationResource;

public class WorkerNonWorkerTagesgang {

    public static void main(String[] args) {
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource uncongested = experiment.getRegime("uncongested");
        final Scenario baseRun = uncongested.getBaseRun().getOutputScenario();

        final RunResource run = uncongested.getMultiRateRun("wurst").getRateRun("0", "3");

        Config outputConfig = run.getOutputConfig();

        IterationResource iteration = run.getLastIteration();

        doIt(baseRun, outputConfig, iteration);
        doIt(baseRun, outputConfig, run.getIteration(0));
        doIt(baseRun, uncongested.getBaseRun().getOutputConfig(), uncongested.getBaseRun().getIteration(0));

    }

    private static void doIt(final Scenario baseRun, Config outputConfig, IterationResource iteration) {
        final EventsManager workersEventsManager = EventsUtils.createEventsManager(outputConfig);
        final EventsManager nonWorkersEventsManager = EventsUtils.createEventsManager(outputConfig);


        EventsManager eventsManager = EventsUtils.createEventsManager(outputConfig);
        eventsManager.addHandler(new BasicEventHandler() {
            @Override
            public void handleEvent(Event event) {
                if (event instanceof HasPersonId) {
                    String id = ((HasPersonId) event).getPersonId().toString();
                    String originalId;
                    if (id.startsWith("I"))
                        originalId = id.substring(id.indexOf("_")+1);
                    else
                        originalId = id;
                    if (CountWorkers.isWorker(baseRun.getPopulation().getPersons().get(Id.createPersonId(originalId)))) {
                        workersEventsManager.processEvent(event);
                    } else {
                        nonWorkersEventsManager.processEvent(event);
                    }
                } else {
                    workersEventsManager.processEvent(event);
                    nonWorkersEventsManager.processEvent(event);
                }
            }

            @Override
            public void reset(int iteration) {

            }
        });

        LegHistogram workersLegHistogram = new LegHistogram(300);
        LegHistogram nonWorkersLegHistogram = new LegHistogram(300);
        workersEventsManager.addHandler(workersLegHistogram);
        nonWorkersEventsManager.addHandler(nonWorkersLegHistogram);


        new MatsimEventsReader(eventsManager).readFile(iteration.getEventsFileName());
        workersLegHistogram.write(iteration.getWd() + "/leghistogram-workers.txt");
        nonWorkersLegHistogram.write(iteration.getWd() + "/leghistogram-nonworkers.txt");
    }

}
