/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractAccelerometerIndividuals.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.projects.digicore.automation.AggregateRawDigicoreFiles;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to read the monthly files processed automatically by the 
 * {@link AggregateRawDigicoreFiles} on the <code>ie-susie.up.ac.za</code> 
 * server.
 * 
 * @author jwjoubert
 */
public class ByIdSplitter {
	final private static Logger LOG = Logger.getLogger(ByIdSplitter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ByIdSplitter.class.toString(), args);
		
		String inputFile = args[0];
		String outputFolder = args[1];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		int option = Integer.parseInt(args[2]);
		
		switch (option) {
		case 1:
			/* Split the combined file by vehicle ID. */
			splitAccelerometerFileById(inputFile, outputFolder);
			break;
		case 2:
			processAccelerometerVehicles(outputFolder, 1);
			break;
		case 3:
			splitWasteFileById(inputFile, outputFolder);
			break;
		case 4:
			processAccelerometerVehicles(outputFolder, 2);
			break;
		default:
		}
		
		Header.printFooter();
	}


	private ByIdSplitter(){
	}
	
	
	
	private static void processAccelerometerVehicles(String outputFolder, int option) {
		Map<String, Integer> map = new TreeMap<String, Integer>();

		List<File> files = FileUtils.sampleFiles(new File(outputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".csv"));
		for(File file : files){
			String oldId = file.getName().substring(0, file.getName().indexOf("."));

			if(!map.containsKey(oldId)){
				map.put(oldId, map.size()+1);
			}
			int newId = map.get(oldId);

			LOG.info(String.format("  Splitting %3s. %s", String.valueOf(newId), oldId));
			switch (option) {
			case 1:
				splitAccelerometerTrips(file, String.valueOf(newId));
				break;
			case 2:
				splitWasteTrips(file, String.valueOf(newId));
			default:
				break;
			}
		}
	}
	
	
	public static void splitWasteTrips(File file, String newId){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		File folder = file.getParentFile();
		String folderName = folder.getAbsolutePath() + (folder.getAbsolutePath().endsWith("/") ? "" : "/") + "split/";
		File newFolder = new File(folderName);
		if(!newFolder.exists()){
			newFolder.mkdirs();
		}
		
		/* Sort the file. */
		BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
	
		Map<String, String> sortedRecords = new TreeMap<>();
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String dateTime = line.substring(30, 54);
				double lon = Double.parseDouble(line.substring(62, 79));
				double lat = Double.parseDouble(line.substring(85, 103));
				
				/* Check basic coordinates. */
				if(lon >= 15.0 && lon <= 33.0 && lat <= -21.0 && lat >= -35.0){
					Coord c = ct.transform(CoordUtils.createCoord(lon, lat));
					
					String ignition = line.substring(104, 105);
					boolean isOn = ignition.equalsIgnoreCase("T") ? true : false;
					
					String cleanString = String.format("%.1f,%.1f,%s", c.getX(), c.getY(), String.valueOf(isOn));
					sortedRecords.put(dateTime, cleanString);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + file.getAbsolutePath());
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + file.getAbsolutePath());
			}
		}
		
		/* Now process the records. */
		Iterator<String> dateStringIterator = sortedRecords.keySet().iterator();
		Map<String, List<String>> dayMap = new TreeMap<>();
		while(dateStringIterator.hasNext()){
			String dateString = dateStringIterator.next();
			String date = dateString.substring(0, 10).replaceAll("-", "");
			String timeOfDay = dateString.substring(11, 21);
			
			String[] entry = sortedRecords.get(dateString).split(",");
			String cleanString = String.format("%s,%s,%.1f", entry[0], entry[1], Time.parseTime(timeOfDay));
			
			if(!dayMap.containsKey(date)){
				dayMap.put(date, new ArrayList<String>());
			}
			dayMap.get(date).add(cleanString);
		}
		
		/* Now write each trip/day to file. */
		for(String date : dayMap.keySet()){
			String tripName = folderName + newId + "_" + date + ".csv.gz";
			BufferedWriter bw = IOUtils.getBufferedWriter(tripName);
			try{
				List<String> records = dayMap.get(date);
				for(String s : records){
					bw.write(s);
					bw.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + tripName);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + tripName);
				}
			}
		}
	}
	
	
	public static void splitAccelerometerTrips(File file, String newId){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		File folder = file.getParentFile();
		String folderName = folder.getAbsolutePath() + (folder.getAbsolutePath().endsWith("/") ? "" : "/") + "split/";
		File newFolder = new File(folderName);
		if(!newFolder.exists()){
			newFolder.mkdirs();
		}
		
		BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
		int trips = 0;
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(folderName + newId + "_" + String.format("%03d.csv", trips));
		try{
			String line = br.readLine();
			String[] sa = line.split(",");
			long previousTime = Long.parseLong(sa[2]);
			
			while((line = br.readLine()) != null){
				sa = line.split(",");
				long time = Long.parseLong(sa[2]);
				double diff = ((double)(time - previousTime)) / 1000.0;
				double lon = Double.parseDouble(sa[3]);
				double lat = Double.parseDouble(sa[4]);
				Coord c = ct.transform(CoordUtils.createCoord(lon, lat));
				
				/* Get a more usable date format. */
				Calendar cal = DigicoreUtils.getCalendarSince1996(time);
				double MatsimTime = Time.parseTime(DigicoreUtils.getTimeOfDayFromCalendar(cal));
				String cleanLine = String.format("%.2f,%.2f,%.3f\n", c.getX(), c.getY(), MatsimTime);
				if(diff <= Time.parseTime("00:01:00")){
					bw.write(cleanLine);
				} else{
					bw.close();
					trips++;
					bw = IOUtils.getAppendingBufferedWriter(folderName + newId + "_" + String.format("%03d.csv", trips));
					bw.write(cleanLine);
//					LOG.info("  diff: " + ((double)diff)/1000.0 + "sec");
				}
				previousTime = time;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + file.getAbsolutePath());
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + file.getAbsolutePath());
			}
		}
	}
	
	
	public static void splitWasteFileById(String inputFile, String outputFolder){
		LOG.info("Splitting file " + inputFile);
		
		/* Check the output folder. */
		File folder = new File(outputFolder);
		if(folder.exists() && folder.isDirectory()){
			if(folder.list().length > 0){
				LOG.error("Output directory is not empty: " + outputFolder);
				throw new RuntimeException("First delete the folder's contents.");
			}
		} else{
			folder.mkdirs();
		}
		
		List<String> idList = new ArrayList<>();
		Counter counter = new Counter("  lines # ");
		
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		String previousId = null;
		try{
			String line = null;
			BufferedWriter bw = null;
			while((line = br.readLine()) != null){
				if(		!line.startsWith("CON_NO") && 
						!line.startsWith("=") && 
						line.length() == 117 ){
					String id = parseIdFromLine(line, 2);
					if(id.equalsIgnoreCase(previousId)){
						/* Keep the current BufferedWriter. */
					} else{
						if(bw != null){
							bw.close();
						}
						if(!idList.contains(id)){
							idList.add(id);
						}
						bw = IOUtils.getAppendingBufferedWriter(outputFolder + id + ".csv");
					}
					
					bw.write(line);
					bw.newLine();
					counter.incCounter();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read/write.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close file.");
			}
		}
		counter.printCounter();
		
		LOG.info("Done splitting file.");
		LOG.info("Individual output files are written to " + outputFolder);
		LOG.info("Identified IDs:");
		for(String s : idList){
			LOG.info("  " + s); 
		}
	}
	
	public static void splitAccelerometerFileById(String inputFile, String outputFolder){
		LOG.info("Splitting file " + inputFile);
		
		/* Check the output folder. */
		File folder = new File(outputFolder);
		if(folder.exists() && folder.isDirectory()){
			if(folder.list().length > 0){
				LOG.error("Output directory is not empty: " + outputFolder);
				throw new RuntimeException("First delete the folder's contents.");
			}
		} else{
			folder.mkdirs();
		}
		
		List<String> idList = new ArrayList<>();
		Counter counter = new Counter("  lines # ");

		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		String previousId = null;
		try{
			String line = null;
			
			BufferedWriter bw = null;
			while((line = br.readLine()) != null){
				String id = parseIdFromLine(line, 1);
				if(id.equalsIgnoreCase(previousId)){
					/* Keep the current BufferedWriter. */
				} else{
					if(bw != null){
						bw.close();
					}
					if(!idList.contains(id)){
						idList.add(id);
					}
					bw = IOUtils.getAppendingBufferedWriter(outputFolder + id + ".csv");
				}
				
				bw.write(line);
				bw.newLine();
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read/write.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close file.");
			}
		}
		counter.printCounter();
		
		LOG.info("Done splitting file.");
		LOG.info("Individual output files are written to " + outputFolder);
		LOG.info("Identified IDs:");
		for(String s : idList){
			LOG.info("  " + s); 
		}
	}
	
	private static String parseIdFromLine(String line, int inputType){
		String id = null;
		switch (inputType) {
		case 1:
			/* Digicore accelerometer data. */
			String[] sa = line.split(",");
			id = sa[1];
			break;
		case 2:
			/* City of Cape Town's waste traces. */
			id = line.substring(9, 29).replaceAll(" ", "");
			break;
		default:
			LOG.error("Don't know how to parse vehicle id from data type " + inputType);
			break;
		}
		
		
		return id;
	}
	

}
