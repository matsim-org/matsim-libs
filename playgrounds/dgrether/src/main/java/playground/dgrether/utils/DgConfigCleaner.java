package playground.dgrether.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgConfigCleaner
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * @author dgrether
 *
 */
public class DgConfigCleaner {
	
	
	public void cleanAndWriteConfig(String inputConfigFileName, String outputConfigFilename) throws FileNotFoundException, IOException{
		BufferedReader reader = IOUtils.getBufferedReader(inputConfigFileName);
		String line = reader.readLine();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputConfigFilename);
		while (line != null) {
			if (line.contains("bikeSpeedFactor")) {
				line = line.replaceAll("bikeSpeedFactor", "bikeSpeed");
			}
			else if (line.contains("undefinedModeSpeedFactor")) {
				line = line.replaceAll("undefinedModeSpeedFactor", "undefinedModeSpeed");
			}
			else if (line.contains("walkSpeedFactor")) {
				line = line.replaceAll("walkSpeedFactor", "walkSpeed");
			}
			//skip line without replacement
			else if (line.contains("ptScaleFactor") ||
					line.contains("localDTDBase") ||
					line.contains("outputSample") ||
					line.contains("outputVersion") ||
					line.contains("evacuationTime") ||
					line.contains("snapshotfile") ||
					line.contains("offsetWalk") ||
					line.contains("outputTimeFormat")
				) {
				line = reader.readLine();
				continue;
			}
			writer.write(line);
			writer.newLine();
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}

}
