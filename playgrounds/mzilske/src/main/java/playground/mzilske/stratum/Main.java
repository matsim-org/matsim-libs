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

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.*;
import playground.mzilske.clones.ClonesConfigGroup;
import playground.mzilske.clones.ClonesModule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args) {
        // run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/random-zeiten/10-count", Arrays.asList(10));
        run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/wurst/8-count", Arrays.asList(8));
        run("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/illustrative/wurst/18-count", Arrays.asList(18));
    }

    private static void run(final String outputDirectory, List<Integer> countHours) {
        AbstractModule phoneModule = new AbstractModule() {
            @Override
            public void install() {
                bind(ZoneTracker.LinkToZoneResolver.class).to(MyLinkToZoneResolver.class);
                bind(CallBehavior.class).to(MyCallBehavior.class);
            }
        };

        Scenario groundTruth = new OneWorkplaceOneStratumUnderestimated().get();
        groundTruth.getConfig().controler().setOutputDirectory(outputDirectory + "-orig");
        Controler controler = new Controler(groundTruth);
        controler.setModules(
                new ControlerDefaultsModule(),
                new CollectSightingsModule(),
                phoneModule);
        controler.run();

        final VolumesAnalyzer groundTruthVolumes = controler.getVolumes();

        final Counts allCounts = CompareMain.volumesToCounts(groundTruth.getNetwork(), groundTruthVolumes, 1.0);
        final Counts calibrationCounts = filterCounts(allCounts, countHours);

        Scenario cdrScenario = new ScenarioReconstructor(groundTruth.getNetwork(), (Sightings) groundTruth.getScenarioElement("sightings"), new MyLinkToZoneResolver()).get();
        cdrScenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);
        cdrScenario.addScenarioElement("calibrationCounts", calibrationCounts);
        cdrScenario.getConfig().controler().setOutputDirectory(outputDirectory);
        ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(cdrScenario.getConfig(), ClonesConfigGroup.NAME, ClonesConfigGroup.class);
        clonesConfig.setCloneFactor(2.0);

        Controler controler2 = new Controler(cdrScenario);
        controler2.setModules(
                new ControlerDefaultsModule(),
                phoneModule,
                new CadytsModule(),
                new ClonesModule());
        controler2.setScoringFunctionFactory(new MyScoringFunctionFactory());
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
