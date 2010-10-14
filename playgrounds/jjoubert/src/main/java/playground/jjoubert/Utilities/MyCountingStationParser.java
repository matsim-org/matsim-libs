/* *********************************************************************** *
 * project: org.matsim.*
 * MyCountingStationParser.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.jjoubert.Utilities.FileSampler.MyFileFilter;

public class MyCountingStationParser {
	private final Logger log;
	private static String root;
	private static String year;
	private static String extension;
	private static String abnormal;
	private List<GregorianCalendar> abnormalList;
	private Map<Id,Counts> countsMap;

	/**
	 * Creates and runs an instance of the traffic counts parser. 
	 * @param args in the following sequence:
	 * <ol>
	 * <li> <b>root</b>, a {@link String} indicating the absolute path of the 
	 * 		root directory where traffic count raw data files reside;
	 * <li> <b>year</b>, a {@link String} indicating the (single) year for 
	 * 		which counts should be parsed, for example "2007";
	 * <li> <b>extension</b>, a {@link String} indicating the file extension of
	 * 		the raw data files to be used. This is used to limit the files in
	 * 		the source folder using the {@link MyFileFilter};
	 * <li> <b>abnormal</b>, a {@link String} indicating the absolute path of
	 * 		the file that contains dates that should be considered <i>abnormal</i>
	 * 		when parsing the raw traffic count data. Traffic counts on these 
	 * 		dates are associated not with the logical {@link Calendar#DAY_OF_WEEK},
	 * 		but rather day `8', considered a seprate type of day, and resulting
	 * 		in separate {@link Counts} objects.   
	 */
	public static void main(String[] args) {
		if(args.length == 3){
			root = args[0];
			year = args[1];
			extension = args[2];
			abnormal = null;
		} else if(args.length == 4){
			root = args[0];
			year = args[1];
			extension = args[2];
			abnormal = args[3];
		} else{
			throw new RuntimeException("Incorrect number of arguments.");
		}
		
		MyCountingStationParser cp = new MyCountingStationParser();
		cp.log.info("========================================================================================");
		cp.log.info(" Parsing counting station data.");
		cp.log.info("----------------------------------------------------------------------------------------");
		cp.log.info("             Root: " + root);
		cp.log.info("             Year: " + year);
		cp.log.info("   File extension: " + extension);
		cp.log.info("    Abnormal days: " + abnormal);
		cp.log.info("========================================================================================");
		
		if(abnormal != null){
			cp.getAbnormalDays(abnormal);
		}

		String folder = root + year + "/Raw/";
		File[] files = null;
		MyFileFilter mff = new MyFileFilter(extension);
		if((new File(folder)).isDirectory()){
			files = (new File(folder)).listFiles(mff);
		}
		
		cp.log.info("Processing files (" + files.length + ") from " + folder);
		int counter = 0;
		int multiplier = 1;
		for (File f : files) {
			cp.parse(f);
			
			// Report progress.
			if(++counter == multiplier){
				cp.log.info("   files complete... " + counter);
				multiplier *= 2;
			}
		}
		cp.log.info("   files complete... " + counter + " (Done)");
		
		cp.writeCountsXml(root + year + "/Xml/");
		
		cp.log.info("----------------------------");
		cp.log.info("         Completed");
		cp.log.info("============================");
	}
	

	/**
	 * Constructor.
	 */
	public MyCountingStationParser() {
		log = Logger.getLogger(MyCountingStationParser.class);
		countsMap = new HashMap<Id, Counts>();
		abnormalList = new ArrayList<GregorianCalendar>();
	}

	/**
	 * Methods reads the records from the provided file, parses the station id,
	 * and hour of day. The counts are then adjusted based on the duration (if 
	 * not a full hour count). Separate counts are kept for different directions 
	 * if a counting station reports on both directions. Heavy vehicles counts 
	 * are summed (short, medium and long).
	 *  
	 * @param f the <code>File</code> to be parsed. Should be of type csv, one
	 * record per line, and should have the following file structure and field
	 * sequence:<br>
	 * <ol>
	 * <li> Station_id; 
	 * <li> Day (actually not used. Day of week is inferred from date), 
	 * <li> Date in the format <code>YYYY/MM/DD</code>; 
	 * <li> Time in the format <code>HH:MM</code>, for example <code>01:00</code> 
	 * 		refers to counts taken from 01:00 - 01:59;
	 * <li> Duration in the format <code>HH:MM</code>, for example <code>0:45</code> 
	 * 		indicates that only 45 minutes of the hour was counted;
	 * <li> Direction_1 description;
	 * <li> Direction_2,description;
	 * <li> Total number of vehicles counted in direction 1;
	 * <li> Total number of class 1 vehicles (light vehicles) counted in 
	 * 		direction 1;
	 * <li> Total number of class 2 vehicles (heavy vehicles - short) counted in 
	 * 		direction 1;
	 * <li> Total number of class 3 vehicles (heavy vehicles - medium) counted 
	 * 		in direction 1;
	 * <li> Total number of class 4 vehicles (heavy vehicles - long) counted in 
	 * 		direction 1;
	 * <li> Total number of vehicles counted in direction 2;
	 * <li> Total number of class 1 vehicles (light vehicles) counted in 
	 * 		direction 2;
	 * <li> Total number of class 2 vehicles (heavy vehicles - short) counted in 
	 * 		direction 2;
	 * <li> Total number of class 3 vehicles (heavy vehicles - medium) counted in 
	 * 		direction 2;
	 * <li> Total number of class 4 vehicles (heavy vehicles - long) counted in 
	 * 		direction 2;
	 * <br><br>The counting station data provided my Mikros Traffic Management 
	 * 		(SANRAL Project, South Africa) include the following (unused in this 
	 * 		class) fields:<br><br>
	 * <li> Total number of vehicles counted in both directions;
	 * <li> Total number of class 1 vehicles (light vehicles) counted in both 
	 * 		directions;
	 * <li> Total number of class 2 vehicles (heavy vehicles - short) counted 
	 * 		in both directions;
	 * <li> Total number of class 3 vehicles (heavy vehicles - medium) counted 
	 * 		in both directions;
	 * <li> Total number of class 4 vehicles (heavy vehicles - long) counted in 
	 * 		both directions;
	 * </ol>
	 */
	private void parse(File f) {
		int counter = 0;
		try {
			BufferedReader br = IOUtils.getBufferedReader(f.getAbsolutePath());
			try{
				String stationId = f.getName().split("_")[0];
				
				Map<Id, Map<Id,List<Integer>>> cMap = new HashMap<Id, Map<Id,List<Integer>>>();
								
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
					if(abnormalList.contains(cal)){
						dayOfWeek = 8;
					}
					
					// Determine the time of day.
					Integer hour = Integer.parseInt(entry[3].split(":")[0]);
					if(hour == 24){
						hour = 0;
					}
					
					// Determine the duration (and hence if it is a complete record).
					String[] duration = entry[4].split(":");
					Double factor;
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
						
						if(entry.length == 22){
							// Create new counts instances if it doesn't already exist.
							// For light vehicles.
							String countsNameLightA = "Day_" + dayOfWeek + "_" + stationId + "_Light_a";
							Id countsIdLightA = new IdImpl(countsNameLightA);
							if(!cMap.containsKey(countsIdLightA)){
								cMap.put(countsIdLightA, buildCountsMap());
							}
							// For heavy vehicles.
							String countsNameHeavyA = "Day_" + dayOfWeek + "_" + stationId + "_Heavy_a";
							Id countsIdHeavyA = new IdImpl(countsNameHeavyA);
							if(!cMap.containsKey(countsIdHeavyA)){
								cMap.put(countsIdHeavyA, buildCountsMap());
							}
							// For the combination of light and heavy vehicles.
							String countsNameTotalA = "Day_" + dayOfWeek + "_" + stationId + "_Total_a";
							Id countsIdTotalA = new IdImpl(countsNameTotalA);
							if(!cMap.containsKey(countsIdTotalA)){
								cMap.put(countsIdTotalA, buildCountsMap());
							}
							
							int lvA = Integer.parseInt(entry[8]);
							lvA = (int) Math.round(((double)lvA ) / factor); // Account for non-full hours.

							int hvA = Integer.parseInt(entry[9]);
							hvA += Integer.parseInt(entry[10]);
							hvA += Integer.parseInt(entry[11]);
							hvA = (int) Math.round(((double)hvA ) / factor); // Account for non-full hours.

							int tA = lvA + hvA;
							
//							int wasLightA = countsMap.get(countsIdLightA).get(new IdImpl(hour)).get(0);
//							System.out.println("Counter: " + ++counter);
							if(counter == 24){
								log.warn("WARNING");
							}
							cMap.get(countsIdLightA).get(new IdImpl(hour)).set(0, cMap.get(countsIdLightA).get(new IdImpl(hour)).get(0) + lvA);
							cMap.get(countsIdLightA).get(new IdImpl(hour)).set(1, cMap.get(countsIdLightA).get(new IdImpl(hour)).get(1) + 1);
							
//							int wasHeavyA = countsMap.get(countsIdHeavyA).get(new IdImpl(hour)).get(0);
							cMap.get(countsIdHeavyA).get(new IdImpl(hour)).set(0, cMap.get(countsIdHeavyA).get(new IdImpl(hour)).get(0) + hvA);
							cMap.get(countsIdHeavyA).get(new IdImpl(hour)).set(1, cMap.get(countsIdHeavyA).get(new IdImpl(hour)).get(1) + 1);
							
							cMap.get(countsIdTotalA).get(new IdImpl(hour)).set(0, cMap.get(countsIdTotalA).get(new IdImpl(hour)).get(0) + tA);
							cMap.get(countsIdTotalA).get(new IdImpl(hour)).set(1, cMap.get(countsIdTotalA).get(new IdImpl(hour)).get(1) + 1);

							/*
							 * The following is just code to verify that counts 
							 * are accumulated correctly. 
							 */
//							if(countsNameLightA.startsWith("Day_2_2520_") && hour==1){
//								System.out.println("Station " + stationId + "; day " + dayOfWeek + "; direction A at time " + hour + ": Light: " + lvA + "; Heavy: " + hvA);
//								System.out.println("   light was " + wasLightA + ", now " + countsMap.get(countsIdLightA).get(new IdImpl(hour)).get(0) + " (counted: " + countsMap.get(countsIdLightA).get(new IdImpl(hour)).get(1) + ")");
//								System.out.println("   heavy was " + wasHeavyA + ", now " + countsMap.get(countsIdHeavyA).get(new IdImpl(hour)).get(0) + " (counted: " + countsMap.get(countsIdHeavyA).get(new IdImpl(hour)).get(1) + ")");
//							}
							
							
							// Check if bi-directional.
							if(!entry[6].isEmpty()){
								/*
								 * Create a separate counting station (if it 
								 * doesn't already exist.
								 */
								// For light vehicles.
								String countsNameLightB = "Day_" + dayOfWeek + "_" + stationId + "_Light_b";
								Id countsIdLightB = new IdImpl(countsNameLightB);
								if(!cMap.containsKey(countsIdLightB)){
									cMap.put(countsIdLightB, buildCountsMap());
								}
								// For heavy vehicles.
								String countsNameHeavyB = "Day_" + dayOfWeek + "_" + stationId + "_Heavy_b";
								Id countsIdHeavyB = new IdImpl(countsNameHeavyB);
								if(!cMap.containsKey(countsIdHeavyB)){
									cMap.put(countsIdHeavyB, buildCountsMap());
								}
								// For the combination of light and heavy vehicles.
								String countsNameTotalB = "Day_" + dayOfWeek + "_" + stationId + "_Total_b";
								Id countsIdTotalB = new IdImpl(countsNameTotalB);
								if(!cMap.containsKey(countsIdTotalB)){
									cMap.put(countsIdTotalB, buildCountsMap());
								}
								
								int lvB = Integer.parseInt(entry[13]);
								lvB = (int) Math.round(((double)lvB ) / factor); // Account for non-full hours.
								
								int hvB = Integer.parseInt(entry[14]);
								hvB += Integer.parseInt(entry[15]);
								hvB += Integer.parseInt(entry[16]);
								hvB = (int) Math.round(((double) hvB) / factor); // Account for non-full hours.
								
								int tB = lvB + hvB;
								
//								int wasLightB = countsMap.get(countsIdLightB).get(new IdImpl(hour)).get(0);
								cMap.get(countsIdLightB).get(new IdImpl(hour)).set(0, cMap.get(countsIdLightB).get(new IdImpl(hour)).get(0) + lvB);
								cMap.get(countsIdLightB).get(new IdImpl(hour)).set(1, cMap.get(countsIdLightB).get(new IdImpl(hour)).get(1) + 1);
								
//								int wasHeavyB = countsMap.get(countsIdHeavyB).get(new IdImpl(hour)).get(0);
								cMap.get(countsIdHeavyB).get(new IdImpl(hour)).set(0, cMap.get(countsIdHeavyB).get(new IdImpl(hour)).get(0) + hvB);
								cMap.get(countsIdHeavyB).get(new IdImpl(hour)).set(1, cMap.get(countsIdHeavyB).get(new IdImpl(hour)).get(1) + 1);
								
								cMap.get(countsIdTotalB).get(new IdImpl(hour)).set(0, cMap.get(countsIdTotalB).get(new IdImpl(hour)).get(0) + tB);
								cMap.get(countsIdTotalB).get(new IdImpl(hour)).set(1, cMap.get(countsIdTotalB).get(new IdImpl(hour)).get(1) + 1);

								/*
								 * The following is just code to verify that counts 
								 * are accumulated correctly. 
								 */
//								if(countsNameLightB.startsWith("Day_2_340_") && hour==1){
//									System.out.println("Station " + stationId + "; day " + dayOfWeek + "; direction B at time " + hour + ": Light: " + lvB + "; Heavy: " + hvB);
//									System.out.println("   light was " + wasLightB + ", now " + countsMap.get(countsIdLightB).get(new IdImpl(hour)).get(0) + " (counted: " + countsMap.get(countsIdLightB).get(new IdImpl(hour)).get(1) + ")");
//									System.out.println("   heavy was " + wasHeavyB + ", now " + countsMap.get(countsIdHeavyB).get(new IdImpl(hour)).get(0) + " (counted: " + countsMap.get(countsIdHeavyB).get(new IdImpl(hour)).get(1) + ")");
//								}
								
							}
//						log.info("2-direction full");
						} else{
							log.warn("Irregular length (" + entry.length + "): " + entry.toString());
						}
						
					} else{
						// It is an empty record.
					}
					
					/*
					 * Alright, we (should) have a complete counts map. Now we 
					 * need to create Counts and Count objects from the map.
					 */
					for (Id cId : cMap.keySet()) {
						String [] sa = cId.toString().split("_");
						String dayName = getDayDescription(Integer.parseInt(sa[1]));
						String stationName = sa[2];
						String typeName = sa[3];
						String directionName = sa[4];
						String name = dayName + "_" + typeName;
						
						if(!countsMap.containsKey(new IdImpl(name))){
							Counts cs = new Counts();
							cs.setName(name);
							cs.setYear(Integer.parseInt(MyCountingStationParser.year));
							countsMap.put(new IdImpl(name), cs);
						}
						Counts cs = countsMap.get(new IdImpl(name));
						
						String entryName = stationName + directionName;
						Count c = null;
						if(!cs.getCounts().containsKey(new IdImpl(entryName))){
							//							log.warn("Multiple entries for station " + entryName + " in " + name);
							c = cs.createCount(new IdImpl(entryName), entryName);
						} 
						c = cs.getCount(new IdImpl(entryName));
						for(Id hourName : cMap.get(cId).keySet()){
							if (cMap.get(cId).get(hourName).get(1) > 0){
								c.createVolume(
										Integer.parseInt(hourName.toString()), 			// hour
										(int) Math.round((double)cMap.get(cId).get(hourName).get(0) /  	// volume: total counts / observations
										(double)cMap.get(cId).get(hourName).get(1))
								);
							} else{
								if(hourName == null){
									log.warn("WARNING");
								}
								c.createVolume(
										Integer.parseInt(hourName.toString()), 
										0.0);
							}

						}
					}

				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
	
	/**
	 * Writes the {@link Counts} objects to file using the {@link CountsWriter}.
	 * @param root a {@link String} indicating the absolute pathname of the 
	 * 		directory where the <code>*.xml.gz</code> files are written to.  
	 */
	private void writeCountsXml(String root){
		log.info("Writing counts files to xml.");
		for (Id cId : countsMap.keySet()) {
			Counts c = countsMap.get(cId);
			
			String filename = root + "Counts_" + c.getName() + ".xml.gz";
			log.info("   writing: " + filename);
			CountsWriter cw = new CountsWriter(c);
			cw.useCompression(true);
			cw.write(filename);
		}
		log.info("Counts files completed.");
	}

	/**
	 * Provides a string description of the day based on the integer value in
	 * the {@link Calendar#DAY_OF_WEEK} field. 
	 * @param i the integer relating to the specific day of the week (<i>Note:</i> 
	 * 		  Sunday is considered the first day of the week.)
	 * @return a {@link String} description of the day.
	 * @see {@link Calendar}
	 */
	private String getDayDescription(int i){
		String day = null;
		switch (i) {
		case 1:
			day = "Sunday";
			break;
		case 2:
			day = "Monday";
			break;
		case 3:
			day = "Tuesday";
			break;
		case 4:
			day = "Wednesday";
			break;
		case 5:
			day = "Thursday";
			break;
		case 6:
			day = "Friday";
			break;
		case 7:
			day = "Saturday";
			break;
		case 8:
			day = "Abnormal";
			break;
		default:
			log.warn("Could not find a suitable day for the value " + i);
			break;
		}
		return day;
	}
	
	/**
	 * Creates and returns a {@link Map} with an entry for each hour. Each entry consists of two 
	 * {@link Integer}s: firstly the total counts observed; and secondly the
	 * number of observations.
	 */
	private Map<Id, List<Integer>> buildCountsMap(){
		Map<Id, List<Integer>> obs = new HashMap<Id, List<Integer>>();
		for(int i = 0; i < 24; i++ ){
			List<Integer> l = new ArrayList<Integer>(2);
			l.add(0); l.add(0);
			obs.put(new IdImpl(i), l);
		}
		return obs;	
	}

	/** 
	 * Reads a flat file containing one date per line in the format <code>YYYY/MM/DD</code>.
	 * @param filename absolute path of the file to be read;
	 */
	private void getAbnormalDays(String filename){
		log.info("Reading abnormal days from " + filename + "...");
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					line = line.replaceAll(" ", "");
					line = line.replace("\t", "");
					String[] date = line.split("/");
					int year = Integer.parseInt(date[0]);
					int month = Integer.parseInt(date[1]);
					int day = Integer.parseInt(date[2]);
					GregorianCalendar gc = new GregorianCalendar(year,month,day);
					abnormalList.add(gc);
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done.");
	}
}
