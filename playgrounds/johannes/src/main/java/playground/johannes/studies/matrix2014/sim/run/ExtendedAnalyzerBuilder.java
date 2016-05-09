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
package playground.johannes.studies.matrix2014.sim.run;

import org.matsim.core.config.Config;
import playground.johannes.studies.matrix2014.analysis.*;
import playground.johannes.studies.matrix2014.config.MatrixAnalyzerConfigurator;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayerLoader;
import playground.johannes.studies.matrix2014.matrix.MatrixSamplerFactory;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.ZoneDistancePredicate;
import playground.johannes.studies.matrix2014.sim.GSVMatrixSampler;
import playground.johannes.synpop.analysis.AnalyzerTaskComposite;
import playground.johannes.synpop.analysis.ConcurrentAnalyzerTask;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneData;
import playground.johannes.synpop.gis.ZoneDataLoader;
import playground.johannes.synpop.sim.PopulationWriter;

import java.util.Collection;

/**
 * @author jillenberger
 */
public class ExtendedAnalyzerBuilder {

    public static void build(Simulator engine, Config config) {
        AnalyzerTaskComposite<Collection<? extends Person>> task = engine.getAnalyzerTasks();

        ZoneData zoneData = (ZoneData) engine.getDataPool().get(ZoneDataLoader.KEY);
        ActivityLocationLayer locations = (ActivityLocationLayer) engine.getDataPool().get(ActivityLocationLayerLoader.KEY);

        DefaultMatrixBuilderFactory matrixBuilderFactory = new DefaultMatrixBuilderFactory();
        /*
        matrix comparators
         */
        ConcurrentAnalyzerTask<Collection<? extends Person>> matrixTasks = new ConcurrentAnalyzerTask<>();
        MatrixComparator mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerITP"),
                engine.getDataPool(),
                matrixBuilderFactory,
                engine.getIOContext()).load();
        mAnalyzer.setLegPredicate(engine.getLegPredicate());
        mAnalyzer.setUseWeights(engine.getUseWeights());
        matrixTasks.addComponent(mAnalyzer);


        ZoneCollection tomtomZones = zoneData.getLayer("tomtom");

        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerTomTom"),
                engine.getDataPool(),
                matrixBuilderFactory,
                engine.getIOContext()).load();
        mAnalyzer.setLegPredicate(engine.getLegPredicate());
        mAnalyzer.setUseWeights(engine.getUseWeights());

        ODPredicate distPredicate = new ZoneDistancePredicate(tomtomZones, 100000);
        mAnalyzer.setNormPredicate(distPredicate);

        matrixTasks.addComponent(mAnalyzer);
        /*
        matrix writer
         */
        MatrixBuilder tomtomBuilder = matrixBuilderFactory.create(locations, tomtomZones);
        tomtomBuilder.setLegPredicate(engine.getLegPredicate());
        tomtomBuilder.setUseWeights(engine.getUseWeights());

        MatrixWriter matrixWriter = new MatrixWriter(tomtomBuilder, engine.getIOContext());
        matrixTasks.addComponent(matrixWriter);

        task.addComponent(new AnalyzerTaskGroup<>(matrixTasks, engine.getIOContext(), "matrix"));
        /*
        population writer
         */
        task.addComponent(new PopulationWriter(engine.getIOContext()));


        long start = (long) Double.parseDouble(config.findParam(Simulator.MODULE_NAME, "matrixSamplingStart"));
        long step = (long) Double.parseDouble(config.findParam(Simulator.MODULE_NAME, "matrixSamplingStep"));
        MatrixSamplerFactory nuts3Sampler = new MatrixSamplerFactory(start, step, engine.getEngineListeners());

        ConcurrentAnalyzerTask<Collection<? extends Person>> samplerTasks = new ConcurrentAnalyzerTask<>();
        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerITP"),
                engine.getDataPool(),
                nuts3Sampler,
                engine.getIOContext()).load();
        mAnalyzer.setLegPredicate(engine.getLegPredicate());
        mAnalyzer.setUseWeights(engine.getUseWeights());

        samplerTasks.addComponent(mAnalyzer);

        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerTomTom"),
                engine.getDataPool(),
                nuts3Sampler,
                engine.getIOContext()).load();
        mAnalyzer.setLegPredicate(engine.getLegPredicate());
        mAnalyzer.setUseWeights(engine.getUseWeights());

        mAnalyzer.setNormPredicate(distPredicate);
        samplerTasks.addComponent(mAnalyzer);
        /*
        matrix writer
         */
        MatrixBuilder mBuilder = nuts3Sampler.create(locations, tomtomZones);
        mBuilder.setLegPredicate(engine.getLegPredicate());
        mBuilder.setUseWeights(engine.getUseWeights());
        matrixWriter = new MatrixWriter(mBuilder, engine.getIOContext());
        samplerTasks.addComponent(matrixWriter);

        task.addComponent(new AnalyzerTaskGroup<>(samplerTasks, engine.getIOContext(), "matrixAvr"));

        GSVMatrixSampler gsvSampler = new GSVMatrixSampler(engine.getRefPersons(),
                engine.getDataPool(),
                "modena",
                engine.getRandom(),
                start,
                step,
                engine.getIOContext());
        engine.getEngineListeners().addComponent(gsvSampler);
        task.addComponent(gsvSampler);
    }
}
