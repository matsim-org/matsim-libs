/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * main.java
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

package playground.mzilske.stratum;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.CDRModule;
import playground.mzilske.cdr.CallBehavior;
import playground.mzilske.cdr.CompareMain;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.clones.ClonesModule;
import playground.mzilske.controller.CharyparNagelModule;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class Main {

    public static void main(String[] args) {
        // run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/random-zeiten/10-count", Arrays.asList(10));
        run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/wurst/8-count", Arrays.asList(8));
        run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/wurst/18-count", Arrays.asList(18));
    }

    private static void run(final String outputDirectory, List<Integer> countHours) {
        Module phoneModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ZoneTracker.LinkToZoneResolver.class).to(MyLinkToZoneResolver.class);
                bind(CallBehavior.class).to(MyCallBehavior.class);
            }
        };

        Injector injector = Guice.createInjector(
                new ControllerModule(),
                new CDRModule(),
                new CharyparNagelModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Config.class).toProvider(OneWorkplaceOneStratumUnderestimated.ConfigProvider.class).in(Singleton.class);
                        bind(Scenario.class).toProvider(OneWorkplaceOneStratumUnderestimated.class).in(Singleton.class);
                    }
                },
                phoneModule
        );


        Controller controler = injector.getInstance(Controller.class);
        controler.run();

        final Network groundTruthNetwork = injector.getInstance(Network.class);
        final VolumesAnalyzer groundTruthVolumes = injector.getInstance(VolumesAnalyzer.class);
        final CompareMain compareMain = injector.getInstance(CompareMain.class);

        final Counts allCounts = CompareMain.volumesToCounts(groundTruthNetwork, groundTruthVolumes);
        final Counts calibrationCounts = filterCounts(allCounts, countHours);

        Injector injector2 = Guice.createInjector(
                new ControllerModule(),
                new CadytsModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(String.class).annotatedWith(Names.named("outputDirectory")).toInstance(outputDirectory);
                        bind(Network.class).annotatedWith(Names.named("groundTruthNetwork")).toInstance(groundTruthNetwork);
                        bind(VolumesAnalyzer.class).annotatedWith(Names.named("groundTruthVolumes")).toInstance(groundTruthVolumes);
                        bind(CompareMain.class).toInstance(compareMain);
                        bind(Double.class).annotatedWith(Names.named("clonefactor")).toInstance(2.0);
                        bind(Double.class).annotatedWith(Names.named("cadytsweight")).toInstance(1.0);
                        bind(Config.class).toProvider(ScenarioReconstructor.ConfigProvider.class).in(Singleton.class);
                        bind(Scenario.class).toProvider(ScenarioReconstructor.class).in(Singleton.class);
                        bind(Counts.class).annotatedWith(Names.named("allCounts")).toInstance(allCounts);
                        bind(Counts.class).annotatedWith(Names.named("calibrationCounts")).toInstance(calibrationCounts);
                        bind(ScoringFunctionFactory.class).to(MyScoringFunctionFactory.class);
                    }
                },
                new ClonesModule(),
                phoneModule
        );

        Controller controler2 = injector2.getInstance(Controller.class);
        controler2.run();
    }

    private static Counts filterCounts(Counts allCounts, List<Integer> countHours) {
        Counts someCounts = new Counts();
        for (Map.Entry<Id<Link>, Count> entry : allCounts.getCounts().entrySet()) {
            String linkId = entry.getKey().toString();
            if (linkId.equals("1") || linkId.equals("21")) {
                Count count = someCounts.createAndAddCount(Id.create(linkId, Link.class), "wurst");
                for (Map.Entry<Integer, Volume> volume : entry.getValue().getVolumes().entrySet()) {
                    if (countHours.contains(volume.getKey())) {
                        count.createVolume(volume.getKey(), volume.getValue().getValue());
                    }
                }
            }
        }
        return someCounts;
    }

}
