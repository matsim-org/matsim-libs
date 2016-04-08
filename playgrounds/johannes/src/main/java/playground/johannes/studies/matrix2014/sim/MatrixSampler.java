/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.sim;

import org.apache.log4j.Logger;
import playground.johannes.studies.matrix2014.analysis.MatrixBuilder;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.sim.MarkovEngineListener;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class MatrixSampler implements MatrixBuilder, MarkovEngineListener {

    private static final Logger logger = Logger.getLogger(MatrixSampler.class);

    private final long start;

    private final long step;

    private long iteration;

    private final NumericMatrix sumMatrix = new NumericMatrix();

    private NumericMatrix avrMatrix;

    private int sampleSize;

    private MatrixBuilder builder;

    public MatrixSampler(MatrixBuilder builder, long start, long step) {
        this.builder = builder;
        this.start = start;
        this.step = step;
        avrMatrix = new NumericMatrix();

    }

    public void drawSample(Collection<? extends Person> persons) {
        NumericMatrix sample = builder.build(persons);

        Set<String> keys = sample.keys();
        for(String i : keys) {
            for(String j : keys) {
                Double vol = sample.get(i, j);
                if(vol != null) {
                    sumMatrix.add(i, j, vol);
                }
            }
        }

        sampleSize++;

        avrMatrix = new NumericMatrix();
        keys = sumMatrix.keys();
        for(String i : keys) {
            for(String j : keys) {
                Double vol = sumMatrix.get(i, j);
                if(vol != null) {
                    avrMatrix.set(i, j, vol/(double)sampleSize);
                }
            }
        }
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        iteration++;

        if(iteration >= start && iteration % step == 0) {
            logger.debug(String.format("Drawing matrix sample. Current sample size = %s.", sampleSize));
            drawSample(population);
            logger.debug("Done drawing matrix sample.");
        }
    }

    @Override
    public void setLegPredicate(Predicate<Segment> predicate) {
        builder.setLegPredicate(predicate);
    }

    @Override
    public void setUseWeights(boolean useWeights) {
        builder.setUseWeights(useWeights);
    }

    @Override
    public NumericMatrix build(Collection<? extends Person> population) {
        return avrMatrix;
    }
}
