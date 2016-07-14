/* *********************************************************************** *
 * project: org.matsim.*
 * CountsCompareToCSV.java
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

package playground.telaviv.counts;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.telaviv.config.TelAvivConfig;

public class CountsCompareToCSV {

	final private static Logger log = Logger.getLogger(CountsCompareToSHP.class);
	
	private final String separator = ",";
	private final Charset charset = Charset.forName("UTF-8");
	
	public static void main(String[] args) throws Exception {
		new CountsCompareToCSV(TelAvivConfig.basePath + "/output/ITERS/it.100/100.countscompare.txt", 
				TelAvivConfig.basePath + "/output/ITERS/it.100/100.countscompare.csv");
		new CountsCompareToCSV(TelAvivConfig.basePath + "/output_cadyts/ITERS/it.500/500.countscompare.txt", 
				TelAvivConfig.basePath + "/output_cadyts/ITERS/it.500/500.countscompare.csv");
	}
	
	public CountsCompareToCSV(String countsInFile, String countsOutFile) throws IOException {
		
		Map<String, List<Line>> countsMap = readCounts(countsInFile);
			
		Counts<Link> counts = createCounts(countsMap);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(countsOutFile, charset);
		writeHeader(writer);
		for (Count count : counts.getCounts().values()) {
			writeRow(writer, count);
		}
		writer.flush();
		writer.close();
	}
	
	private Map<String, List<Line>> readCounts(String countsInFile) throws IOException {

		BufferedReader reader = IOUtils.getBufferedReader(countsInFile);
		Map<String, List<Line>> counts = new HashMap<String, List<Line>>();
		
		// skip first Line
		reader.readLine();
		
		String textLine;
		while((textLine = reader.readLine()) != null) {
			textLine = textLine.replace(",", "");
			String[] cols = textLine.split("\t");
			
			Line line = new Line();
			line.Link_Id = cols[0];
			line.Hour = cols[1];
			line.MATSIM_Volumes = cols[2];
			line.Count_Volumes = cols[3];
			line.Relative_Error = cols[4];
			
			List<Line> list = counts.get(line.Link_Id);
			if (list == null) {
				list = new ArrayList<Line>();
				counts.put(line.Link_Id, list);
			}
			list.add(line);
		}
		
		reader.close();
		
		log.info("Read " + counts.size() + " counts.");
		
		return counts;
	}
	
	private Counts createCounts(Map<String, List<Line>> countsMap) {
		
		Counts counts = new Counts();
		counts.setName("Tel Aviv Model");
		counts.setDescription("Tel Aviv Model");
		counts.setYear(2012);
		
		for (List<Line> lines : countsMap.values()) {
			Count count = counts.createAndAddCount(Id.create(lines.get(0).Link_Id, Link.class), "");
			
			for (Line line : lines) {
				count.createVolume(Integer.valueOf(line.Hour), Double.valueOf(line.MATSIM_Volumes));
			}
		}
		
		return counts;
	}
	
	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write("loc_id");
		writer.write(separator);
		writer.write("cs_id");
		for (int i = 1; i < 25; i++) {
			writer.write(separator);
			writer.write("hour_" + i);
		}
		writer.newLine();
	}
	
	private void writeRow(BufferedWriter writer, Count count) throws IOException {
		writer.write(count.getLocId().toString());
		writer.write(separator);
		writer.write(count.getCsId());
		for (int i = 1; i < 25; i++) {
			writer.write(separator);
			Volume volume = count.getVolume(i);
			if (volume == null) writer.write("-1.0");
			else writer.write(String.valueOf(volume.getValue()));
		}
		writer.newLine();
	}

	private static class Line {
//		Link Id	Hour	MATSIM volumes	Count volumes	Relative Error	
//		10005	6	71.6	5,521	-98.703
		String Link_Id;
		String Hour;
		String MATSIM_Volumes;
		String Count_Volumes;
		String Relative_Error;
	}
}
