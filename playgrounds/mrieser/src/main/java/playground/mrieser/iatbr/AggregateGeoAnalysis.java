/* *********************************************************************** *
 * project: org.matsim.*
 * AggregateGeoAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.iatbr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class AggregateGeoAnalysis {

	private final Map<String, Data> data = new HashMap<String, Data>(6000);

	public void aggregateData(String infileAll, String infileS7, String outfile) throws FileNotFoundException, IOException {
		BufferedReader reader = IOUtils.getBufferedReader(infileAll);
		String line = reader.readLine(); // header
		line = reader.readLine();
		while (line != null) {
			String[] parts = StringUtils.explode(line, '\t');
			if (parts.length == 6) {
				Data d = getData(parts);
				if ("car".equals(parts[3])) {
					d.nOfCarUsers++;
				} else if ("transit".equals(parts[3])) {
					d.nOfTransitUsers++;
				} else if ("walk".equals(parts[3])) {
					d.nOfWalkUsers++;
				} else {
					d.nOfOtherUsers++;
				}
			}
			line = reader.readLine();
		}
		reader.close();

		
		BufferedReader readerS7 = IOUtils.getBufferedReader(infileS7);
		String lineS7 = readerS7.readLine(); // header
		lineS7 = readerS7.readLine();
		while (lineS7 != null) {
			String[] parts = StringUtils.explode(lineS7, '\t');
			if (parts.length == 6) {
				Data d = getData(parts);
				if ("car".equals(parts[3])) {
					d.nOfCarUsersS7++;
				} else if ("transit".equals(parts[3])) {
					d.nOfTransitUsersS7++;
				} else if ("walk".equals(parts[3])) {
					d.nOfWalkUsersS7++;
				} else {
					d.nOfOtherUsersS7++;
				}
			}
			lineS7 = readerS7.readLine();
		}
		readerS7.close();

		BufferedWriter writer = IOUtils.getBufferedWriter(outfile);
		writer.write("X\tY\tGID\tGNAME\t" +
				"CAR\tPT\tWALK\tOTHER\t" +
				"CAR_PCT\tPT_PCT\tWALK_PCT\tOTHER_PCT\t" +
				"S7_CAR\tS7_PT\tS7_WALK\tS7_OTHER\t" +
				"S7_CAR_PCT\tS7_PT_PCT\tS7_WALK_PCT\tS7_OTHER_PCT\t" +
				"ABSDIFF_CAR_PCT\tABSDIFF_PT_PCT\tABSDIFF_WALK_PCT\tABSDIFF_OTHER_PCT\t" +
				"RELDIFF_CAR_PCT\tRELDIFF_PT_PCT\tRELDIFF_WALK_PCT\tRELDIFF_OTHER_PCT" +
				"\n");
		for (Data d : this.data.values()) {
			writer.write("600000\t200000\t" + d.gid + "\t" + d.gname + "\t");
			writer.write(d.nOfCarUsers + "\t" + d.nOfTransitUsers + "\t" + d.nOfWalkUsers + "\t" + d.nOfOtherUsers + "\t");
			double allUsers = d.nOfCarUsers + d.nOfTransitUsers + d.nOfWalkUsers + d.nOfOtherUsers;
			double pctCarUsers = d.nOfCarUsers / allUsers;
			double pctTransitUsers = d.nOfTransitUsers / allUsers;
			double pctWalkUsers = d.nOfWalkUsers / allUsers;
			double pctOtherUsers = d.nOfOtherUsers / allUsers;
			writer.write(pctCarUsers + "\t" + pctTransitUsers + "\t" + pctWalkUsers + "\t" + pctOtherUsers + "\t");
			
			writer.write(d.nOfCarUsersS7 + "\t" + d.nOfTransitUsersS7 + "\t" + d.nOfWalkUsersS7 + "\t" + d.nOfOtherUsersS7 + "\t");
			double allUsersS7 = d.nOfCarUsersS7 + d.nOfTransitUsersS7 + d.nOfWalkUsersS7 + d.nOfOtherUsersS7;
			double pctCarUsersS7 = d.nOfCarUsersS7 / allUsersS7;
			double pctTransitUsersS7 = d.nOfTransitUsersS7 / allUsersS7;
			double pctWalkUsersS7 = d.nOfWalkUsersS7 / allUsersS7;
			double pctOtherUsersS7 = d.nOfOtherUsersS7 / allUsersS7;
			writer.write(pctCarUsersS7 + "\t" + pctTransitUsersS7 + "\t" + pctWalkUsersS7 + "\t" + pctOtherUsersS7 + "\t");
	
			double absDiffCar = pctCarUsersS7 - pctCarUsers;
			double absDiffTransit = pctTransitUsersS7 - pctTransitUsers;
			double absDiffWalk = pctWalkUsersS7 - pctWalkUsers;
			double absDiffOther = pctOtherUsersS7 - pctOtherUsers;
			
			writer.write(absDiffCar + "\t" + absDiffTransit + "\t" + absDiffWalk + "\t" + absDiffOther + "\t");
			writer.write((absDiffCar / pctCarUsers) + "\t" + (absDiffTransit / pctTransitUsers) + "\t" + (absDiffWalk / pctWalkUsers) + "\t" + (absDiffOther / pctOtherUsers) + "\n");
		}
		writer.close();
	}
	
	private Data getData(String[] parts) {
		Data d = this.data.get(parts[4]);
		if (d == null) {
			d = new Data();
			d.gid = parts[4];
			d.gname = parts[5];
			this.data.put(parts[4], d);
		}
		return d;
	}

	/*package*/ static class Data {
		/*package*/ String gid;
		/*package*/ String gname;

		/*package*/ int nOfCarUsers = 0;
		/*package*/ int nOfTransitUsers = 0;
		/*package*/ int nOfWalkUsers = 0;
		/*package*/ int nOfOtherUsers = 0;

		/*package*/ int nOfCarUsersS7 = 0;
		/*package*/ int nOfTransitUsersS7 = 0;
		/*package*/ int nOfWalkUsersS7 = 0;
		/*package*/ int nOfOtherUsersS7 = 0;
	}
	
	public static void main(String[] args) {
		try {
			new AggregateGeoAnalysis().aggregateData(
					"/Volumes/Data/VSP/projects/diss/runs/tr100pct1/coordsGmde.txt",
					"/Volumes/Data/VSP/projects/diss/runs/tr100pct1S7/coordsGmde.txt",
					"/Volumes/Data/VSP/projects/diss/runs/tr100compare.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
