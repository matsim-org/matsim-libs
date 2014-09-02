/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.SimpleWriter;

/**
 * Calculates CountVarianceQuetient and outputs scatter plot with counts on the
 * x-axis and variance on the y-axis
 * 
 * @author C
 * 
 */
public class CountVarianceQuotient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String countsFilename = "test/input/bln2pct/SB.2000.counts.xml"//
		, varianceFilename = "test/input/bln2pct/SB.simpleVariance.log"//
		, networkFilename = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz"//
		, outputFilenameBase = "test/output/bln2pct/SBcountsVSvariance.";

		// reads network
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		Network network = scenario.getNetwork();

		// reads counts
		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);
		// read variances of counts
		SampleVarianceReader varReader = new SampleVarianceReader(
				varianceFilename);
		varReader.read();
		Map<String, Map<Integer, Double>> countsSampleVariances = varReader
				.getCountsSampleVariances();

		// initialize writer
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "log");
		writer.writeln("countId\ttimeStep (H)\tcount [veh/h]\tvariance [(veh/h)^2]\tquotient (count/variance)\tlink flow Capacity [veh/h]");
		writer.flush();

		List<Double> countList = new LinkedList<Double>()//
		, countVarList = new LinkedList<Double>();

		TreeMap<Id<Link>, Count> countsMap = counts.getCounts();
		for (Id countId : countsMap.keySet()) {
			Map<Integer, Volume> dayCounts = countsMap.get(countId)
					.getVolumes();
			Map<Integer, Double> dayCountVars = countsSampleVariances
					.get(countId.toString());

			for (Integer hour : dayCounts.keySet()) {
				double count = dayCounts.get(hour).getValue();
				double variance = dayCountVars.get(hour);

				double quotient = variance != 0 ? count / variance : 0;

				countList.add(count);
				countVarList.add(variance);

				Link link = network.getLinks().get(countId);
				writer.writeln(countId.toString() + "\t" + hour + "\t" + count
						+ "\t" + variance + "\t" + quotient + "\t"
						+ link.getCapacity() / network.getCapacityPeriod()
						* 3600d);
			}
		}

		writer.close();

		double[] countsArray = Collection2Array.toArrayFromDouble(countList)//
		, countsVarArray = Collection2Array.toArrayFromDouble(countVarList);

		XYScatterChart chart = new XYScatterChart("Counts <> sample variance",
				"Counts", "Sample variance");
		chart.addMatsimLogo();
		chart.addSeries("", countsArray, countsVarArray);
		chart.saveAsPng(outputFilenameBase + "png", 1024, 768);

		System.out.println("Done!");
	}

}
