/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.processing;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.LoggerUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.util.Executor;

import java.util.*;

/**
 * @author johannes
 */
public class TaskRunner {

    private static final Logger logger = Logger.getLogger(TaskRunner.class);

    public static void run(PersonTask task, Collection<? extends Person> persons) {
        for (Person person : persons) task.apply(person);
    }

    public static void run(EpisodeTask task, Collection<? extends Person> persons) {
        run(task, persons, false);
    }

    public static void run(EpisodeTask task, Collection<? extends Person> persons, boolean verbose) {
        if (verbose) {
            ProgressLogger.init(persons.size(), 2, 10);
        }

        for (Person person : persons) {
            for (Episode plan : person.getEpisodes())
                task.apply(plan);

            if (verbose)
                ProgressLogger.step();
        }

        if (verbose)
            ProgressLogger.terminate();
    }

    public static void run(EpisodeTask task, Collection<? extends Person> persons, int nThreads, boolean verbose) {
        if (verbose) {
            ProgressLogger.init(persons.size(), 2, 10);
        }

        List<? extends Person>[] segments = CollectionUtils.split(persons, nThreads);
        List<Runnable> threads = new ArrayList<>(nThreads);
        for(int i = 0; i < nThreads; i++) {
            threads.add(new RunThread(segments[i], task, verbose));
        }

        Executor.submitAndWait(threads);

        if (verbose)
            ProgressLogger.terminate();
    }

    public static void runLegTask(SegmentTask task, Collection<? extends Person> persons) {
        run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment leg : episode.getLegs()) task.apply(leg);
            }
        }, persons);
    }

    public static void runActTask(SegmentTask task, Collection<? extends Person> persons) {
        run(new EpisodeTask() {
            @Override
            public void apply(Episode episode) {
                for(Segment act : episode.getActivities()) task.apply(act);
            }
        }, persons);
    }

    private static final class RunThread implements Runnable {

        private final List<? extends Person> persons;

        private final EpisodeTask task;

        private final boolean verbose;

        public RunThread(List<? extends Person> persons, EpisodeTask task, boolean verbose) {
            this.persons = persons;
            this.task = task;
            this.verbose = verbose;
        }

        @Override
        public void run() {
            for (Person person : persons) {
                for (Episode plan : person.getEpisodes())
                    task.apply(plan);

                if (verbose)
                    ProgressLogger.step();
            }
        }
    }

    public static void validatePersons(PersonTask task, Collection<? extends Person> persons) {
        LoggerUtils.disableNewLine();
        logger.info(String.format("Running validator %s...", task.getClass().getSimpleName()));

        Set<Person> delete = new HashSet<>(persons.size());

        run(task, persons);

        for (Person person : persons) {
            if (CommonValues.TRUE.equalsIgnoreCase(person.getAttribute(CommonKeys.DELETE))) {
                delete.add(person);
            }
        }

        for (Person person : delete) {
            persons.remove(person);
        }

        if (delete.size() > 0) System.out.println(String.format(" %s invalid persons.", delete.size()));
        else System.out.println(" ok.");
        LoggerUtils.enableNewLine();
    }

    public static void validateEpisodes(EpisodeTask task, Collection<? extends Person> persons) {
        LoggerUtils.disableNewLine();
        logger.info(String.format("Running validator %s...", task.getClass().getSimpleName()));

        run(task, persons);

        int cnt = 0;
        for (Person person : persons) {
            List<Episode> remove = new ArrayList<>();
            for (Episode plan : person.getEpisodes()) {
                if (CommonValues.TRUE.equalsIgnoreCase(plan.getAttribute(CommonKeys.DELETE))) {
                    remove.add(plan);
                    cnt++;
                }
            }

            for (Episode plan : remove) {
                person.getEpisodes().remove(plan);
            }

        }

        if (cnt > 0) {
            System.out.println(String.format(" %s invalid episodes.", cnt));
        } else {
            System.out.println(" ok.");
        }

        LoggerUtils.enableNewLine();
    }
}
