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

package playground.johannes.studies.matrix2014.matrix.io;

import org.matsim.facilities.ActivityFacilities;
import playground.johannes.studies.matrix2014.analysis.MatrixBuilder;
import playground.johannes.studies.matrix2014.matrix.DefaultMatrixBuilder;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.sim.MarkovEngineListener;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.Collection;

/**
 * @author johannes
 */
public class CachedMatrixBuilder implements MatrixBuilder, MarkovEngineListener {

    private long iteration = 0;

    private long lastBuilt = -1;

    private NumericMatrix matrix;

    private final DefaultMatrixBuilder matrixBuilder;

    private final Collection<? extends Person> population;

    private Predicate<Segment> legPredicate;

    private boolean useWeights = true;

    public CachedMatrixBuilder(Collection<? extends Person> population, ActivityFacilities facilities, ZoneCollection zones) {
        this.population = population;
        matrixBuilder = new DefaultMatrixBuilder(facilities, zones);
    }

    public void setLegPredicate(Predicate<Segment> predicate) {
        this.legPredicate = predicate;
    }

    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        iteration++;
    }

    @Override
    public NumericMatrix build(Collection<? extends Person> population) {
        if(this.population != population) throw new RuntimeException("Trying to build a matrix from different population!");

        if(lastBuilt < iteration) {
            matrix = matrixBuilder.build(this.population, legPredicate, useWeights);
            lastBuilt = iteration;
        }

        return matrix;
    }
}
