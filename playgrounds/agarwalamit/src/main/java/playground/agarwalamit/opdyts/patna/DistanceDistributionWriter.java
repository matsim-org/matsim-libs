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

package playground.agarwalamit.opdyts.patna;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionFromPlansAnalyzer;
import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionHandler;
import playground.agarwalamit.opdyts.OpdytsObjectiveFunctionCases;

/**
 * Created by amit on 20/10/16.
 */

public class DistanceDistributionWriter {

    private BufferedWriter writer ;
    private final Scenario scenario;
    private final PatnaCMPDistanceDistribution referenceStudyDistri ;
    private final LegModeBeelineDistanceDistributionHandler handler ;
    private final SortedMap<String, SortedMap<Double, Integer>> mode2DistanceClass2LegCount = new TreeMap<>();

    public DistanceDistributionWriter(final Scenario scenario, final OpdytsObjectiveFunctionCases opdytsObjectiveFunctionCases) {
        referenceStudyDistri = new PatnaCMPDistanceDistribution(opdytsObjectiveFunctionCases);
        this.scenario = scenario;
        List<Double> dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        this.handler = new LegModeBeelineDistanceDistributionHandler(dists, scenario.getNetwork());

        // following is done in pre-process because, it should remain unchanged afterwards
        LegModeBeelineDistanceDistributionFromPlansAnalyzer distributionFromPlansAnalyzer = new LegModeBeelineDistanceDistributionFromPlansAnalyzer(dists);
        distributionFromPlansAnalyzer.init(this.scenario);
        distributionFromPlansAnalyzer.preProcessData();
        distributionFromPlansAnalyzer.postProcessData();
        mode2DistanceClass2LegCount.putAll( distributionFromPlansAnalyzer.getMode2DistanceClass2LegCount() );
    }

    public EventHandler getEventHandler() {
        return this.handler;
    }

    public void writeResults(String outputFile) {
        writer = IOUtils.getBufferedWriter(outputFile);
        try {
            writer.write( "distBins" + "\t" );
            for(Double d : referenceStudyDistri.getDistClasses()) {
                writer.write(d + "\t");
            }
            writer.newLine();

            // from initial plans
            {
                writer.write("===== begin writing distribution from initial plans ===== ");
                writer.newLine();

                for (String mode : this.mode2DistanceClass2LegCount.keySet()) {
                    writer.write(mode + "\t");
                    for (Double d : this.mode2DistanceClass2LegCount.get(mode).keySet()) {
                        writer.write(this.mode2DistanceClass2LegCount.get(mode).get(d) + "\t");
                    }
                    writer.newLine();
                }

                writer.write("===== end writing distribution from initial plans ===== ");
                writer.newLine();
            }

            // from objective function
            {
                writer.write("===== begin writing distribution from objective function ===== ");
                writer.newLine();

                SortedMap<String, double []> mode2counts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
                for (String mode : mode2counts.keySet()) {
                    writer.write(mode + "\t");
                    for (Double d : mode2counts.get(mode)) {
                        writer.write(d + "\t");
                    }
                    writer.newLine();
                }

                writer.write("===== end writing distribution from objective function ===== ");
                writer.newLine();
            }

            // from simulation
            {
                writer.write("===== begin writing distribution from simulation ===== ");
                writer.newLine();

                SortedMap<String, SortedMap<Double, Integer>> mode2dist2counts = this.handler.getMode2DistanceClass2LegCounts();
                for (String mode : mode2dist2counts.keySet()) {
                    writer.write(mode + "\t");
                    for (Double d : mode2dist2counts.get(mode).keySet()) {
                        writer.write(mode2dist2counts.get(mode).get(d) + "\t");
                    }
                    writer.newLine();
                }

                writer.write("===== end writing distribution from simulation ===== ");
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }
}
