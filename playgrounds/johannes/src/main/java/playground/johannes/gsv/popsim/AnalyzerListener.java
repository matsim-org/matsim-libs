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

package playground.johannes.gsv.popsim;

import playground.johannes.gsv.popsim.analysis.AnalyzerTask;
import playground.johannes.gsv.popsim.analysis.AnalyzerTaskRunner;
import playground.johannes.gsv.popsim.analysis.FileIOContext;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.MarkovEngineListener;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 */
public class AnalyzerListener implements MarkovEngineListener {

    private final AnalyzerTask task;

    private final long interval;

    private final AtomicLong iters = new AtomicLong();

    private final FileIOContext ioContext;

    public AnalyzerListener(AnalyzerTask task, FileIOContext ioContext, long interval) {
        this.ioContext = ioContext;
        this.interval = interval;
        this.task = task;

    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        if (iters.get() % interval == 0) {
            DecimalFormat df = new DecimalFormat("0");
            df.setMaximumFractionDigits(340);
            ioContext.append(df.format(iters.get()));
            AnalyzerTaskRunner.run(population, task, ioContext);

        }
        iters.incrementAndGet();
    }
}
