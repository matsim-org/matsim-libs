/* *********************************************************************** *
 * project: org.matsim.*
 * SplitAfcDataByDay.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.afc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to split a given Automated Fare Collection (AFC) data set from City of 
 * Cape Town into a day-by-day schedule, and then sorting each file.
 * 
 * @author jwjoubert
 */
public class AfcDataSplitter {
	final private static Logger LOG = Logger.getLogger(AfcDataSplitter.class);
	private static String HEADER;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcDataSplitter.class.toString(), args);
		String inputFile = args[0];
		String outputFolder = args[1];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		
		splitAfcDataByDay(inputFile, outputFolder);
		sortAfcData(outputFolder);
		Header.printFooter();
	}

	
	private AfcDataSplitter() {
		/* Hide. */
	}
	
	public static void splitAfcDataByDay(String inputFile, String outputFolder){
		File folder = new File(outputFolder);
		if(folder.exists() && folder.isDirectory() && folder.list().length > 0){
			try {
				throw new IOException("Folder not empty.");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("First clean the output folder " + outputFolder);
			}
		}
		folder.mkdirs();
		
		LOG.info("Splitting the AFC data from " + inputFile + "...");
		
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		BufferedWriter bw = null;
		String currentDate = null;
		Counter counter = new Counter("  lines # ");
		try{
			String line = br.readLine(); /* Header. */
			HEADER = line;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String date = sa[8];
				if(!date.equalsIgnoreCase(currentDate)){
					if(currentDate != null){
						bw.close();
					}
					bw = IOUtils.getAppendingBufferedWriter(outputFolder + date + ".csv");
					currentDate = date;
				}
				bw.write(line);
				bw.newLine();
				
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Oops...");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Oops...");
			}
		}
		counter.printCounter();
		LOG.info("Done splitting the input file.");
		
	}
	
	public static void sortAfcData(String outputFolder){
		LOG.info("Sorting the AFC data in " + outputFolder + "...");
		File folder = new File(outputFolder);
		List<File> files = FileUtils.sampleFiles(
				folder, Integer.MAX_VALUE, 
				FileUtils.getFileFilter(".csv"));
		
		for(File file : files){
			LOG.info("  Sorting " + file.getAbsolutePath());
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			BufferedWriter bw = IOUtils.getBufferedWriter(file.getAbsolutePath() + ".gz");
			Map<String, List<String>> map = new TreeMap<String, List<String>>();
			Counter counter = new Counter("    lines # ");
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					if(sa.length == 13){
						String hour = sa[9];
						String minutes = sa[12];
						String time = String.format("%02d%02d", 
								Integer.parseInt(hour),
								Integer.parseInt(minutes));
						if(!map.containsKey(time)){
							map.put(time, new ArrayList<String>());
						}
						map.get(time).add(line);
					} else{
						LOG.warn("Line length: " + sa.length);
					}
					counter.incCounter();
				}
				
				
				Comparator<String> typeComparator = new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						String[] sa1 = o1.split(",");
						String[] sa2 = o2.split(",");
						
						Integer type1 = getType(sa1[6]);
						Integer type2 = getType(sa2[6]);
						
						return type1.compareTo(type2);
					}
					
					private int getType(String s){
						if(s.equalsIgnoreCase("1st boarding")){
							return 0;
						} else if(s.equalsIgnoreCase("Alighting")){
							return 1;
						} else if (s.equalsIgnoreCase("Connection")){
							return 2;
						} else if(s.equalsIgnoreCase("Cancellation")){
							return 3;
						} else{
							throw new RuntimeException("Don't know what tap type is: " + s);
						}
					}
				};
				
				
				/* Write the sorted map to file. */
				bw.write(HEADER);
				bw.newLine();
				for(String s : map.keySet()){
					List<String> list = map.get(s);
					/* Sort the per-minute list. */
					Collections.sort(list, typeComparator);
					
					for(String ss : list){
						bw.write(ss);
						bw.newLine();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Oops...");
			} finally{
				try {
					br.close();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Oops...");
				}
			}
			counter.printCounter();
			boolean deleted = file.delete();
			if(!deleted){
				LOG.error("Could not delete " + file.getAbsolutePath());
			}
		}
		
		LOG.info("Done splitting the input file.");
		
	}
	
	
	
	
	
	
	
}
