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

package playground.johannes.gsv.counts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class UpdateVisumCounts {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		/*
		 * read bast file
		 */
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/counts/bast2013.raw.csv"));

		Map<String, Double> values = new HashMap<>();

		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(";", -1);
			String id = tokens[1];

			if (!tokens[14].isEmpty()) {
				double total = Double.parseDouble(tokens[14]);
				double sv = 0;
				if (!tokens[17].isEmpty())
					sv = Double.parseDouble(tokens[17]);

				values.put(id, total - sv);
			}
		}
		reader.close();
		/*
		 * read visum file
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter("/mnt/vboxsf/B_USER/counts-list2.txt"));
		reader = new BufferedReader(new FileReader("/mnt/vboxsf/B_USER/counts-list.txt"));

		writer.write(reader.readLine());
		writer.write("\r\n");
		writer.write(reader.readLine());
		writer.write("\r\n");
		writer.write(reader.readLine());
		writer.write("\r\n");
		writer.write(reader.readLine());
		writer.write("\r\n");
		writer.write(reader.readLine());
		writer.write("\r\n");

		while ((line = reader.readLine()) != null) {
			if(!line.isEmpty()) {
			String[] tokens = line.split("\t");
			String id = tokens[9];

			Double val = values.get(id);
			if (val == null)
				val = 0.0;

			writer.write(line);
//			writer.write("\t");
			writer.write(String.valueOf(val / 2.0));
//			writer.newLine();
			writer.write("\r\n");
			}
		}
		writer.close();
	}

}
