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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 */
public class HamiltonianLogger implements MarkovEngineListener {

    private static final Logger logger = Logger.getLogger(HamiltonianLogger.class);

    private final Hamiltonian h;

    private final long logInterval;

    private final long startIteration;

    private AtomicLong iter = new AtomicLong();

    private BufferedWriter writer;

    private static final String TAB = "\t";

    private final String outdir;

    private final DecimalFormat format;

    private final String name;

    public HamiltonianLogger(Hamiltonian h, int logInterval, String name) {
        this(h, logInterval, name, null);
    }

    public HamiltonianLogger(Hamiltonian h, long logInterval, String name, String outdir) {
        this(h, logInterval, name, outdir, 0);
    }

    public HamiltonianLogger(Hamiltonian h, long logInterval, String name, String outdir, long startIteration) {
        this.h = h;
        this.logInterval = logInterval;
        this.startIteration = startIteration;
        this.outdir = outdir;

        if(name == null)
            this.name = h.getClass().getSimpleName();
        else
            this.name = name;

        format = new DecimalFormat("0E0", new DecimalFormatSymbols(Locale.US));
        format.setMaximumFractionDigits(340);

        if (outdir != null) {
            try {
                writer = new BufferedWriter(new FileWriter(outdir + "/" + name + ".txt"));
                writer.write("iter\th");
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        if(iter.get() % logInterval == 0) {
            long iterNow = iter.get();
            if (iterNow >= startIteration) {
                double hVal = h.evaluate(population);
                logger.info(String.format("%s [%s]: %s", name, format.format(iterNow), hVal));

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
            } else {
                logger.info(String.format("%s [%s]: <<inactive>>", name, format.format(iterNow)));
            }
        }

        iter.incrementAndGet();
    }
}
