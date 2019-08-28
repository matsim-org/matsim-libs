/* *********************************************************************** *
 * project: org.matsim.*
 * PKMbyModeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.StackedBarChart;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * analyses passenger kilometer traveled based on experienced plans.
 * @author jbischoff
 */
public class PKMbyModeCalculator {

    private final Map<Integer,Map<String,Double>> pmtPerIteration = new TreeMap<>();
    private final boolean writePng;
    private final OutputDirectoryHierarchy controlerIO;
    private final static char DEL = '\t';
    private final DecimalFormat df = new DecimalFormat();
    final static String FILENAME = "pkm_ModeStats";


    @Inject
    PKMbyModeCalculator(ControlerConfigGroup controlerConfigGroup, OutputDirectoryHierarchy controlerIO) {
        writePng = controlerConfigGroup.isCreateGraphs();
        this.controlerIO = controlerIO;
    }

    void addIteration(int iteration, Map<Id<Person>, Plan> map) {
        Map<String,Double> pmtbyMode = map.values()
                .parallelStream()
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(Leg.class::isInstance)
                .map(l->{
                    Leg leg = (Leg) l;
                    double dist = leg.getRoute()!=null?leg.getRoute().getDistance():0;
                    if (Double.isNaN(dist)) {dist = 0.0; }
                    return new AbstractMap.SimpleEntry<>(leg.getMode(),dist);
                })
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue(),(a,b)->a+b));
        pmtPerIteration.put(iteration,pmtbyMode);
    }


    void writeOutput() {
        writeVKTText();

    }

    private void writeVKTText() {
        TreeSet<String> allModes = new TreeSet<>();
        allModes.addAll(this.pmtPerIteration.values()
                .stream()
                .flatMap(i->i.keySet().stream())
                .collect(Collectors.toSet()));

        try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getOutputFilename(FILENAME + ".txt"))), CSVFormat.DEFAULT.withDelimiter(DEL))) {
            csvPrinter.print("Iteration");
            csvPrinter.printRecord(allModes);

            for (Map.Entry<Integer,Map<String,Double>> e : pmtPerIteration.entrySet()){
                csvPrinter.print(e.getKey());
                for (String mode : allModes){
                    csvPrinter.print(df.format(e.getValue().getOrDefault(mode,0.0)/1000.0));
                }
                csvPrinter.println();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (writePng){
            String[] categories = new String[pmtPerIteration.size()];
            int i = 0;
            for (Integer it : pmtPerIteration.keySet()){
                categories[i++] = it.toString();
            }

            StackedBarChart chart = new StackedBarChart("Passenger kilometers traveled per Mode","Iteration","pkm",categories);
            //rotate x-axis by 90degrees
            chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

            for (String mode : allModes){
                double[] value =  pmtPerIteration.values().stream()
                        .mapToDouble(k->k.getOrDefault(mode,0.0)/1000.0)
                        .toArray();
                chart.addSeries(mode, value);
            }
            chart.addMatsimLogo();
            chart.saveAsPng(controlerIO.getOutputFilename(FILENAME + ".png"), 1024, 768);

        }

    }
}

