/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package playground.yu.counts;

import java.util.Collection;
import java.util.Map;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsFromLinkStats {
	public static void main(String[] args) {
		String originalCountsFilename, networkFilename, linkStatsFilename, newCountsFilename;
		double volScaleFactor;
		if (args.length != 5) {
			originalCountsFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml";
			networkFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz";
			linkStatsFilename = "test/input/bln2pct/SB.2000.linkstats.txt.gz";
			newCountsFilename = "test/output/bln2pct/SB.2000.counts.xml";
			volScaleFactor = 50;
		} else {
			originalCountsFilename = args[0];
			networkFilename = args[1];
			linkStatsFilename = args[2];
			newCountsFilename = args[3];
			volScaleFactor = Double.parseDouble(args[4]);
		}
		// ////////////////////////////////////////////////
		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(originalCountsFilename);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		CalcLinkStats linkStats = new CalcLinkStats(scenario.getNetwork(),
				volScaleFactor);
		linkStats.readFile(linkStatsFilename);

		CountsFromLinkStats cfls = new CountsFromLinkStats(counts, linkStats);
		cfls.writeNewCounts(newCountsFilename);
	}

	private final Counts counts;

	private final CalcLinkStats linkStats;

	public CountsFromLinkStats(Counts counts, CalcLinkStats linkStats) {
		this.counts = counts;
		this.linkStats = linkStats;
		extractMeasuresFromLinkStatsToCounts();
	}

	private void extractMeasuresFromLinkStatsToCounts() {

		// parser counts
		Collection<Count> countCollec = counts.getCounts().values();
		for (Count count : countCollec) {
			// get Volumes from linkStats
			double[] linkStatsVols = linkStats.getAvgLinkVolumes(count
					.getLocId());
			System.out.println("---->There are\t" + linkStatsVols.length
					+ "\ttime steps.");
			// set volumes to counts
			Map<Integer, Volume> countVols = count.getVolumes();
			for (Integer timeIdx : countVols.keySet()) {
				countVols.get(timeIdx).setValue(linkStatsVols[timeIdx - 1]);
			}
		}

	}

	public void writeNewCounts(String newCountsFilename) {
		new CountsWriter(counts).write(newCountsFilename);
	}
}
