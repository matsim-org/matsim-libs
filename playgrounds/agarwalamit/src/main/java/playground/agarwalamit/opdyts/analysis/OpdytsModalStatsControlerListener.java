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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.analysis.tripDistance.LegModeBeelineDistanceDistributionFromPlansAnalyzer;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator;
import playground.agarwalamit.opdyts.equil.EquilMixedTrafficObjectiveFunctionPenalty;

/**
 * Created by amit on 20/09/16.
 */

public class OpdytsModalStatsControlerListener implements StartupListener, ShutdownListener, ReplanningListener {

    @Inject
    private EventsManager events;
    @Inject
    private Scenario scenario;

    public static final String OPDYTS_STATS_LABEL_STARTER = "iterationNr";
    public static final String OPDYTS_STATS_FILE_NAME = "opdyts_modalStats";

    private final DistanceDistribution referenceStudyDistri ;

    private final SortedMap<String, SortedMap<Double, Integer>> initialMode2DistanceClass2LegCount = new TreeMap<>();

    private BufferedWriter writer;
    private final Collection<String> mode2consider;

    /*
     * after every 10/20/50 iterations, distance distribution will be written out.
     */
    private final int distriEveryItr = 50;

    public OpdytsModalStatsControlerListener(final Collection<String> modes2consider, final DistanceDistribution referenceStudyDistri) {
        this.mode2consider = modes2consider;
        this.referenceStudyDistri = referenceStudyDistri;
    }

    public OpdytsModalStatsControlerListener() {
        this(Arrays.asList(TransportMode.car, TransportMode.pt), null);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/"+OPDYTS_STATS_FILE_NAME+".txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            StringBuilder stringBuilder = new StringBuilder(OPDYTS_STATS_LABEL_STARTER + "\t");
            mode2consider.stream().forEach(mode -> stringBuilder.append("legs_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("asc_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("util_trav_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("util_dist_"+mode+ "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append("money_dist_rate_"+mode+ "\t"));
            stringBuilder.append("objectiveFunctionValue"+"\t");
            stringBuilder.append("penaltyForObjectiveFunction"+"\t");
            stringBuilder.append("totalObjectiveFunctionValue");
            writer.write(stringBuilder.toString());
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

    @Override
    public void notifyReplanning(ReplanningEvent event) {

       final int iteration = event.getIteration();
       final Config config = event.getServices().getConfig();

        // initializing it here so that scenario can be accessed.
        List<Double> dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        LegModeBeelineDistanceDistributionFromPlansAnalyzer beelineDistanceDistributionHandler = new LegModeBeelineDistanceDistributionFromPlansAnalyzer(dists);
        beelineDistanceDistributionHandler.init(scenario);
        beelineDistanceDistributionHandler.preProcessData();
        beelineDistanceDistributionHandler.postProcessData();

        if (iteration == config.controler().getFirstIteration()) {
            this.initialMode2DistanceClass2LegCount.putAll(beelineDistanceDistributionHandler.getMode2DistanceClass2LegCount());
        } else {
            // nothing to do
        }

        ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(scenario.getPopulation());
        modalShareFromPlans.run();
        SortedMap<String, Integer> mode2Legs = modalShareFromPlans.getModeToNumberOfLegs();

        // evaluate objective function value
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = new ObjectiveFunctionEvaluator();
        Map<String, double[]> realCounts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
        Map<String, double[]> simCounts = new TreeMap<>();

        // initialize simcounts array for each mode
        realCounts.entrySet().forEach(entry -> simCounts.put(entry.getKey(), new double[entry.getValue().length]));

        SortedMap<String, SortedMap<Double, Integer>> simCountsHandler = beelineDistanceDistributionHandler.getMode2DistanceClass2LegCount();
        for (Map.Entry<String, SortedMap<Double, Integer>> e : simCountsHandler.entrySet()) {
            if (!realCounts.containsKey(e.getKey())) continue;
            double[] counts = new double[realCounts.get(e.getKey()).length];
            int index = 0;
            for (Integer count : simCountsHandler.get(e.getKey()).values()) {
                counts[index++] = count;
            }
            simCounts.put(e.getKey(), counts);
        }

        double objectiveFunctionValue = objectiveFunctionEvaluator.getObjectiveFunctionValue(realCounts,simCounts);
        double penalty = 0.;
        switch (this.referenceStudyDistri.getOpdytsScenario()) {
            case EQUIL:
            case PATNA_1Pct:
            case PATNA_10Pct:
                break;
            case EQUIL_MIXEDTRAFFIC:
                double ascBicycle = config.planCalcScore().getModes().get("bicycle").getConstant();
                double bicycleShare = objectiveFunctionEvaluator.getModeToShare().get("bicycle");
                penalty = EquilMixedTrafficObjectiveFunctionPenalty.getPenalty(bicycleShare, ascBicycle);
        }

        try {
            // write modalParams
            Map<String, PlanCalcScoreConfigGroup.ModeParams> mode2Params = config.planCalcScore().getModes();

            StringBuilder stringBuilder = new StringBuilder(iteration + "\t");
            mode2consider.stream().forEach(mode-> stringBuilder.append( mode2Legs.containsKey(mode) ? mode2Legs.get(mode) + "\t" : String.valueOf(0) + "\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getConstant()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMarginalUtilityOfTraveling()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMarginalUtilityOfDistance()+"\t"));
            mode2consider.stream().forEach(mode -> stringBuilder.append(mode2Params.get(mode).getMonetaryDistanceRate()+"\t"));
            stringBuilder.append(objectiveFunctionValue+"\t");
            stringBuilder.append(penalty+"\t");
            stringBuilder.append(String.valueOf(objectiveFunctionValue+penalty));

            writer.write(stringBuilder.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        if( iteration % distriEveryItr == 0 || iteration == config.controler().getLastIteration() ) {
            // dist-distribution file
            String distriDir = config.controler().getOutputDirectory() + "/distanceDistri/";
            new File(distriDir).mkdir();
            String outFile = distriDir + "/" + iteration + ".distanceDistri.txt";
            writeDistanceDistribution(outFile, beelineDistanceDistributionHandler.getMode2DistanceClass2LegCount());
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

    private void writeDistanceDistribution(String outputFile, final SortedMap<String, SortedMap<Double, Integer>> mode2dist2counts) {
        final BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFile);
        try {
            writer2.write( "distBins" + "\t" );
            for(Double d : referenceStudyDistri.getDistClasses()) {
                writer2.write(d + "\t");
            }
            writer2.newLine();

            // from initial plans
            {
                writer2.write("===== begin writing distribution from initial plans ===== ");
                writer2.newLine();

                for (String mode : this.initialMode2DistanceClass2LegCount.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : this.initialMode2DistanceClass2LegCount.get(mode).keySet()) {
                        writer2.write(this.initialMode2DistanceClass2LegCount.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from initial plans ===== ");
                writer2.newLine();
            }

            // from objective function
            {
                writer2.write("===== begin writing distribution from objective function ===== ");
                writer2.newLine();

                Map<String, double []> mode2counts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
                for (String mode : mode2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2counts.get(mode)) {
                        writer2.write(d + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from objective function ===== ");
                writer2.newLine();
            }

            // from simulation
            {
                writer2.write("===== begin writing distribution from simulation ===== ");
                writer2.newLine();

                for (String mode : mode2dist2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2dist2counts.get(mode).keySet()) {
                        writer2.write(mode2dist2counts.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from simulation ===== ");
                writer2.newLine();
            }
            writer2.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }
}
