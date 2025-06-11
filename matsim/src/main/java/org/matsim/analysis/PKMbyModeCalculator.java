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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.StackedBarChart;

/**
 * analyses passenger kilometer traveled based on experienced plans.
 * @author jbischoff
 */
public class PKMbyModeCalculator {

    private final Map<Integer,Map<String,Double>> pmtPerIteration = new TreeMap<>();
    private final OutputDirectoryHierarchy controllerIO;
		private final String delimiter;
    private final static String FILENAME = "pkm_modestats";

    @Inject
    PKMbyModeCalculator(OutputDirectoryHierarchy controllerIO, GlobalConfigGroup globalConfig) {
        this.controllerIO = controllerIO;
        this.delimiter = globalConfig.getDefaultDelimiter();
    }

    void addIteration(int iteration, IdMap<Person, Plan> map) {
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
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, Double::sum));
        pmtPerIteration.put(iteration,pmtbyMode);
    }

    void writeOutput(boolean writePng) {
        writeCsv();
		if (writePng){
			new Thread(this::writePng).start();
		}
    }

    private void writeCsv() {
		TreeSet<String> allModes = getAllModes();

		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(controllerIO.getOutputFilename(FILENAME + ".csv")));
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create().setDelimiter((this.delimiter.charAt(0))).build());
            writeHeader(csvPrinter, allModes);
			writeValues(csvPrinter, allModes);
			csvPrinter.close();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Could not write PKM Modestats.");
        }
    }

	private void writeHeader(CSVPrinter csvPrinter, TreeSet<String> allModes) throws IOException {
		csvPrinter.print("Iteration");
		csvPrinter.printRecord(allModes);
	}

	private void writeValues(CSVPrinter csvPrinter, TreeSet<String> allModes) throws IOException {
		for (Map.Entry<Integer,Map<String,Double>> e : pmtPerIteration.entrySet()){
			csvPrinter.print(e.getKey());
			for (String mode : allModes){
				csvPrinter.print((int) Math.round(e.getValue().getOrDefault(mode, 0.0) / 1000.0));
			}
			csvPrinter.println();
		}
	}

	private void writePng() {
		String[] categories = new String[pmtPerIteration.size()];
		int i = 0;
		for (Integer it : pmtPerIteration.keySet()){
			categories[i++] = it.toString();
		}

		StackedBarChart chart = new StackedBarChart("Passenger kilometers traveled per Mode","Iteration","pkm",categories);
		//rotate x-axis by 90degrees
		chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

		for (String mode : getAllModes()){
			double[] value =  pmtPerIteration.values().stream()
					.mapToDouble(k->k.getOrDefault(mode,0.0)/1000.0)
					.toArray();
			chart.addSeries(mode, value);
		}
		chart.addMatsimLogo();

		synchronized (controllerIO) {
			chart.saveAsPng(controllerIO.getOutputFilename(FILENAME+ ".png"), 1024, 768);
		}
	}

	private TreeSet<String> getAllModes() {
		return this.pmtPerIteration.values()
                                   .stream()
                                   .flatMap(i -> i.keySet().stream())
                                   .collect(Collectors.toCollection(TreeSet::new));
	}
}

