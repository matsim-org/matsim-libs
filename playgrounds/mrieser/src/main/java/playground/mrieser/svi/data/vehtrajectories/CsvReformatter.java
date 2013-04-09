/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;

/**
 * Reformat vehicle trajectories into real tables to be more easily parsed by other tools,
 * written out as CSV. 
 * 
 * @author mrieser
 */
public class CsvReformatter implements VehicleTrajectoryHandler, Closeable {
	
	private final ZoneIdToIndexMapping mapping;
	private final BufferedWriter writer;
	
	public CsvReformatter(final ZoneIdToIndexMapping mapping, String filename) {
		this.mapping = mapping;
		this.writer = IOUtils.getBufferedWriter(filename);
		try {
			this.writer.write("VehID,OrigZone,DestZone,OrigNode,DestNode,DepTime,TravelTime,JamTime,Nodes,TimeStamps,TravelTimes,JamTimes");
			this.writer.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void handleVehicleTrajectory(VehicleTrajectory trajectory) {
		try {
			int[] nodes = trajectory.getTravelledNodes();
			this.writer.write(Integer.toString(trajectory.getVehNr()));
			this.writer.write(",");
			this.writer.write(Integer.toString(findZoneIndex(trajectory.getOrigZone())));
			this.writer.write(",");
			this.writer.write(Integer.toString(findZoneIndex(trajectory.getDestZone())));
			this.writer.write(",");
			this.writer.write(Integer.toString(trajectory.getUpstreamNode()));
			this.writer.write(",");
			this.writer.write(Integer.toString(nodes[nodes.length - 1]));
			this.writer.write(",");
			this.writer.write(Double.toString(trajectory.getStartTime() / 60.0));
			this.writer.write(",");
			this.writer.write(Double.toString(trajectory.getTravelTime() / 60.0));
			this.writer.write(",");
			this.writer.write(Double.toString(trajectory.getJamTimes()[trajectory.getJamTimes().length -1] / 60.0));
			this.writer.write(",");
			this.writer.write(asList(nodes));
			this.writer.write(",");
			this.writer.write(asList(trajectory.getTimeStamps()));
			this.writer.write(",");
			this.writer.write(asList(trajectory.getTravelledNodeTimes()));
			this.writer.write(",");
			this.writer.write(asList(trajectory.getJamTimes()));
		
			this.writer.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public void close() throws IOException {
		this.writer.close();
	}
	
	private String asList(final int[] array) {
		StringBuilder str = new StringBuilder(1000);
		str.append('"');
		boolean isFirst = true;
		for (int i : array) {
			if (!isFirst) {
				str.append(' ');
			}
			isFirst = false;
			str.append(Integer.toString(i));
		}
		str.append('"');
		return str.toString();
	}

	private String asList(final double[] array) {
		StringBuilder str = new StringBuilder(1000);
		str.append('"');
		boolean isFirst = true;
		for (double i : array) {
			if (!isFirst) {
				str.append(' ');
			}
			isFirst = false;
			str.append(Double.toString(Math.round(i * 100) / 100));
		}
		str.append('"');
		return str.toString();
	}

	private final int findZoneIndex(final String zoneId) {
		int idx = 0;
		for (String id : this.mapping.getIndexToIdMapping()) {
			if (id != null && id.equals(zoneId)) {
				return idx;
			}
			idx++;
		}
		return -1;
	}
	
	public static void main(String[] args) throws IOException {
		String zoneMappingFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/l41_ZoneNo_TAZ_mapping.csv";
		String vehTrajFilename = "/Volumes/Data/projects/sviDosierungsanlagen/runs/D34_4/it.100/100.DynusT/VehTrajectory.dat";
		String csvTrajFilename = "/Volumes/Data/projects/sviDosierungsanlagen/runs/D34_4/100.vehTrajectories.csv";
		
		ZoneIdToIndexMapping zoneMapping = new ZoneIdToIndexMapping();
		new ZoneIdToIndexMappingReader(zoneMapping).readFile(zoneMappingFilename);
		
		CsvReformatter csv = new CsvReformatter(zoneMapping, csvTrajFilename);
		new VehicleTrajectoriesReader(csv, zoneMapping).readFile(vehTrajFilename);
		csv.close();
	}

}
