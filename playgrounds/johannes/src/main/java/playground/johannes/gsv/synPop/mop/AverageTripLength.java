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

package playground.johannes.gsv.synPop.mop;

import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.WSMStatsFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class AverageTripLength {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		DescriptivePiStatistics stats = new WSMStatsFactory().newInstance();

		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/matrices/raw/mop/2012-13/TXT-Daten/W12.TXT"));

		String line = reader.readLine();

		while ((line = reader.readLine()) != null) {
			String tokens[] = line.split("\\s+");
			double dist = Double.parseDouble(tokens[19]);
			double w = Double.parseDouble(tokens[22]);

			if (dist > 100) {
				stats.addValue(dist, 1 / w);
			}
		}
		reader.close();
		
		System.out.println(String.format("Average distance > 100 KM: %s", stats.getMean()));

	}

}
