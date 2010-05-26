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

	public static void main(String[] args) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader("/data/vis/zrh25pct/ia/agentdrive.txt.gz");
		BufferedWriter writer = IOUtils.getBufferedWriter("/data/vis/zrh25pct/ia/agentdrive7-8.txt");

		double startLimit = 7.0 * 3600;
		double endLimit = 8.0 * 3600;

		try {
			String header = reader.readLine();
			writer.write(header + "\n");

			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, '\t');
				double startTime = Double.parseDouble(parts[5]);
				double endTime = Double.parseDouble(parts[6]);
				if ((startTime >= startLimit && startTime <= endLimit) || (endTime >= startLimit && endTime <= endLimit)) {
					writer.write(line + "\n");
				}
			}
		}
		finally {
			reader.close();
			writer.close();
		}
	}

}
