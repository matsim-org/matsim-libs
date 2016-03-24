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
import playground.johannes.studies.matrix2014.analysis.AnalyzerTaskGroup;
import playground.johannes.studies.matrix2014.analysis.MatrixComparator;
import playground.johannes.studies.matrix2014.analysis.MatrixWriter;
import playground.johannes.studies.matrix2014.config.MatrixAnalyzerConfigurator;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayerLoader;
import playground.johannes.studies.matrix2014.matrix.DefaultMatrixBuilder;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.ZoneDistancePredicate;
import playground.johannes.studies.matrix2014.sim.MatrixSampler;
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

//        FacilityData facilityData = (FacilityData) engine.getDataPool().get(FacilityDataLoader.KEY);
        ZoneData zoneData = (ZoneData) engine.getDataPool().get(ZoneDataLoader.KEY);
        ActivityLocationLayer locations = (ActivityLocationLayer) engine.getDataPool().get(ActivityLocationLayerLoader.KEY);

        // FIXME: requires consolidation!
//        CachedMatrixBuilder builder = new CachedMatrixBuilder(engine.getSimPersons(), facilityData.getAll(), zoneData.getLayer("nuts3"));
        DefaultMatrixBuilder nuts3Builder = new DefaultMatrixBuilder(locations, zoneData.getLayer("nuts3"), "nuts3");
        nuts3Builder.setLegPredicate(engine.getLegPredicate());
        nuts3Builder.setUseWeights(engine.getUseWeights());
        /*
        matrix comparators
         */
        ConcurrentAnalyzerTask<Collection<? extends Person>> matrixTasks = new ConcurrentAnalyzerTask<>();
        MatrixComparator mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerITP"),
                engine.getDataPool(),
                engine.getIOContext(),
                nuts3Builder).load();
//        mAnalyzer.setLegPredicate(engine.getLegPredicate());
//        mAnalyzer.setUseWeights(engine.getUseWeights());
        matrixTasks.addComponent(mAnalyzer);


        ZoneCollection tomtomZones = zoneData.getLayer("tomtom");
        DefaultMatrixBuilder tomtomBuilder = new DefaultMatrixBuilder(locations, tomtomZones, "tomtom");
        tomtomBuilder.setLegPredicate(engine.getLegPredicate());
        tomtomBuilder.setUseWeights(engine.getUseWeights());
        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerTomTom"),
                engine.getDataPool(),
                engine.getIOContext(),
                tomtomBuilder).load();
//        mAnalyzer.setLegPredicate(engine.getLegPredicate());

//        ZoneData zoneData = (ZoneData) engine.getDataPool().get(ZoneDataLoader.KEY);

        ODPredicate distPredicate = new ZoneDistancePredicate(tomtomZones, 100000);

        mAnalyzer.setNormPredicate(distPredicate);
//        mAnalyzer.setUseWeights(engine.getUseWeights());
        matrixTasks.addComponent(mAnalyzer);
        /*
        matrix writer
         */
//        ActivityFacilities facilities = ((FacilityData) engine.getDataPool().get(FacilityDataLoader.KEY)).getAll();
        MatrixWriter matrixWriter = new MatrixWriter(tomtomBuilder, engine.getIOContext());
//        matrixWriter.setPredicate(engine.getLegPredicate());
//        matrixWriter.setUseWeights(engine.getUseWeights());
        matrixTasks.addComponent(matrixWriter);

        task.addComponent(new AnalyzerTaskGroup<>(matrixTasks, engine.getIOContext(), "matrix"));
        /*
        population writer
         */
        task.addComponent(new PopulationWriter(engine.getIOContext()));
        /*
        facility based distance
         */
//        HistogramWriter histogramWriter = new HistogramWriter(engine.getIOContext(), new StratifiedDiscretizerBuilder(100, 100));
//        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new LinearDiscretizer(50000), "linear"));
//        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new FixedBordersDiscretizer(new double[]{-1,
//                100000, Integer.MAX_VALUE}), "100KM"));
//
//        FacilityData fData = (FacilityData) engine.getDataPool().get(FacilityDataLoader.KEY);
//        NumericAnalyzer actDist = new ActDistanceBuilder()
//                .setHistogramWriter(histogramWriter)
//                .setPredicate(engine.getLegPredicate(), engine.getLegPredicateName())
//                .setUseWeights(engine.getUseWeights())
//                .build(fData.getAll());
//        task.addComponent(actDist);

        long start = (long) Double.parseDouble(config.findParam(Simulator.MODULE_NAME, "matrixSamplingStart"));
        long step = (long) Double.parseDouble(config.findParam(Simulator.MODULE_NAME, "matrixSamplingStep"));

        MatrixSampler nuts3Sampler = new MatrixSampler(nuts3Builder, start, step);
        engine.getEngineListeners().addComponent(nuts3Sampler);

        ConcurrentAnalyzerTask<Collection<? extends Person>> samplerTasks = new ConcurrentAnalyzerTask<>();
        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerITP"),
                engine.getDataPool(),
                engine.getIOContext(),
                nuts3Sampler).load();
        samplerTasks.addComponent(mAnalyzer);

        MatrixSampler tomtomSampler = new MatrixSampler(tomtomBuilder, start, step);
        engine.getEngineListeners().addComponent(tomtomSampler);

        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(
                config.getModule("matrixAnalyzerTomTom"),
                engine.getDataPool(),
                engine.getIOContext(),
                tomtomSampler).load();

        mAnalyzer.setNormPredicate(distPredicate);
        samplerTasks.addComponent(mAnalyzer);
        /*
        matrix writer
         */
        matrixWriter = new MatrixWriter(nuts3Sampler, engine.getIOContext());
        samplerTasks.addComponent(matrixWriter);

        task.addComponent(new AnalyzerTaskGroup<>(samplerTasks, engine.getIOContext(), "matrixAvr"));
    }
}
