/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.util.Collection;

/**
 * @author johannes
 */
public class TransitionLogger implements MarkovEngineListener {

    private static final Logger logger = Logger.getLogger(TransitionLogger.class);

    private long acceptedIterations;

    private long rejectedIterations;

    private long interval;

    private long time;

    public TransitionLogger(long interval) {
        this.interval = interval;
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        if(accepted) acceptedIterations++;
        else rejectedIterations++;

        if((acceptedIterations + rejectedIterations) % interval == 0) {
            double stepsPerSec = (acceptedIterations + rejectedIterations)/ (System.currentTimeMillis() - time);
            double ratio = acceptedIterations/(double)(acceptedIterations + rejectedIterations);
            logger.info(String.format("Steps accepted %s, rejected %s, ratio %.4f, steps per msec %s.", acceptedIterations,
                    rejectedIterations, ratio, stepsPerSec));
            acceptedIterations = 0;
            rejectedIterations = 0;
            time = System.currentTimeMillis();
        }
    }
}
