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

package playground.johannes.synpop.sim;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 */
public class HamiltonianLogger implements MarkovEngineListener {

    private static final Logger logger = Logger.getLogger(HamiltonianLogger.class);

    private final Hamiltonian h;

    private final long logInterval;

    private AtomicLong iter = new AtomicLong();

    private BufferedWriter writer;

    private static final String TAB = "\t";

    private final String outdir;

    public HamiltonianLogger(Hamiltonian h, int logInterval) {
        this(h, logInterval, null);
    }

    public HamiltonianLogger(Hamiltonian h, long logInterval, String outdir) {
        this.h = h;
        this.logInterval = logInterval;
        this.outdir = outdir;

        if (outdir != null) {
            try {
                writer = new BufferedWriter(new FileWriter(outdir + "/" + h.getClass().getSimpleName() + ".txt"));
                writer.write("iter\th");
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        if (iter.get() % logInterval == 0) {
            long iterNow = iter.get();
            double hVal = h.evaluate(population);
            logger.info(String.format("%s [%.0E]: %s", h.getClass().getSimpleName(), (double)iterNow, hVal));

            if (writer != null) {
                try {
                    writer.write(String.valueOf(iterNow));
                    writer.write(TAB);
                    writer.write(String.valueOf(hVal));
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        iter.incrementAndGet();
    }
}
