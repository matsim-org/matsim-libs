/* *********************************************************************** *
 * project: org.matsim.*
 * AreaUtilityOffsets.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.math.SimpleStatistics;

public class AreaUtilityOffsets {
	private Coord coord;
	private int zeroUtilOffsetCnt = 0;
	private List<Double> nonzeroUtilOffsets;
	private static double interval = 0.25;

	public AreaUtilityOffsets(Coord coord) {
		this.coord = coord;
		nonzeroUtilOffsets = new ArrayList<Double>();
	}

	public Coord getCoord() {
		return coord;
	}

	public int getZeroUtilOffsetCnt() {
		return zeroUtilOffsetCnt;
	}

	public static double getInterval() {
		return interval;
	}

	public int getNonzeroUtilityOffsetsCnt() {
		return nonzeroUtilOffsets.size();
	}

	public static void setInterval(double interval) {
		AreaUtilityOffsets.interval = interval;
	}

	public void addZeroUtilOffset() {
		zeroUtilOffsetCnt++;
	}

	public double getAverageNonzeroUtilityOffset() {
		return SimpleStatistics.average(nonzeroUtilOffsets);
	}

	public boolean isInOneSigma() {
		double avg = getAverageNonzeroUtilityOffset();
		double sigma = Math.sqrt(SimpleStatistics.variance(nonzeroUtilOffsets));
		return Math.abs(avg) <= sigma;
	}

	public void addNonzeroUtilOffset(double offset) {
		if (offset != 0d) {
			nonzeroUtilOffsets.add(offset);
		} else {
			Logger.getLogger("Nonzero Problem").error(
					"This offset should NOT be zero!!!");
			throw new RuntimeException();
		}
	}

	public void output(String filenameBase) {
		DistributionCreator creator = new DistributionCreator(
				nonzeroUtilOffsets, interval);
		creator.writePercent(filenameBase + "txt");
		creator.createChartPercent(filenameBase + "png",
				"trip utility offset distribution", "trip utility offset",
				"frequency (in percent)");
	}
}
