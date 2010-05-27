/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.iavis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class IADriveFilter {

	private static double minX = 678000.0;
	private static double maxX = 690000.0;
	private static double minY = 245000.0;
	private static double maxY = 253000.0;


	public static void main(String[] args) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader("/data/vis/zrh25pct/ia/agentdrive.txt.gz");
		BufferedWriter writer = IOUtils.getBufferedWriter("/data/vis/zrh25pct/ia/agentdrive7-8zrh.txt");

		double startLimit = 7.0 * 3600;
		double endLimit = 8.0 * 3600;

		try {
			String header = reader.readLine();
//			writer.write(header + "\n");

			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, '\t');
				double x1 = Double.parseDouble(parts[1]);
				double y1 = Double.parseDouble(parts[2]);
				double x2 = Double.parseDouble(parts[3]);
				double y2 = Double.parseDouble(parts[4]);
				double startTime = Double.parseDouble(parts[5]);
				double endTime = Double.parseDouble(parts[6]);
				if ((startTime >= startLimit && startTime <= endLimit) || (endTime >= startLimit && endTime <= endLimit)) {
					if (inRegion(x1, y1) || inRegion(x2, y2)) {
						writer.write(line + "\n");
					}
				}
			}
		}
		finally {
			reader.close();
			writer.close();
		}
	}

	private static boolean inRegion(double x, double y) {
		if (x < minX) {
			return false;
		}
		if (x > maxX) {
			return false;
		}
		if (y < minY) {
			return false;
		}
		if (y > maxY) {
			return false;
		}
		return true;
	}

}
