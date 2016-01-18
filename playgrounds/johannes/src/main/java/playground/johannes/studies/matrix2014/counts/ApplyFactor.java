/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

/**
 * @author johannes
 * 
 */
public class ApplyFactor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.net20140909.5.xml";
		String outFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.net20140909.5.24h.xml";
		double factor = 1/24.0;

		Counts<Link> counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.parse(inFile);

		Counts<Link> newCounts = new Counts();
		newCounts.setDescription(counts.getDescription());
		newCounts.setName(counts.getName());
		newCounts.setYear(counts.getYear());

		for (Count count : counts.getCounts().values()) {
			if (count.getVolume(1).getValue() != 0) {
				Count newCount = newCounts.createAndAddCount(count.getLocId(), count.getCsId());
				for (int i = 1; i < 25; i++) {
					newCount.createVolume(i, count.getVolume(i).getValue() * factor);
				}
				newCount.setCoord(count.getCoord());
			}
		}

		CountsWriter writer = new CountsWriter(newCounts);
		writer.write(outFile);
	}

}
