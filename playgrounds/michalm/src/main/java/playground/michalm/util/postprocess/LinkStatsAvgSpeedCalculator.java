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

package playground.michalm.util.postprocess;

import java.io.*;
import java.util.List;

import playground.michalm.util.postprocess.LinkStatsReader.LinkStats;

public class LinkStatsAvgSpeedCalculator {
	public static void main(String[] args) {
		String dir = "d:\\PP-rad\\poznan\\";
		String linkStatsFile = dir + "40.linkstats-filtered.txt.gz";
		String avgSpeedFile = dir + "40.avg_speed-filtered.txt";

		List<? extends LinkStats> lsl = LinkStatsReader.readLinkStats(linkStatsFile);

		double[] hrsLengthSum = new double[24];
		double[] hrsTTSum = new double[24];

		for (LinkStats ls : lsl) {
			for (int i = 0; i < 24; i++) {
				hrsLengthSum[i] += ls.hrs[i] * ls.length;
				hrsTTSum[i] += ls.hrs[i] * ls.tt[i];
			}
		}

		try (PrintWriter pw = new PrintWriter(new File(avgSpeedFile))) {
			for (int i = 0; i < 24; i++) {
				double avgSpeed = 3.6 * hrsLengthSum[i] / hrsTTSum[i];
				pw.println(i + "\t" + avgSpeed);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
