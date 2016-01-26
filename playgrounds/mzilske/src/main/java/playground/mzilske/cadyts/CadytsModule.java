/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.cadyts;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.car.PlansTranslatorBasedOnEvents;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import playground.mzilske.ant2014.StreamingOutput;
import playground.mzilske.util.IterationSummaryFileControlerListener;

public class CadytsModule extends AbstractModule {

    @Override
    public void install() {
        Multibinder<MeasurementLoader<Link>> measurementLoaderBinder = Multibinder.newSetBinder((Binder) binder(), new TypeLiteral<MeasurementLoader<Link>>(){});
        bind(AnalyticalCalibrator.class).toProvider(CalibratorProvider.class).in(Singleton.class);
        bind(PlansTranslatorBasedOnEvents.class).in(Singleton.class);
        bind(PlansTranslator.class).to(PlansTranslatorBasedOnEvents.class).in(Singleton.class);
        addControlerListenerBinding().to(CadytsControlerListener.class);
        addControlerListenerBinding().toProvider(MyControlerListenerProvider.class);
        addEventHandlerBinding().to(PlansTranslatorBasedOnEvents.class);
    }

    static class CalibratorProvider implements Provider<AnalyticalCalibrator<Link>> {

        @Inject Scenario scenario;
        @Inject Set<MeasurementLoader<Link>> measurementLoaders;

        @Override
        public AnalyticalCalibrator<Link> get() {
            CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
            LookUpItemFromId<Link> linkLookUp = new LookUpItemFromId<Link>() {
                @Override
                public Link getItem(Id id) {
                    return scenario.getNetwork().getLinks().get(id);
                }
            };
            Counts<Link> calibrationCounts = (Counts) scenario.getScenarioElement("calibrationCounts");
//            cadytsConfig.setCalibratedItems(calibrationCounts.getCounts().keySet());
            Set<String> links = new HashSet<>() ;
            for ( Id<Link> linkId : calibrationCounts.getCounts().keySet() ) {
            	links.add( linkId.toString() ) ;
            }
      	cadytsConfig.setCalibratedItems(links);
            AnalyticalCalibrator<Link> linkAnalyticalCalibrator = CadytsBuilder.buildCalibratorAndAddMeasurements(scenario.getConfig(), calibrationCounts, linkLookUp, Link.class);
            for (MeasurementLoader<Link> measurementLoader : measurementLoaders) {
                measurementLoader.load(linkAnalyticalCalibrator);
            }
            return linkAnalyticalCalibrator;
        }
    }

    static class MyControlerListenerProvider implements Provider<ControlerListener> {
        @Inject Scenario scenario;
        @Inject OutputDirectoryHierarchy controlerIO;
        @Inject VolumesAnalyzer volumesAnalyzer;
        @Override
        public ControlerListener get() {
            Map<String, IterationSummaryFileControlerListener.Writer> things = new HashMap<>();
            things.put("linkstats.txt", new IterationSummaryFileControlerListener.Writer() {
                @Override
                public StreamingOutput notifyStartup(StartupEvent event) {
                    return new StreamingOutput() {
                        @Override
                        public void write(PrintWriter pw) throws IOException {
                            pw.printf("%s\t%s\t%s\t%s\t%s\n",
                                    "iteration",
                                    "link",
                                    "hour",
                                    "sim.volume",
                                    "count.volume");
                        }
                    };
                }

                @Override
                public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                    CountsComparisonAlgorithm countsComparisonAlgorithm = new CountsComparisonAlgorithm(volumesAnalyzer, (Counts) scenario.getScenarioElement("counts"), scenario.getNetwork(), 1.0);
                    countsComparisonAlgorithm.run();
                    final List<CountSimComparison> comparison = countsComparisonAlgorithm.getComparison();
                    return new StreamingOutput() {
                        @Override
                        public void write(PrintWriter pw) throws IOException {
                            for (CountSimComparison countLink : comparison) {
                                pw.printf("%d\t%s\t%d\t%f\t%f\n",
                                        event.getIteration(),
                                        countLink.getId().toString(),
                                        countLink.getHour(),
                                        countLink.getSimulationValue(),
                                        countLink.getCountValue());
                            }
                        }
                    };
                }
            });
            return new IterationSummaryFileControlerListener(controlerIO, things);
        }
    }

}
