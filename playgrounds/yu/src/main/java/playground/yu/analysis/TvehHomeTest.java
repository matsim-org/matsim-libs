/* *********************************************************************** *
 * project: org.matsim.*
 * TvehHomeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;


public class TvehHomeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String origTvehFilename = "../runs/run264/T.veh.gz";
		final String homeTvehFilename = "../runs/run264/homeT.veh.gz";
		// final String origTvehFilename = "./test/T.veh.gz";
		// final String homeTvehFilename = "./output/homeT.veh.gz";

		TransimsSnapshotFileReader reader = new TransimsSnapshotFileReader(
				origTvehFilename);
		String[] heads = reader.readLine();
		StringBuffer head = new StringBuffer();
		for (int i = 0; i < heads.length; i++) {
			head.append(heads[i]);
			head.append('\t');
		}
		String s1_8 = "\t0\t0\t0\t1\t0\t0\t1\t0\t";
		String s10 = "\t0\t";
		String s13_15 = "\t0\t0\t0";
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(homeTvehFilename);
			writer.write(head + "\n");
			String[] line = null;
			do {
				line = reader.readLine();
				if (line != null) {
					writer.write(line[0] + s1_8 + line[9] + s10 + line[11]
							+ "\t" + line[12] + s13_15 + "\n");
				}
			} while (line != null);
			writer.close();
			System.out.println("--> Done!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
