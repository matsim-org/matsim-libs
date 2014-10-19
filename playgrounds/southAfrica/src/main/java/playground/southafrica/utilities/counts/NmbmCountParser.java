/* *********************************************************************** *
 * project: org.matsim.*
 * NmbmCountParser.java
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

package playground.southafrica.utilities.counts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.southafrica.utilities.Header;

/**
 * Parsing the traffic counts from the data provided by Rodney Steinhofel,
 * PBS Consulting, for the Nelson Mandela Bay Metropolitan area.
 *  
 * @author johanwjoubert
 */
public class NmbmCountParser {
	private final static Logger LOG = Logger.getLogger(NmbmCountParser.class);
	private Counts counts_light;
	private Counts counts_short;
	private Counts counts_medium;
	private Counts counts_long;
	private Counts counts_total;
	private Map<Id<Link>, List<Integer[]>> countMap = new TreeMap<>();
	private Map<Id<Link>, Double[]> avgCountMap = new TreeMap<>();
	private Map<Id<Link>, Double[]> classCountMap = new TreeMap<Id<Link>, Double[]>();

	
	private final int[] setLight = {1,2,3};
	private final int[] setShort = {4,5};
	private final int[] setMedium = {6,7,8,9};
	private final int[] setLong = {10,11,12,13,14,15,16,17};
	
	private int dummyCounter = 1;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmCountParser.class.toString(), args);
		String inputFolder = args[0];
		/* If the input folder is a file, then only read that file. Otherwise,
		 * sample all the relevant files, assuming they all start with the
		 * phrase "Station_". */
		
		NmbmCountParser cp = new NmbmCountParser();
		cp.parse(inputFolder);	
		cp.process(inputFolder);
		cp.writeCounts(inputFolder);
		
		Header.printFooter();
	}
	
	public NmbmCountParser() {
		counts_light = new Counts();
		counts_light.setName("NMBM_Light");
		counts_light.setDescription("Nelson Mandela Bay Metropolitan, light vehicles only");
		counts_short = new Counts();
		counts_short.setName("NMBM_Short");
		counts_short.setDescription("Nelson Mandela Bay Metropolitan, short heavy vehicles only");
		counts_medium = new Counts();
		counts_medium.setName("NMBM_Medium");
		counts_medium.setDescription("Nelson Mandela Bay Metropolitan, medium heavy vehicles only");
		counts_long = new Counts();
		counts_medium.setName("NMBM_Long");
		counts_medium.setDescription("Nelson Mandela Bay Metropolitan, long heavy vehicles only");
		counts_total = new Counts();
		counts_total.setName("NMBM_Total");
		counts_total.setDescription("Nelson Mandela Bay Metropolitan, all vehicles");
	}
	
	
	public void parse(String filename){
		File f = new File(filename);
		if(f.isDirectory()){
			LOG.info("Parsing folder...");
			FileFilter ff = new FileFilter() {
				@Override
				public boolean accept(File arg0) {
					if(arg0.getName().startsWith("Station_")){
						return true;
					}
					return false;
				}
			};
			File[] listOfFiles = f.listFiles(ff);
			Counter counter = new Counter("  files # ");
			for(File file : listOfFiles){
				parseStation(file.getAbsolutePath());
				counter.incCounter();
			}
			counter.printCounter();
		} else if (f.isFile()){
			LOG.info("Parsing single file...");
			parseStation(filename);
		}
	}
	
	
	private void parseStation(String filename){
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = br.readLine(); /* Header */
			String header = "\"YEAR\",\"STATIONNR\",\"LANENO\",\"ENDDATETIME\",\"DURATION\",\"EDITCODE\",\"CLASS0\",\"CLASS1\",\"CLASS2\",\"CLASS3\",\"CLASS4\",\"CLASS5\",\"CLASS6\",\"CLASS7\",\"CLASS8\",\"CLASS9\",\"CLASS10\",\"CLASS11\",\"CLASS12\",\"CLASS13\",\"CLASS14\",\"CLASS15\",\"CLASS16\",\"CLASS17\",\"DAYDATE\",\"DAYTIME\"";
			if(!line.equalsIgnoreCase(header)){
				LOG.error("Wrong header!! " + filename);
			}
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String stationId = String.valueOf(Integer.parseInt(sa[1].replace("\"", "")));
				String lane = sa[2];
//				boolean found = false;
//				if(stationId.contains("1277")){
//					found = true;
//				}
				
				/* Convert the date */
				String dateTime = sa[3];
				String[] sadt = dateTime.split(" ");
				String date = sadt[0];
				String time = sadt[1];
				boolean am = sadt[2].equalsIgnoreCase("AM") ? true : false;
				String[] saTime = time.split(":");
				int hour = Integer.parseInt(saTime[0]);
				int minute = Integer.parseInt(saTime[1]);
				int second = Integer.parseInt(saTime[2]);
				minute += second*60;
				int hourOfDay;
				if(hour == 12 && am){
					/* The format indicate 12:00:00 AM as midnight */
					hour = 0;
				} else if(!am && hour != 12){
					/* Add twelve hours to all PM times. */
					hour += 12;
				}
				if(minute == 0){
					/* It is the end of the hour, so use the previous hour value. */
					hourOfDay = hour - 1;
				} else{
					/* It is within an hour, so use the current hour value. */
					hourOfDay = hour;
				}
				
				/* Parse the counts. */
				Integer[] ia = new Integer[17];
				for(int i = 0; i < 17; i++){
					ia[i] = Integer.parseInt(sa[7+i]);
				}
				
				Id<Link> id = Id.create(stationId + "_" + lane + "_" + hourOfDay, Link.class);
				if(!countMap.containsKey(id)){
					countMap.put(id, new ArrayList<Integer[]>());
				}
				countMap.get(id).add(ia);
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader "
					+ filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader "
						+ filename);
			}
		}
	}
	
	/**
	 * Initial processing of counts.
	 * @param folder
	 */
	public void process(String folder){
		/*TODO Check if there is an order sequence to averaging and aggregating. */ 
		
		/* Calculate the average. */
		LOG.info("Calculating the averages for each station...");
		for(Id<Link> id : countMap.keySet()){
			List<Integer[]> list = countMap.get(id);
			Double[] avg = new Double[17];
			for(int i = 0; i < 17; i++){
				double sum = 0.0;
				for(Integer[] ia : list){
					sum += ia[i];
				}
				avg[i] = sum / (list.size());
			}
			avgCountMap.put(id, avg);
		}
		LOG.info("Done calculating averages.");
		
		/* Aggregate into vehicle classes. */
		LOG.info("Aggregate counts to toll classes.");
		for(Id<Link> id : avgCountMap.keySet()){
			Double[] classCounts = {0.0, 0.0, 0.0, 0.0};
			Double[] ia = avgCountMap.get(id);
			/* Light vehicles. */
			for(int i : setLight){
				classCounts[0] += ia[i-1];
			}
			/* Short heavy vehicles. */
			for(int i : setShort){
				classCounts[1] += ia[i-1];
			}
			/* Medium heavy vehicles. */
			for(int i : setMedium){
				classCounts[2] += ia[i-1];
			}
			/* Long heavy vehicles. */
			for(int i : setLong){
				classCounts[3] += ia[i-1];
			}
			classCountMap.put(id, classCounts);
		}
		LOG.info("Done aggregating counts.");
		
		/* Write out the file for perusal. TODO Can delete once done. */
		LOG.info("Writing validation data to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(folder + "aaa.csv");
		try {
			for(Id<Link> id : countMap.keySet()){
				List<Integer[]> list = countMap.get(id);
				String[] sa = id.toString().split("_");
				for(Integer[] ia : list){
					bw.write(String.format("%s,%s,%s,", sa[0], sa[1], sa[2]));
					for(int i = 0; i < ia.length-1; i++){
						bw.write(String.format("%d,", ia[i]));
					}
					bw.write(String.format("%d\n", ia[ia.length-1]));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ folder);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ folder);
			}
		}
		BufferedWriter bw2 = IOUtils.getBufferedWriter(folder + "bbb.csv");
		try {
			for(Id<Link> id : avgCountMap.keySet()){
				Double[] da = avgCountMap.get(id);
				String[] sa = id.toString().split("_");
				bw2.write(String.format("%s,%s,%s,", sa[0], sa[1], sa[2]));
				for(int i = 0; i < da.length-1; i++){
					bw2.write(String.format("%.2f,", da[i]));
				}
				bw2.write(String.format("%.2f\n", da[da.length-1]));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ folder);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ folder);
			}
		}
		BufferedWriter bw3 = IOUtils.getBufferedWriter(folder + "ccc.csv");
		try {
			for(Id<Link> id : classCountMap.keySet()){
				Double[] da = classCountMap.get(id);
				String[] sa = id.toString().split("_");
				bw3.write(String.format("%s,%s,%s,", sa[0], sa[1], sa[2]));
				for(int i = 0; i < da.length-1; i++){
					bw3.write(String.format("%.2f,", da[i]));
				}
				bw3.write(String.format("%.2f\n", da[da.length-1]));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ folder);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ folder);
			}
		}
		LOG.info("Done writing validation data.");
		
		/* Sort out those stations that only has two lanes. */
		Map<Id<Link>, List<String>> laneList = new TreeMap<>();
		for(Id<Link> id : classCountMap.keySet()){
			String[] sa = id.toString().split("_");
			Id<Link> tmpId = Id.create(sa[0], Link.class);
			if(!laneList.containsKey(tmpId)){
				List<String> list = new ArrayList<String>();
				list.add(sa[1]);
				laneList.put(tmpId, list);
			} else{
				List<String> list = laneList.get(tmpId);
				if(!list.contains(sa[1])){
					list.add(sa[1]);
				}
			}
		}
		
		LOG.info("Creating counting station volumes...");
		List<Id> idToRemove = new ArrayList<Id>();
//		for(Id id : laneList.keySet()){
//			List<String> list = laneList.get(id);
//			if(list.size() == 2){
//				/* There are only one lane in each direction (I assume) */
//				
//				String[] sa = {"a","b"};
//				for(String suffix : sa){
//					Id dummyLinkId = Id.create("dummy" + dummyCounter++);
//					String direction = suffix.equalsIgnoreCase("a") ? "1" : "2";
//					Count count_light = counts_light.createCount(dummyLinkId, id.toString() + suffix);
//					Count count_short = counts_short.createCount(dummyLinkId, id.toString() + suffix);
//					Count count_medium = counts_medium.createCount(dummyLinkId, id.toString() + suffix);
//					Count count_long = counts_long.createCount(dummyLinkId, id.toString() + suffix);
//					Count count_total = counts_total.createCount(dummyLinkId, id.toString() + suffix);
//					for(int hour = 1; hour <= 24; hour++){
//						Id classId = Id.create(id.toString() + "_" + direction + "_" + (hour-1));
//						idToRemove.add(classId);
//						Double[] ia = classCountMap.get(classId);
//						double light = ia[0];
//						double s = ia[1];
//						double m = ia[2];
//						double l = ia[3];
//						count_light.createVolume(hour, light);
//						count_short.createVolume(hour, s);
//						count_medium.createVolume(hour, m);
//						count_long.createVolume(hour, l);
//						count_total.createVolume(hour, light + s + m + l);
//					}
//				}
//			}
//		}
		
		/* Remove solved stations, and report outstanding ones. */
		for(Id id : idToRemove){
			classCountMap.remove(id);
		}
		LOG.info("Outstanding counts: " + classCountMap.size());
		BufferedWriter output = IOUtils.getBufferedWriter(folder + "ddd.csv");
		try {
			output.write("Id,Light,Short,Medium,Long");
			output.newLine();
			Counter c = new Counter("  counts # ");
			for(Id id : classCountMap.keySet()){
				Double[] da = classCountMap.get(id);
				output.write(String.format("%s,%.2f,%.2f,%.2f,%.2f", id.toString(), da[0], da[1], da[2], da[3]));
				output.newLine();
				c.incCounter();
			}
			c.printCounter();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ folder + "ddd.csv");
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ folder + "ddd.csv");
			}
		}
		
		/* After inspecting the above output, the following fixes are made. */
		String[] sa = new String[4];

		addCounts("1277", new String[]{"1","2"}, "a");
		addCounts("3620", new String[]{"1"}, "a");
		addCounts("3620", new String[]{"2"}, "b");
		addCounts("40211", new String[]{"1"}, "a");
		addCounts("40211", new String[]{"2"}, "b");
		addCounts("40212", new String[]{"1"}, "a");
		addCounts("40212", new String[]{"2"}, "b");
		addCounts("40213", new String[]{"1"}, "a");
		addCounts("40213", new String[]{"2"}, "b");
		addCounts("40214", new String[]{"1"}, "a");
		addCounts("40214", new String[]{"2"}, "b");
		addCounts("40215", new String[]{"1"}, "a");
		addCounts("40215", new String[]{"2"}, "b");
		addCounts("40216", new String[]{"1"}, "a");
		addCounts("40216", new String[]{"2"}, "b");
		
		addCounts("40217", new String[]{"1","2"}, "a");
		addCounts("40217", new String[]{"3","4"}, "b");
		addCounts("403161", new String[]{"1","2","3"}, "a");
		addCounts("403162", new String[]{"1","2","3"}, "a");
		addCounts("403171", new String[]{"1","2","3"}, "a");
		addCounts("403172", new String[]{"1","2","3"}, "a");
		addCounts("403181", new String[]{"1","2","3"}, "a");
		addCounts("403182", new String[]{"1","2","3","4"}, "a");
		addCounts("40322", new String[]{"1"}, "a");
		addCounts("40322", new String[]{"2"}, "b");
		addCounts("40324", new String[]{"1"}, "a");
		addCounts("40324", new String[]{"2"}, "b");
		addCounts("40325", new String[]{"1"}, "a");
		addCounts("40325", new String[]{"2"}, "b");
		addCounts("40326", new String[]{"1"}, "a");
		addCounts("40327", new String[]{"1"}, "a");
		addCounts("40327", new String[]{"2"}, "b");
		addCounts("40328", new String[]{"1"}, "a");
		addCounts("40328", new String[]{"2"}, "b");
		addCounts("40395", new String[]{"1"}, "a");
		addCounts("40395", new String[]{"2"}, "b");
		addCounts("40396", new String[]{"1"}, "a");
		addCounts("40396", new String[]{"2"}, "b");
		addCounts("40397", new String[]{"1"}, "a");
		addCounts("40397", new String[]{"2"}, "b");
		addCounts("40398", new String[]{"1"}, "a");
		addCounts("40398", new String[]{"2"}, "b");
		addCounts("40399", new String[]{"1"}, "a");
		addCounts("40399", new String[]{"2"}, "b");
		addCounts("40401", new String[]{"1"}, "a");
		addCounts("40401", new String[]{"2"}, "b");
		addCounts("40402", new String[]{"1"}, "a");
		addCounts("40402", new String[]{"2"}, "b");
		addCounts("40403", new String[]{"1"}, "a");
		addCounts("40403", new String[]{"2"}, "b");
		addCounts("40404", new String[]{"1"}, "a");
		addCounts("40404", new String[]{"2"}, "b");
		addCounts("40405", new String[]{"1"}, "a");
		addCounts("40405", new String[]{"2"}, "b");
		addCounts("40406", new String[]{"1"}, "a");
		addCounts("40406", new String[]{"2"}, "b");
		addCounts("40407", new String[]{"1"}, "a");
		addCounts("40407", new String[]{"2"}, "b");
		addCounts("40408", new String[]{"1"}, "a");
		addCounts("40408", new String[]{"2"}, "b");
		addCounts("40409", new String[]{"1","2"}, "a");
		addCounts("40409", new String[]{"3","4"}, "b");
		addCounts("40410", new String[]{"1"}, "a");
		addCounts("40410", new String[]{"2"}, "b");
		addCounts("40411", new String[]{"1","2"}, "a");
		addCounts("40411", new String[]{"3","4"}, "b");
		addCounts("40412", new String[]{"1"}, "a");
		addCounts("40412", new String[]{"2"}, "b");
		addCounts("40413", new String[]{"1"}, "a");
		addCounts("40413", new String[]{"2"}, "b");
		addCounts("40414", new String[]{"1"}, "a");
		addCounts("40414", new String[]{"2"}, "b");
		addCounts("40415", new String[]{"1"}, "a");
		addCounts("40415", new String[]{"2"}, "b");
		addCounts("40416", new String[]{"1", "2"}, "a");
		addCounts("40417", new String[]{"1", "2"}, "a");
		addCounts("40418", new String[]{"1"}, "a");
		addCounts("40418", new String[]{"2"}, "b");
		addCounts("40420", new String[]{"1", "2"}, "a");
		addCounts("40421", new String[]{"1", "2"}, "a");
		addCounts("40422", new String[]{"1"}, "a");
		addCounts("40422", new String[]{"2"}, "b");
		addCounts("40423", new String[]{"1","2"}, "a");
		addCounts("40423", new String[]{"3","4"}, "b");
		addCounts("40424", new String[]{"1"}, "a");
		addCounts("40424", new String[]{"2"}, "b");
		addCounts("40425", new String[]{"1"}, "a");
		addCounts("40425", new String[]{"2"}, "b");
		addCounts("40426", new String[]{"1"}, "a");
		addCounts("40426", new String[]{"2"}, "b");
		addCounts("40427", new String[]{"1"}, "a");
		addCounts("40427", new String[]{"2"}, "b");
		addCounts("40428", new String[]{"1"}, "a");
		addCounts("40428", new String[]{"2"}, "b");
		addCounts("40430", new String[]{"1"}, "a");
		addCounts("40430", new String[]{"2"}, "b");
//		addCounts("40431", new String[]{"1","2"}, "a"); // There SHOULD be 4 lanes according to the year book.
//		addCounts("40431", new String[]{"3","4"}, "b");
		addCounts("40432", new String[]{"1","2"}, "a");
		addCounts("40432", new String[]{"3","4"}, "b");
		addCounts("40433", new String[]{"1"}, "a");
		addCounts("40434", new String[]{"1"}, "a");
		addCounts("40434", new String[]{"2"}, "b");
		addCounts("40435", new String[]{"1"}, "a");
		addCounts("40435", new String[]{"2"}, "b");
		addCounts("40436", new String[]{"1"}, "a");
		addCounts("40436", new String[]{"2"}, "b");
		addCounts("40437", new String[]{"1"}, "a");
		addCounts("40437", new String[]{"2"}, "b");
		addCounts("40438", new String[]{"1"}, "a");
		addCounts("40438", new String[]{"2","3"}, "b");
		addCounts("40439", new String[]{"1","2"}, "a");
		addCounts("40439", new String[]{"3","4"}, "b");
		
		LOG.info("Cleaned up. Counts remaining: " + classCountMap.size());
		output = IOUtils.getBufferedWriter(folder + "eee.csv");
		try {
			output.write("Id,Light,Short,Medium,Long");
			output.newLine();
			Counter c = new Counter("  counts # ");
			for(Id id : classCountMap.keySet()){
				Double[] da = classCountMap.get(id);
				output.write(String.format("%s,%.2f,%.2f,%.2f,%.2f", id.toString(), da[0], da[1], da[2], da[3]));
				output.newLine();
				c.incCounter();
			}
			c.printCounter();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ folder + "ddd.csv");
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ folder + "ddd.csv");
			}
		}
		
	}
	
	public void writeCounts(String folder){
		CountsWriter cw = new CountsWriter(counts_light);
		cw.write(folder + "nmbm_counts_light.xml");
		cw = new CountsWriter(counts_short);
		cw.write(folder + "nmbm_counts_short.xml");
		cw = new CountsWriter(counts_medium);
		cw.write(folder + "nmbm_counts_medium.xml");
		cw = new CountsWriter(counts_long);
		cw.write(folder + "nmbm_counts_long.xml");
		cw = new CountsWriter(counts_total);
		cw.write(folder + "nmbm_counts_total.xml");
		
	}
	
	private void addCounts(String station, String[] lanes, String suffix){
		List<Id<Link>> idToRemove = new ArrayList<>();
		Id<Link> dummyLinkId = Id.create("dummy" + dummyCounter++, Link.class);
		Count count_light = counts_light.createAndAddCount(dummyLinkId, station + suffix);
		Count count_short = counts_short.createAndAddCount(dummyLinkId, station + suffix);
		Count count_medium = counts_medium.createAndAddCount(dummyLinkId, station + suffix);
		Count count_long = counts_long.createAndAddCount(dummyLinkId, station + suffix);
		Count count_total = counts_total.createAndAddCount(dummyLinkId, station + suffix);
		for(int hour = 1; hour <= 24; hour++){
			/* Aggregate the vehicle counts for the different lanes. */
			double light = 0.0;
			double s = 0.0;
			double m = 0.0;
			double l = 0.0;
			for(String lane : lanes){
				Id<Link> classId = Id.create(station + "_" + lane + "_" + (hour-1), Link.class);
				idToRemove.add(classId);
				Double[] ia = classCountMap.get(classId);
				light += ia[0];
				s += ia[1];
				m += ia[2];
				l += ia[3];				
			}
			count_light.createVolume(hour, light);
			count_short.createVolume(hour, s);
			count_medium.createVolume(hour, m);
			count_long.createVolume(hour, l);
			count_total.createVolume(hour, light + s + m + l);
		}
		
		/* Clean up the class count map. */
		for(Id<Link> id : idToRemove){
			classCountMap.remove(id);
		}
	}

}

