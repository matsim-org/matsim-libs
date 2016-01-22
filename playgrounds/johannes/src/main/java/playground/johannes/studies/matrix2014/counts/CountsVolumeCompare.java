/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.counts;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

import java.io.IOException;

/**
 * @author johannes
 *
 */
public class CountsVolumeCompare {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String refFile = "/home/johannes/gsv/germany-scenario/counts/counts.2013.net20140909.5.24h.xml";
		String targetFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.net20140909.5.24h.xml";
		String histFile = "/home/johannes/gsv/matrix2014/counts/errors.txt";

		Counts<Link> refCounts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(refCounts);
		reader.parse(refFile);
		
		Counts<Link> targetCounts = new Counts();
		reader = new CountsReaderMatsimV1(targetCounts);
		reader.parse(targetFile);

		TDoubleArrayList errors = new TDoubleArrayList();
		TDoubleArrayList absErrors = new TDoubleArrayList();

		int noMatch = 0;
		for(Count refCount : refCounts.getCounts().values()) {
			Count targetCount = targetCounts.getCount(refCount.getLocId());
			if(targetCount != null) {
				double err = (targetCount.getVolume(1).getValue() - refCount.getVolume(1).getValue()) / refCount.getVolume(1).getValue();
				double absErr = Math.abs(err);

				errors.add(err);
				absErrors.add(absErr);
			} else {
				noMatch++;
			}
		}

		System.out.println(String.format("No matches: %s", noMatch));

		double[] errorValues = errors.toArray();
		System.out.println(String.format("Error: mean = %s, min = %s, max = %s", StatUtils.mean(errorValues),
				StatUtils.min(errorValues), StatUtils.max(errorValues)));
		TDoubleDoubleHashMap hist = Histogram.createHistogram(errorValues, new LinearDiscretizer(0.01), false);
		Histogram.normalize(hist);
		StatsWriter.writeHistogram(hist, "error", "proba", histFile);

		errorValues = absErrors.toArray();
		System.out.println(String.format("Absolute Error: mean = %s, min = %s, max = %s", StatUtils.mean(errorValues),
				StatUtils.min(errorValues), StatUtils.max(errorValues)));
	}

}
