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
public class CsvReformatter2 implements VehicleTrajectoryHandler, Closeable {

	private final ZoneIdToIndexMapping mapping;
	private final BufferedWriter writer;

	public CsvReformatter2(final ZoneIdToIndexMapping mapping, String filename) {
		this.mapping = mapping;
		this.writer = IOUtils.getBufferedWriter(filename);
		try {
			//this.writer.write("VehID,OrigZone,DestZone,OrigNode,DestNode,DepTime,TravelTime,JamTime,Nodes,TimeStamps,TravelTimes,JamTimes");
			this.writer.write("VehId,nodeFrom,tDep,nodeTo,tArr,tTT,tStop");
			this.writer.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void handleVehicleTrajectory(VehicleTrajectory trajectory) {
		int[] nodes = trajectory.getTravelledNodes();
		double[] timeStamps=trajectory.getTimeStamps();
		double[] travelTimes=trajectory.getTravelledNodeTimes();
		double[] stopTimes=trajectory.getJamTimes();
		double startTime=trajectory.getStartTime();
		for(int i=0; i<nodes.length; i++){

			try {
				this.writer.write(Integer.toString(trajectory.getVehNr()));
				this.writer.write(",");

				if(i==0){
					this.writer.write(Integer.toString(trajectory.getUpstreamNode()));
					this.writer.write(",");
					this.writer.write(Double.toString(startTime));
					this.writer.write(",");
					this.writer.write(Integer.toString(nodes[i]));
					this.writer.write(",");
					this.writer.write(Double.toString((timeStamps[i]+startTime)));
					this.writer.write(",");
					this.writer.write(Double.toString(travelTimes[i]));
					this.writer.write(",");
					this.writer.write(Double.toString((stopTimes[i])-stopTimes[0]));
				}else{
					this.writer.write(Integer.toString(nodes[i-1]));
					this.writer.write(",");
					this.writer.write(Double.toString((timeStamps[i-1])+startTime));
					this.writer.write(",");
					this.writer.write(Integer.toString(nodes[i]));
					this.writer.write(",");
					this.writer.write(Double.toString((timeStamps[i])+startTime));
					this.writer.write(",");
					this.writer.write(Double.toString(travelTimes[i]));
					this.writer.write(",");
					this.writer.write(Double.toString((stopTimes[i]-stopTimes[i-1])));

				}

				this.writer.newLine();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			
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
		if (args.length!=2){
		    System.out.println("Error- 2 Pfade müssen eingegeben werden. Bitte in der Dokumentation nachschlagen.");
		}
		else {
		    System.out.println("Zone File "+args[0]);
		    System.out.println("Output directory "+args[1]);
		}
		System.out.println("running");
		String zoneMappingFilename=args[0];
		String runDirName=args[1];
		//String zoneMappingFilename="C:/Users/jeremy/SVI Dosierung/KreuzlingenNetzSzen3/Marcel20130507/l41_ZoneNo_TAZ_mapping.csv";
		//String runDirName="C:/Users/jeremy/SVI Dosierung/KreuzlingenNetzSzen3/Marcel20130507/ohneDosierung_it.100/it.100/100.DynusT";
		String vehTrajFilename=runDirName+"/VehTrajectory.dat";
		String csvTrajFilename=runDirName+"/100.NodeSummary.csv";


		execute(zoneMappingFilename,csvTrajFilename,vehTrajFilename);
		System.out.println("done.");
	}
	private static void execute(String zoneMappingFilename, String csvTrajFilename, String vehTrajFilename) throws IOException{

		ZoneIdToIndexMapping zoneMapping = new ZoneIdToIndexMapping();
		new ZoneIdToIndexMappingReader(zoneMapping).readFile(zoneMappingFilename);
		CsvReformatter2 csv = new CsvReformatter2(zoneMapping, csvTrajFilename);
		new VehicleTrajectoriesReader(csv, zoneMapping).readFile(vehTrajFilename);
		csv.close();
	}

}
