/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.data.vehtrajectories;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.mrieser.svi.data.ZoneIdToIndexMapping;

/**
 * @author mrieser
 */
public class VehicleTrajectoriesReader {

	private final static Logger log = Logger.getLogger(VehicleTrajectoriesReader.class);

	private final VehicleTrajectoryHandler handler;
	private final String[] zoneIdxToIdMapping;

	public VehicleTrajectoriesReader(final VehicleTrajectoryHandler handler, final ZoneIdToIndexMapping zoneMapping) {
		this.handler = handler;
		this.zoneIdxToIdMapping = zoneMapping.getIndexToIdMapping();
	}

	public void readFile(final String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		try {
			// first read 5 header lines
			for (int i = 0; i < 5; i++) {
				reader.readLine();
			}
			// now (try to) read one trajectory after the next
			while (true) {
				VehicleTrajectory traj = readTrajectory(reader);
				if (traj == null) {
					return;
				}
				this.handler.handleVehicleTrajectory(traj);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				log.error("Could not close stream for " + filename);
			}
		}
	}

	private VehicleTrajectory readTrajectory(final BufferedReader reader) throws IOException {
		// read header line

		// example line:
		//     "Veh #        4 Tag= 2 OrigZ=   24 DestZ=   21 Class= 3 UstmN=   3161 DownN=   3469 DestN=   3466 STime=   0.00 Total Travel Time=   0.36 # of Nodes=   3 VehType 1 LOO 1"
		// idx = 0  1        2   3  4    5      6   7       8   9    10   11     12   13       14    15       16   17      18   19    20     21      22 23 24  25     26  27    28 29 30
		String line = reader.readLine();
		if (line.contains("###")) {
			return null;
		}
		String[] header = line.replace("=", "= ").split("\\s+"); // make sure there is a space after every =, I've had a file where STime=1000.00 was written...
		if (header.length != 31) {
			log.warn("Line could not be parsed: " + line);
			return null;
		}

		int vehNr = Integer.parseInt(header[2]);
		int tag = Integer.parseInt(header[4]);
		int upstreamNode = Integer.parseInt(header[12]);
		int origZ = Integer.parseInt(header[6]);
		int destZ = Integer.parseInt(header[8]);
		double sTime = Double.parseDouble(header[18]) * 60.0; // convert minutes to seconds
		double travelTime = Double.parseDouble(header[22]) * 60.0;
		int nOfNodes = Integer.parseInt(header[26]);

		// read nodes
		int[] nodes = new int[nOfNodes];

		int nOfLines = 0;
		{
			int nodesIdx = 0;
			while (nodesIdx < nOfNodes) {
				nOfLines++;
				line = reader.readLine();
				String parts[] = line.split("\\s+");
				for (String part : parts) {
					if (part.length() > 0) {
						nodes[nodesIdx] = Integer.parseInt(part);
						nodesIdx++;
					}
				}
			}
		}

		// time stamp of vehicle in system at nodes
		double[] timeStamps = new double[nOfNodes];
		{
			int nodesIdx = 0;
			for (int i = 0; i < nOfLines; i++) {
				line = reader.readLine(); // currently ignore
				
				String parts[] = line.split("\\s+");
				for (String part : parts) {
					if (part.length() > 0) {
						timeStamps[nodesIdx] = Double.parseDouble(part) * 60.0; // convert minutes to seconds
						nodesIdx++;
					}
				}
			}
		}

		// travel time between nodes
		double[] travelTimes = new double[nOfNodes];
		{
			int nodesIdx = 0;
			for (int i = 0; i < nOfLines; i++) {
				line = reader.readLine(); // currently ignore
				
				String parts[] = line.split("\\s+");
				for (String part : parts) {
					if (part.length() > 0) {
						travelTimes[nodesIdx] = Double.parseDouble(part) * 60.0; // convert minutes to seconds
						nodesIdx++;
					}
				}
			}
		}

		// delay times at nodes
		double[] jamTimes = new double[nOfNodes];
		{
			int nodesIdx = 0;
			for (int i = 0; i < nOfLines; i++) {
				line = reader.readLine(); // currently ignore
				
				String parts[] = line.split("\\s+");
				for (String part : parts) {
					if (part.length() > 0) {
						jamTimes[nodesIdx] = Double.parseDouble(part) * 60.0; // convert minutes to seconds
						nodesIdx++;
					}
				}
			}
		}

		VehicleTrajectory traj = new VehicleTrajectory(vehNr, tag, this.zoneIdxToIdMapping[origZ], this.zoneIdxToIdMapping[destZ], sTime, travelTime);
		traj.setTravelledNodes(upstreamNode, nodes);
		traj.setTimeStamps(timeStamps);
		traj.setTravelledNodeTimes(travelTimes);
		traj.setJamTimes(jamTimes);
		return traj;
	}
}
