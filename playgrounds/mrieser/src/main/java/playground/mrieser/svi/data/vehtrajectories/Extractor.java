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

package playground.mrieser.svi.data.vehtrajectories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author mrieser
 */
public class Extractor {

	private final static Logger log = Logger.getLogger(Extractor.class);
	
	public static void main(String[] args) {
		String inputVehTrajectory;
		double startTimeInMinutes;
		double stopTimeInMinutes;
		String outputVehTrajectory;

		if (args.length == 4) {
			inputVehTrajectory = args[0];
			startTimeInMinutes = Double.parseDouble(args[1]);
			stopTimeInMinutes = Double.parseDouble(args[2]);
			outputVehTrajectory = args[3];
		} else {
			inputVehTrajectory = "/Volumes/Data/virtualbox/exchange/dynusT_output/ITERS/it.2/2.DynusT/VehTrajectory.dat";
			startTimeInMinutes = 6.0 * 60;
			stopTimeInMinutes = 9.0 * 60;
			outputVehTrajectory = "/Volumes/Data/virtualbox/exchange/dynusT_output/ITERS/it.2/2.DynusT/VehTrajectory.filtered.dat";
		}
		
		filterVehTrajectory(inputVehTrajectory, startTimeInMinutes, stopTimeInMinutes, outputVehTrajectory);
	}
	
	public static void filterVehTrajectory(final String inputFile, final double startTimeInMinutes, final double stopTimeInMinutes, final String outputFile) {
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {

			String line = null;

			// first read 5 header lines
			for (int i = 0; i < 5; i++) {
				line = reader.readLine();
				writer.write(line);
				writer.write(IOUtils.NATIVE_NEWLINE);
			}

			ArrayList<String> tmpLines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				if (line.contains("###")) {
					break; // all finished veh trajectories read, finish here
				}
				String[] header = line.replace("=", "= ").split("\\s+"); // make sure there is a space after every =, I've had a file where STime=1000.00 was written...
				if (header.length != 31) {
					log.warn("Line could not be parsed: " + line);
					break;
				}
				
				double sTime = Double.parseDouble(header[18]);
				double travelTime = Double.parseDouble(header[22]);
				int nOfNodes = Integer.parseInt(header[26]);
				tmpLines.add(line);
				
				// read nodes
				int nOfLines = 0;
				{
					int nodesIdx = 0;
					while (nodesIdx < nOfNodes) {
						nOfLines++;
						line = reader.readLine();
						tmpLines.add(line);
						String parts[] = line.split("\\s+");
						for (String part : parts) {
							if (part.length() > 0) {
								nodesIdx++;
							}
						}
					}
				}

				// time stamp of vehicle in system at nodes
				for (int i = 0; i < nOfLines; i++) {
					line = reader.readLine();
					tmpLines.add(line);
				}
				
				// travel time between nodes
				for (int i = 0; i < nOfLines; i++) {
					line = reader.readLine();
					tmpLines.add(line);
				}
				
				// delay times at nodes
				for (int i = 0; i < nOfLines; i++) {
					line = reader.readLine();
					tmpLines.add(line);
				}
				
				// now decide if we use this veh trajectory
				if ((sTime >= startTimeInMinutes) && (sTime < stopTimeInMinutes)) {
					// copy lines to output File
					for (String tmp : tmpLines) {
						writer.write(tmp);
						writer.write(IOUtils.NATIVE_NEWLINE);
					}
				}
				tmpLines.clear();
				
			}
		
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				log.error("Could not close file " + outputFile);
			}
			try {
				reader.close();
			} catch (IOException e) {
				log.error("Could not close file " + inputFile);
			}
		}
	}
}
