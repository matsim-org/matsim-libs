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

package playground.michalm.audiAV.flowPaper;

import java.util.Arrays;

import org.matsim.contrib.taxi.util.stats.TaxiStatsReader;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.utils.io.IOUtils;

public class TaxiStatsExtractor {
	public static final String[] FLEETS = { "02.2", "04.4", "06.6", "08.8", "11.0" };
	public static final String[] AVS = { "1.0", "1.5", "2.0" };
	public static final int COUNT = FLEETS.length * AVS.length;

	public static String getId(String fleet, String av) {
		return fleet + "k_AV" + av;
	}

	public static void main(String[] args) {
		String path = "../../../shared-svn/projects/audi_av/papers/03_transport_special_issue/results_0.15fc/";
		int hours = 25;
		int iter = 50;

		String[] header = new String[COUNT + 1];
		String[][] meanWaitTimes = new String[hours + 1][COUNT + 1];
		String[][] p95WaitTimes = new String[hours + 1][COUNT + 1];
		String[][] fleetEmptyRatios = new String[hours + 1][COUNT + 1];
		String[][] fleetWaitRatios = new String[hours + 1][COUNT + 1];

		header[0] = "hour";
		for (int h = 0; h < hours; h++) {
			meanWaitTimes[h][0] = h + "";
			p95WaitTimes[h][0] = h + "";
			fleetEmptyRatios[h][0] = h + "";
			fleetWaitRatios[h][0] = h + "";
		}
		meanWaitTimes[hours][0] = "daily";
		p95WaitTimes[hours][0] = "daily";
		fleetEmptyRatios[hours][0] = "daily";
		fleetWaitRatios[hours][0] = "daily";

		int i = 1;
		for (String fleet : FLEETS) {
			for (String av : AVS) {
				String file = path + getId(fleet, av) + "." + iter + ".hourly_stats_new_stats.txt";
				TaxiStatsReader r = new TaxiStatsReader(file);

				header[i] = fleet + "_" + av;
				for (int h = 0; h <= hours; h++) {
					meanWaitTimes[h][i] = String.format("%.1f", r.getMeanWaitTime(h));
					p95WaitTimes[h][i] = String.format("%.0f", r.getP95WaitTime(h));
					fleetEmptyRatios[h][i] = String.format("%.4f", r.getFleetEmptyDriveRatio(h));
					fleetWaitRatios[h][i] = String.format("%.4f", r.getFleetWaitRatio(h));
				}

				i++;
			}
		}

		try (CompactCSVWriter writer = new CompactCSVWriter(
				IOUtils.getBufferedWriter(path + "hourly_stats_combined.txt"))) {
			writer.writeNext("Mean Passenger Wait Time [s]");
			writer.writeNext(header);
			writer.writeAll(Arrays.asList(meanWaitTimes));
			writer.writeNextEmpty();

			writer.writeNext("95%ile Passenger Wait Time [s]");
			writer.writeNext(header);
			writer.writeAll(Arrays.asList(p95WaitTimes));
			writer.writeNextEmpty();

			writer.writeNext("Vehicle Empty Drive Ratio");
			writer.writeNext(header);
			writer.writeAll(Arrays.asList(fleetEmptyRatios));
			writer.writeNextEmpty();

			writer.writeNext("Vehicle Wait Ratio");
			writer.writeNext(header);
			writer.writeAll(Arrays.asList(fleetWaitRatios));
			writer.writeNextEmpty();
		}
	}
}
