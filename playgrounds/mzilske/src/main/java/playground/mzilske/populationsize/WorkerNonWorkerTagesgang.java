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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import playground.mzilske.ant2014.IterationResource;

import java.util.Arrays;

public class WorkerNonWorkerTagesgang {

    interface Predicate {
        boolean test(Id<Person> personId);
    }

    public static void main(String[] args) {
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource uncongested = experiment.getRegime("uncongested3");
        final Scenario baseRun = uncongested.getBaseRun().getOutputScenario();




        Predicate predicate;
        predicate = new Predicate() {
            @Override
            public boolean test(Id<Person> personId) {
                return isWorker(personId, baseRun);
            }
        };


        doIt(predicate, baseRun.getConfig(), uncongested.getBaseRun().getIteration(0));
        for (String rate : Arrays.asList("5", "0")) {
            final MultiRateRunResource multiRateRun = uncongested.getMultiRateRun("randomcountlocations1.0");
            final RunResource run = multiRateRun.getRateRun(rate, "3");
            IterationResource iteration = run.getLastIteration();
            doIt(predicate, baseRun.getConfig(), iteration);
            doIt(predicate, baseRun.getConfig(), run.getIteration(0));
        }
    }

    private static void doIt(final Predicate isHeavyPhoner, Config outputConfig, IterationResource iteration) {
        final EventsManager workersEventsManager = EventsUtils.createEventsManager(outputConfig);
        final EventsManager nonWorkersEventsManager = EventsUtils.createEventsManager(outputConfig);
        EventsManager eventsManager = EventsUtils.createEventsManager(outputConfig);
        eventsManager.addHandler(new BasicEventHandler() {
            @Override
            public void handleEvent(Event event) {
                if (event instanceof HasPersonId) {
                    Id<Person> personId = ((HasPersonId) event).getPersonId();
                    if (isHeavyPhoner.test(personId)) {
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

    private static boolean isWorker(Id<Person> personId, Scenario baseRun) {
        Id<Person> originalId = resolvePerson(personId);
        return CountWorkers.isWorker(baseRun.getPopulation().getPersons().get(originalId));
    }

    private static Id<Person> resolvePerson(Id<Person> personId) {
        String id = personId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_")+1);
        else
            originalId = id;
        return Id.createPersonId(originalId);
    }

}
