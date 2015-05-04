/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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
package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to check if new raw Digicore files were added to the server.
 * 
 * @author jwjoubert
 */
public class AggregateRawDigicoreFiles {
	final private static Logger LOG = Logger.getLogger(AggregateRawDigicoreFiles.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AggregateRawDigicoreFiles.class.toString(), args);
		
		String rawFolder = args[0];
		String processedFolder = args[1];
		String logFolder = args[2];

		try {
			moveEventsFile(args);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot move the events file.");
		}
		checkFileStatus(args);
		
		Header.printFooter();
	}
	
	
	public static void moveEventsFile(String[] args) throws IOException{
		File eventsFile = new File(args[0] + (args[0].endsWith("/") ? "" : "/") + "events.csv.gz");
		if(eventsFile.exists()){
			LOG.info("There exists an 'events.xml.gz' file in the input folder " + args[0]);
			
			File newEventsFile = new File(args[1] + (args[1].endsWith("/") ? "" : "/") + "events.csv.gz");
			if(newEventsFile.exists()){
				LOG.warn("In moving the 'events.xml.gz' file, the following file will be replaced: ");
				LOG.warn("   " + newEventsFile.getAbsolutePath());
			}
			
			FileUtils.copyFile(eventsFile, newEventsFile);
			
			/* Remove the original file. */
			eventsFile.delete();
		} else{
			LOG.info("No 'events.csv.gz' file exists in the input folder " + args[0]);
		}
	}
	
	
	/**
	 * 
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public static List<String> parseFileRegister(String[] args) throws IOException{
		/* Read the register of previously processed files. */
		List<String> list = new ArrayList<>();
		String sRegister = args[2] + (args[2].endsWith("/") ? "" : "/") + "registerOfProcessedFiles.csv";
		File fRegister = new File(sRegister);
		if(fRegister.exists()){
			BufferedReader br = IOUtils.getBufferedReader(sRegister);
			try{
				String line = br.readLine();
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					list.add(sa[0]);
				}
			} finally {
				br.close();
			}
		}
		return list;
	}
	
	
	public static void processRawFiles(String[] args) throws IOException{
		String sRegister = args[2] + (args[2].endsWith("/") ? "" : "/") + "registerOfProcessedFiles.csv";
		File fRegister = new File(sRegister);
		boolean registerNeedsHeader = true;
		if(fRegister.exists()){
			registerNeedsHeader = false;
		}
		
		List<String> register = parseFileRegister(args);
		
		LOG.info("Processing raw input files...");
		/* Get all the input files to process. */
		List<File> files = FileUtils.sampleFiles(new File(args[0]), Integer.MAX_VALUE, FileUtils.getFileFilter("csv.gz"));
		
		Map<File, GregorianCalendar> processedMap;

		if(files != null){
			processedMap = new HashMap<File, GregorianCalendar>(files.size());
			/* Copy the original allData.csv.gz file, for safe keeping. */
			String allRecords = args[1] + (args[1].endsWith("/") ? "" : "/") + "allData.csv.gz";
			File allRecordsFile = new File(allRecords);
			String allRecordsTmp = args[1] + (args[1].endsWith("/") ? "" : "/") + "allDataTmp.csv.gz";
			File allRecordsTmpFile = new File(allRecordsTmp);
			int statusOverall = 0;
			
			if(allRecordsFile.exists()){
				FileUtils.copyFile(allRecordsFile, allRecordsTmpFile);
			}
			
			BufferedWriter bw = IOUtils.getBufferedWriter(allRecords);
			try{
				/* Copy the original allData.csv.gz records, if it existed. */
				if(allRecordsTmpFile.exists()){
					BufferedReader brOriginal = IOUtils.getBufferedReader(allRecordsTmp);
					try{
						String line = null;
						while((line = brOriginal.readLine()) != null){
							bw.write(line);
							bw.newLine();
						}
					} finally{
						brOriginal.close();
					}
				}
				
				/* Process each file. */
				Iterator<File> fileIterator = files.iterator();
				while(fileIterator.hasNext() && statusOverall == 0){
					File file = fileIterator.next();
					String filename = file.getName();
					
					/* Only handle the file if it has not yet been processed. */
					if(register.contains(filename)){
						LOG.warn("Why is there a duplicate?! This file has already been logged as processed.");
						LOG.error("Handle this file manually: " + file.getAbsolutePath());
						statusOverall = 1;
					} else{
						LOG.info("   Processing " + file.getName() + "...");
						
						/* Do the processing. */
						int status = 1;
						try{
							BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
							try{
								String line = null;
								while((line = br.readLine()) != null){
									bw.write(line);
									bw.newLine();
								}
								
								/* If the code reaches this point, all records were 
								 * written, and the status can be changed to 0. */
								status = 0;
							} finally{
								br.close();
							}
						} finally{
							bw.close();
						}
						if(status != 0){
							statusOverall = 1;
						} else{
							GregorianCalendar processed = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
							processed.setTimeInMillis(System.currentTimeMillis());
							processedMap.put(file, processed);
						}
					}
				}
			} finally{
				bw.close();
			}
			
			/* Confirm temporary file, or revert back if the overall status is
			 * not 0 anymore. */
			if(statusOverall == 0){
				/* Move the raw data files. */
				for(File rawFile : files){
					GregorianCalendar modified = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
					modified.setTimeInMillis(rawFile.lastModified());
					String modifiedString = DigicoreUtils.getPrettyDateAndTime(modified);
					String processedString = DigicoreUtils.getPrettyDateAndTime(processedMap.get(rawFile));

					/* Update the register. */
					BufferedWriter bwRegister = IOUtils.getAppendingBufferedWriter(sRegister);
					try{
						if(registerNeedsHeader){
							bwRegister.write("filename,modified,processed");
							bwRegister.newLine();
						}
						bwRegister.write(String.format("%s,%s,%s\n", rawFile.getName(), modifiedString, processedString));
					} finally{
						bwRegister.close();
					}
					
					/* Move the file. */
					String newLocation = args[1] + (args[1].endsWith("/") ? "" : "/") + rawFile.getName();
					File newLocationFile = new File(newLocation);
					FileUtils.copyFile(rawFile, newLocationFile);
					boolean deleted = rawFile.delete();
					if(!deleted){
						LOG.warn("File cannot be deleted after updating register: " + rawFile.getAbsolutePath());
					}
				}
				/* Success. Keep the new allData.csv.gz file. */
				LOG.info("Copying one or more raw files were successfull.");
				LOG.info("Keeping new adapted allData.csv.gz file.");
				allRecordsTmpFile.delete();
				
			} else{
				/* Failure. Revert to previous allData.csv.gz file. */
				LOG.error("Copying one or more raw files have failed.");
				LOG.error("Reverting to old allData.csv.gz file.");
				allRecordsFile.delete();
				if(allRecordsTmpFile.exists()){
					FileUtils.copyFile(allRecordsTmpFile, allRecordsFile);
					allRecordsTmpFile.delete();
				}
			}
		}
		LOG.info("Done processing raw input files.");
	}
	
	
	public static String getPrettyDate(GregorianCalendar g){
		String result = "";
		
		return result;
	}
	
	
	public static void checkFileStatus(String[] args){
		LOG.info("Reporting file status...");
		
		/* Check the input directory. */
		File folder = new File(args[0]);
		List<File> files = null;
		if(!folder.isDirectory()){
			LOG.error("The input directory " + args[0] + " is not a directory!");
			throw new IllegalArgumentException("Need a valid input directory!");
		} else{
			files = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter("csv.gz"));
			if(files != null){
				LOG.info("The input directory " + args[0] + " exists and contains " + files.size() + " files with extension '*.csv.gz'");
			} else{
				LOG.info("The input directory " + args[0] + " exists and contains no files with extension '*.csv.gz'");
			}
		}
		
		/* Check the file register. */
		File register = new File(args[2] + "");
		if(register.exists() && register.isFile()){
			LOG.info("The file register " + args[2] + "does exist.");
			LOG.info("   The following files have been processed in the past:");
			
			/* Report the files already processed. */
			BufferedReader br = IOUtils.getBufferedReader(register.getAbsolutePath());
			try{
				String line = br.readLine(); /* Header. */
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					String filename = sa[0];
					String date = sa[1];
					LOG.info(String.format("%s%s (%s);", "      ", filename, date));
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot read from file register " + register.getAbsolutePath());
			} finally{
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close file register " + register.getAbsolutePath());
				}
			}
		} else{
			LOG.warn("The file register " + args[2] + " does not exist. If needed, a new one will be created.");
		}
		
		LOG.info("Done reporting file status.");
	}

}
