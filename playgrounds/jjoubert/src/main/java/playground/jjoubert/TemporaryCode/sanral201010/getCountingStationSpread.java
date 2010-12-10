/* *********************************************************************** *
 * project: org.matsim.*
 * getCountingStationSpread.java
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

package playground.jjoubert.TemporaryCode.sanral201010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.jjoubert.Utilities.MyCountingStationParser;

public class getCountingStationSpread {
	private final static Logger log = Logger.getLogger(getCountingStationSpread.class);
	private String folder;
	private String station;
	private String direction;
	private String abnormalDayFile;
	private List<List<List<Double>>> counts;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		getCountingStationSpread cs = new getCountingStationSpread();
		if(args.length == 4){
			cs.folder = args[0];
			cs.station = args[1];
			cs.direction = args[2];
			cs.abnormalDayFile = args[3];
		} else{
			throw new IllegalArgumentException("Incorrect number of arguments passed. Needs folder and station number.");
		}
		
		MyCountingStationParser csp = new MyCountingStationParser();
		List<GregorianCalendar> abnormal = csp.getAbnormalDays(cs.abnormalDayFile);
		
		String station = (cs.station.split("_")[0] + (cs.direction.equalsIgnoreCase("1") ? "a" : "b"));
		File output = new File("./output/" + station + "/");
		if(!output.exists()){
			boolean b = output.mkdirs();
			if(!b){
				log.warn("could not create the output directory ./output/" + station + "/");
			}
		} else{
			log.warn("Output directory ./output/" + station + "/ exists, and content may be overwritten!");
		}

		File theFolder = new File(cs.folder);
		File theFile = new File(cs.folder + cs.station);
		
		if(!theFolder.exists() || !theFile.exists() || !theFile.canRead()){
			throw new RuntimeException("Problem reading station " + cs.station + " from " + cs.folder);
		} else{
			// Read the file.
			try {
				log.info("Reading file " + cs.station);
				BufferedReader br = IOUtils.getBufferedReader(theFile.getAbsolutePath());
				try{
					int directionOneIndex = 7;
					int directionTwoIndex = 12;
					int index = cs.direction.equalsIgnoreCase("1") ? directionOneIndex : directionTwoIndex;

					String line = br.readLine();
					line = br.readLine();
					while((line = br.readLine()) != null){
						line = line.replaceAll("\"", "");
						String[] entry = line.split(",");

						// Determine the day.
						Integer year = Integer.parseInt(entry[2].split("/")[0]);
						Integer month = Integer.parseInt(entry[2].split("/")[1]) - 1;
						Integer day = Integer.parseInt(entry[2].split("/")[2]);
						
						GregorianCalendar cal = new GregorianCalendar(year, month, day);
						int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
						if(abnormal.contains(cal)){
							dayOfWeek = 8;
						}
						
						// Determine the time of day.
						Integer hour = Integer.parseInt(entry[3].split(":")[0]);
						
						// Determine the duration (and hence if it is a complete record).
						String[] duration = entry[4].split(":");
						Double factor = new Double(1);
						if(duration.length == 2){
							/* 
							 * Determine if a full hour was counted. If not, then
							 * determine a factor to adjust the counts by.
							 */
							Integer durationH = Integer.parseInt(duration[0]);
							Integer durationM = Integer.parseInt(duration[1]);
							if(durationH != 1){
								factor = (double)durationM / 60.0;
							} else{
								factor = 1.0;
							}
							
							/* Now, add the count, but only if there was a
							 * reading, i.e. duration > 0. 
							 */
//							log.info("Index: " + index + "; " + entry.length);
							Double d = Double.parseDouble(entry[index]) / factor;
							cs.counts.get(dayOfWeek-1).get(hour-1).add(d);			
						}
						
						
					}	
				} finally{
					br.close();
					log.info("Done");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Now write the files.
			for(int a = 0; a < 8; a++){
				int day = a + 1;
				for(int b = 0; b < 24; b++){
					int hour = b + 1;
					
					try {
						String filename = station + "_" + day + "_" + hour + ".txt";
						BufferedWriter bw = IOUtils.getBufferedWriter(output.getAbsolutePath() + "/" + filename);
						try{
							bw.write(filename);
							bw.newLine();
							for(Double d : cs.counts.get(a).get(b)){
								bw.write(String.valueOf(d));
								bw.newLine();
							}
						} finally {
							bw.close();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		

		log.info("----------------------------");
		log.info("           Done");
		log.info("============================");
	}
	
	public getCountingStationSpread() {
		counts = new ArrayList<List<List<Double>>>(8);
		for(int a = 0; a < 8; a++){
			List<List<Double>> day = new ArrayList<List<Double>>(24);
			for(int b = 0; b < 24; b++){
				List<Double> hour = new ArrayList<Double>();
				day.add(hour);
			}
			counts.add(day);
		}
	}

}
