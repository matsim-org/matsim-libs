/* *********************************************************************** *
 * project: org.matsim.*
 * GetDateString.java
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

package playground.jjoubert.digicoreNew.step1_Split;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.jjoubert.Utilities.DateString;

public class SplitDigicoreFile {
	private static Logger log = Logger.getLogger(SplitDigicoreFile.class);
	
	private static String root;
	private static String filename;
	private static long startLine;
	
	private static int fieldVehId;
	private static int fieldTime;
	private static int fieldLong;
	private static int fieldLat;
	private static int fieldStatus;
	private static int fieldSpeed;
	
	private static String delimiter = ",";
	private static long numberOfLinesToRead = Long.MAX_VALUE;

	/**
	 * This class only has a main method. It is invoked on its own to process the single file 
	 * provided by DigiCore holdings, <code>Poslog_Research_Data.csv</code>, and split it into 
	 * separate files, one for each vehicle. The file provided by DigiCore has six comma 
	 * delimited fields in the following order:
	 * 
	 * <nl>
	 * 	<li> <em>VehicleId</em>, a unique vehicle identifier from the vehicle sending the GPS log entry; 
	 * 	<li> <em>Time</em>, a UNIX-based time stamp (in seconds);
	 * 	<li> <em>Longitude</em>, the x-coordinate of the GPS log (in decimal degrees);
	 * 	<li> <em>Latitude</em>, the y-coordinate of the GPS log (in decimal degrees); 
	 * 	<li> <em>Status</em>, an integer digit indicating a predefined vehicle status. Status codes are described in <code>Statuses.xls</code>;
	 * 	<li> <em>Speed</em>, a field reflecting the speed of the vehicle when sending the log entry. This field, however, proved to be quite useless.
	 * </nl>
	 * <hr>
	 * The extent of the data is known to be 1 January 2008 - 30 June 2008.
	 * 
	 * <h4>
	 * Process
	 * </h4>
	 * <p>A single line is read from the input file (comma delimited), and die vehicle ID is 
	 * determined. The associated vehicle file is opened (with amending privileges), and the 
	 * line is added (again comma delimited) to the vehicle file. In an attempt to save time,
	 * I've implemented the reading such that the next line is also read. If the new line
	 * relates to the same vehicle, the vehicle file is kept open. Otherwise, the file is closed,
	 * and the new vehicle file is opened.</p> 
	 * 
	 * <br>Since the process runs over multiple days, and interruptions during the process can
	 * lead to lost time, a <i>safety mechanism</i> has been employed: after every line read, 
	 * the total number of lines read (and processed) is written to a text file named
	 * <code>logRecordsRead_*.txt</code> where <code>`*'</code> denotes the time stamp (in seconds)
	 * when the process started. This allows the user to set two parameters. This gives the user
	 * flexibility in terms of which line of the input files should be processed first, and how
	 * many lines should be processed. An interrupted job can then be easily <i>continued</i> by
	 * just checking what the last line was that was processed.   
	 * @param startLine the first line in the input file to be read. If the whole file is to be 
	 * 		  processed, then this value should be zero;
	 * @param numberOfLinesToRead the total number of lines to be read. If the whole is to be
	 * 	   	  processed, then this value should be <code>Long.MAX_VALUE</code>.
	 * 
	 * @author jwjoubert
	 */
	public static void main( String[] args){
		if(args.length != 9){
			throw new RuntimeException("Must provide 9 field arguments: filename, startLine, root, and the field locations for VehId, Time, Long, Lat, Status and Speed.");
		}
		filename = args[0];
		startLine = Integer.parseInt(args[1]);
		root = args[2];		
		
		fieldVehId = Integer.parseInt(args[3]);
		fieldTime = Integer.parseInt(args[4]);
		fieldLong = Integer.parseInt(args[5]);
		fieldLat = Integer.parseInt(args[6]);
		fieldStatus = Integer.parseInt(args[7]);
		fieldSpeed = Integer.parseInt(args[8]);
		
		
		log.info("=================================================================");
		log.info("  Splitting the DigiCore data file into seperate vehicle files.");
		log.info("=================================================================");
		long line = 0;
		long linesRead = 0;
		long reportValue = 1;

		File outputFolder = new File(root + "Vehicles/");
		if(outputFolder.exists()){
			String s = "The folder already exists! Delete " + outputFolder.getPath() + " and rerun.";
			throw new RuntimeException(s);
		} else{
			boolean checkDirectory = outputFolder.mkdirs();
			if(!checkDirectory){
				log.warn("Could not make " + outputFolder.toString() + ", or it already exists!");
			}
		}		

		BufferedReader input = null;
		BufferedWriter output = null;
		String vehicleFile = null;
		
		DateString ds = new DateString();
		try {
			BufferedWriter logRecords = IOUtils.getBufferedWriter(root + "logRecordsRead_" + ds.toString() + ".txt");
			try{
				try {
					input = IOUtils.getBufferedReader(root + filename);
					String inputLine = null;
					while((inputLine = input.readLine()) != null){
						if(++line >= startLine && linesRead <= numberOfLinesToRead){
							String [] inputString = inputLine.split(delimiter);
							if(inputString.length == 6){
								/* Open the file for the vehicle
								 * TODO Change this to *.txt.gz oonce Marcel changed IOUTils.getBufferedWriter to allow appending  
								 */
								vehicleFile = root + "Vehicles/" + inputString[fieldVehId] + ".txt";
								output = new BufferedWriter(new FileWriter(vehicleFile, true) , 10000 );
								
								// Write the record to the associated file
								output.write(inputString[fieldVehId]); // Vehicle ID
								output.write(delimiter);
								output.write(inputString[fieldTime]); // Time stamp
								output.write(delimiter);
								output.write(inputString[fieldLong]); // X (longitude)
								output.write(delimiter);
								output.write(inputString[fieldLat]); // Y (latitude)
								output.write(delimiter);
								output.write(inputString[fieldStatus]); // Status
								output.write(delimiter);
								output.write(inputString[fieldSpeed]); // Speed
								output.newLine();
								
								logRecords.write(String.valueOf(linesRead));
								logRecords.newLine();
								
								// Update report
								if(++linesRead == reportValue){
									log.info("   Lines read... " + linesRead);
									reportValue *= 2;
								}
							} else{
								log.warn("Line " + line + " does not contain 6 entries.");
							}	
							
							String vehID = inputString[fieldVehId];
							while( (inputLine = input.readLine()) != null){
								
								// Read the next input line
								inputString = inputLine.split(delimiter);								
								if(++line >= startLine && linesRead <= numberOfLinesToRead){
									if(inputString.length == 6){
										if ( !vehID.equalsIgnoreCase(inputString[fieldVehId])) {
											// Close the file for the current vehicle.
											output.close();
											
											/* Open the file for the new vehicle
											 * TODO Change this to *.txt.gz oonce Marcel changed IOUTils.getBufferedWriter to allow appending  
											 */
											vehicleFile = root + "Vehicles/" + inputString[fieldVehId] + ".txt";
											try {
												output = new BufferedWriter(new FileWriter(vehicleFile, true) , 100000 );
											} catch (IOException e) {
												e.printStackTrace();
											}			
										}
										
										// Write the record to the new file
										output.write(inputString[fieldVehId]); // Vehicle ID
										output.write(delimiter);
										output.write(inputString[fieldTime]); // Time stamp
										output.write(delimiter);
										output.write(inputString[fieldLong]); // X (longitude)
										output.write(delimiter);
										output.write(inputString[fieldLat]); // Y (latitude)
										output.write(delimiter);
										output.write(inputString[fieldStatus]); // Status
										output.write(delimiter);
										output.write(inputString[fieldSpeed]); // Speed
										output.newLine();
										
										logRecords.write(String.valueOf(linesRead));
										logRecords.newLine();
										
										vehID = inputString[fieldVehId];
										
										// Update report
										if(++linesRead == reportValue){
											log.info("   Lines read... " + linesRead);
											reportValue *= 2;
										}
									}
								} else{
									line++;
								}
								
							}
						} else{
							line++;
						}
						
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally{
				logRecords.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
					
		log.info("   Lines read... " + linesRead + " (Done)");
		log.info("----------------------------------");
		log.info("   Process complete.");
		log.info("==================================");
	}
}
