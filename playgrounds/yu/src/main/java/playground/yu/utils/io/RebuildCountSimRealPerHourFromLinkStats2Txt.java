/* *********************************************************************** *
 * project: org.matsim.*
 * RebuildCountSimRealPerHourFromLinkStats2Txt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils.io;

import java.util.List;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

/**
 * rebuilds data of CountsSimRealPerHour (scatter plots) from LinkStatsfile
 * 
 * @author yu
 * 
 */
public class RebuildCountSimRealPerHourFromLinkStats2Txt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml", countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		// String linkStatsFilename =
		// "../runs-svn/run669/it.500/500.linkstats.txt.gz", outputFilenameBase
		// = "../runs-svn/run669/it.500/500.count-sim.";
		String linkStatsFilename = "../runs-svn/run1301/ITERS/it.1000/1000.linkstats.txt.gz", outputFilenameBase = "../runs-svn/run1301/ITERS/it.1000/1000.count-sim.";
		double countsScaleFactor = 10d, distanceFilter = 30000d;
		String distanceFilterCenterNodeId = "2531";

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimNetworkReader(scenario).readFile(networkFilename);
		Network network = scenario.getNetwork();

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		CalcLinkStats linkStats = new CalcLinkStats(network);
		linkStats.readFile(linkStatsFilename);

		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(
				linkStats, counts, network, countsScaleFactor);
		// if (config.counts().getDistanceFilterCenterNode() != null) {
		cca.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
		// }
		cca.setCountsScaleFactor(countsScaleFactor);
		cca.run();

		List<CountSimComparison> cscsList = cca.getComparison();
		for (int hour = 9; hour <= 19; hour += 2) {
			System.out.println("List<CountSimComparison> size :\t"
					+ cscsList.size());
			SimpleWriter writer = new SimpleWriter(outputFilenameBase + hour
					+ ".txt");
			for (CountSimComparison csc : cscsList) {
				if (csc.getHour() == hour) {
					double cntVal = csc.getCountValue(), simVal = csc
							.getSimulationValue();
					if (cntVal <= 0d || simVal <= 0d) {
						cntVal = Math.max(1d, cntVal);
						simVal = Math.max(1d, simVal);
					}
					writer.writeln(cntVal + " " + simVal);
				}
			}
			writer.close();
		}
	}
}
