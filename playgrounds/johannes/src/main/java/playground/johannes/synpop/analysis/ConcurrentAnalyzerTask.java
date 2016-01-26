/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.analysis;

import org.apache.log4j.Logger;
import playground.johannes.synpop.util.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author jillenberger
 */
public class ConcurrentAnalyzerTask<T> extends AnalyzerTaskComposite<T> {

    private static final Logger logger = Logger.getLogger(ConcurrentAnalyzerTask.class);

    @Override
    public void analyze(final T object, final List<StatsContainer> containers) {
        final List<StatsContainer> concurrentContainers = new CopyOnWriteArrayList<>();

        List<Runnable> runnables = new ArrayList<>(components.size());
        for (final AnalyzerTask<T> task : components) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    task.analyze(object, concurrentContainers);
                }
            });
            logger.trace(String.format("Submitting analyzer task %s...", task.getClass().getSimpleName()));
        }

        Executor.submitAndWait(runnables);

        containers.addAll(concurrentContainers);

        logger.trace("Tasks done.");
    }
}
