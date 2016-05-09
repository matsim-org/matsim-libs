/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.matrix;

import playground.johannes.studies.matrix2014.analysis.MatrixBuilder;
import playground.johannes.studies.matrix2014.analysis.MatrixBuilderFactory;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.sim.MatrixSampler;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.sim.MarkovEngineListenerComposite;

/**
 * @author jillenberger
 */
public class MatrixSamplerFactory implements MatrixBuilderFactory {

    private final long start;

    private final long step;

    private final MarkovEngineListenerComposite listeners;

    public  MatrixSamplerFactory(long start, long step, MarkovEngineListenerComposite listeners) {
        this.start = start;
        this.step = step;
        this.listeners = listeners;
    }
    @Override
    public MatrixBuilder create(ActivityLocationLayer locations, ZoneCollection zones) {
        MatrixSampler sampler = new MatrixSampler(new DefaultMatrixBuilder(locations, zones), start, step);
        listeners.addComponent(sampler);
        return sampler;
    }
}
