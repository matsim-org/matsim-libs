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
package playground.johannes.gsv.popsim.analysis;

import org.matsim.contrib.common.collections.Composite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author jillenberger
 */
public class ConcurrentAnalyzerTask<T> extends Composite<AnalyzerTask<T>> implements AnalyzerTask<T> {

    @Override
    public void analyze(final T object, final List<StatsContainer> containers) {
        final List<StatsContainer> concurrentContainers = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>(components.size());

        for (final AnalyzerTask<T> task : components) {
            futures.add(Executor.submit(new Runnable() {
                @Override
                public void run() {
                    task.analyze(object, concurrentContainers);
                }
            }));
        }

        for(Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        containers.addAll(concurrentContainers);
    }
}
