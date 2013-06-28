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

package playground.southafrica.freight.digicore.extract.step1_split;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.DateString;

public class DigicoreFileSplitter {
	private static Logger log = Logger.getLogger(DigicoreFileSplitter.class);
	
	private String inputFilename;
	private String outputFolder;
	private long startLine;
	private long numberOfLinesToRead;	
	private String delimiter = ",";
	
	private long earliest = Long.MAX_VALUE;
	private long latest = Long.MIN_VALUE;
	
	
	/**
	 * Creates a file splitter that will start reading at a given line number,
	 * and <i>only</i> read a specific number of lines. If you want to read 
	 * all remaining lines in the file, rather consider using 
	 * {@link DigicoreFileSplitter#DigicoreFileSplitter(String, String, long)}
	 * @param inputFilename
	 * @param filename
	 * @param startLine
	 * @param linesToRead
	 */
	public DigicoreFileSplitter(String inputFilename, String outputFolder, long startLine, long linesToRead) {
		this.outputFolder = outputFolder;
		this.inputFilename = inputFilename;
		this.startLine = startLine;
		this.numberOfLinesToRead = linesToRead;
		
		createVehicleFolder(outputFolder);
	}
	
	
	/**
	 * Creates a file splitter that will read all the remaining lines in the file, 
	 * starting at the given line number. If you only want to read a certain number 
	 * of lines, rather consider using 
	 * {@link DigicoreFileSplitter#DigicoreFileSplitter(String, String, long, long)} 
	 * @param inputFilename
	 * @param outputFolder
	 * @param startline
	 */
	public DigicoreFileSplitter(String inputFilename, String outputFolder, long startline){
		this(inputFilename, outputFolder, startline, Long.MAX_VALUE);
	}
	
	
	/**
	 * This class processes a single file provided by DigiCore holdings, 
	 * <code>Poslog_Research_Data.txt</code>, and split it into separate files, 
	 * one for each vehicle. The main method creates a file splitter, and splits the
	 * entire file.
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
	 * when the process started. This gives the user flexibility in terms of which line of the input 
	 * files should be processed first. An interrupted job can then be easily <i>continued</i> by
	 * just checking what the last line was that was processed in the terminated run. 
	 * 
	 * @param args The following arguments should be passed, and in the given order:
	 *<ol>
	 *	<li> input file to be split;
	 *	<li> output folder into which a folder will be <i>created</i> within which 
	 *		 separate vehicle files will be written;
	 *	<li> line number at which processing should start (starting with the first line as 2 to exclude
	 *		 the header line and start at a line containing data.  If it is started at line 1, the headers
	 *		 will also be written out extra to a separate file as if it was a vehicle);
	 *	<li> field number containing the <i>VehicleId</i>;
	 *	<li> field number containing the <i>Time</em>;
	 *	<li> field number containing the <i>Longitude</i>;
	 *	<li> field number containing the <i>Latitude</i>;
	 *	<li> field number containing the <i>Status</i>;
	 *	<li> field number containing the <i>Speed</i>;
	 *</ol>
	 *
	 * @author jwjoubert
	 */
	public static void main( String[] args){
		if(args.length != 9){
			throw new RuntimeException("Must provide 9 field arguments: filename, startLine, outputfolder and the field locations for VehId, Time, Long, Lat, Status and Speed.");
		}
		
		log.info("=================================================================");
		log.info("  Splitting the DigiCore data file into seperate vehicle files.");
		log.info("=================================================================");

		DigicoreFileSplitter dfs = new DigicoreFileSplitter(args[0], args[1], Long.parseLong(args[2]));
		
		dfs.split(Integer.parseInt(args[3]),
				  Integer.parseInt(args[4]),
				  Integer.parseInt(args[5]),
				  Integer.parseInt(args[6]),
				  Integer.parseInt(args[7]),
				  Integer.parseInt(args[8]));  
		
		GregorianCalendar first = new GregorianCalendar(TimeZone.getTimeZone("GMT+02"), Locale.ENGLISH);
		first.setTimeInMillis(dfs.getEarliestTimestamp() * 1000);
		GregorianCalendar last = new GregorianCalendar(TimeZone.getTimeZone("GMT+02"), Locale.ENGLISH);
		last.setTimeInMillis(dfs.getLatestTimestamp() * 1000);
					
		log.info("-----------------------------------------------------------------");
		log.info("   Process complete.");
		log.info("-----------------------------------------------------------------");
		log.info("   Earliest date parsed: " + dfs.calendarToString(first));
		log.info("     Latest date parsed: " + dfs.calendarToString(last));
		log.info("=================================================================");
	}


	/**
	 * The file provided by DigiCore has six comma delimited fields.
	 * 
	 * @param fieldVehId the field number containing a unique vehicle identifier 
	 * 			from the vehicle sending the GPS log entry; 
	 * @param fieldTime the field number containing a UNIX-based time stamp (in 
	 * 			seconds);
	 * @param fieldLong the field number containing the x-coordinate of the GPS 
	 * 			log (in decimal degrees);
	 * @param fieldLat the field number containing the y-coordinate of the GPS 
	 * 			log (in decimal degrees); 
	 * @param fieldStatus the field number containing an integer digit indicating 
	 * 			a predefined vehicle status. Status codes are described in <code>
	 * 			Statuses.xls</code>;
	 * @param fieldSpeed the field number containing a field reflecting the speed 
	 * 			of the vehicle when sending the log entry. This field, however, 
	 * 			proved to be quite useless.
	 */
	public void split(int fieldVehId, int fieldTime, int fieldLong, int fieldLat, int fieldStatus, int fieldSpeed) {
		long line = 0;
		Counter lineCounter = new Counter("   lines: ");
		
		BufferedReader input = null;
		BufferedWriter output = null;
		String vehicleFile = null;
		
		DateString ds = new DateString();
		try {
			File inputFolder = (new File(inputFilename)).getParentFile();
			BufferedWriter logRecords = IOUtils.getBufferedWriter(inputFolder.getAbsolutePath() + "/logRecordsRead_" + ds.toString() + ".txt");
			input = IOUtils.getBufferedReader(inputFilename);
			try{
				String inputLine = null;
				while((inputLine = input.readLine()) != null){
					if(++line >= startLine && lineCounter.getCounter() <= numberOfLinesToRead){
						String [] inputString = inputLine.split(delimiter);
						if(inputString.length == 6){
							/* Check the record's date against earliest and latest. */
							earliest = Math.min(earliest, Long.parseLong(inputString[fieldTime]));
							latest = Math.max(latest, Long.parseLong(inputString[fieldTime]));
							
							/* Open the file for the vehicle
							 */
							vehicleFile = outputFolder + "Vehicles/" + inputString[fieldVehId] + ".txt";
							output = IOUtils.getAppendingBufferedWriter(vehicleFile);

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

							logRecords.write(String.valueOf(lineCounter.getCounter()));
							logRecords.newLine();

							lineCounter.incCounter();
						} else{
							log.warn("Line " + line + " does not contain 6 entries.");
						}	

						String vehID = inputString[fieldVehId];
						while( (inputLine = input.readLine()) != null){

							// Read the next input line
							inputString = inputLine.split(delimiter);	
							lineCounter.incCounter();
							if(++line >= startLine && lineCounter.getCounter() <= numberOfLinesToRead){
								if(inputString.length == 6){
									/* Check the record's date against earliest and latest. */
									earliest = Math.min(earliest, Long.parseLong(inputString[fieldTime]));
									latest = Math.max(latest, Long.parseLong(inputString[fieldTime]));

									if ( !vehID.equalsIgnoreCase(inputString[fieldVehId])) {
										// Close the file for the current vehicle.
										output.close();

										/* Open the file for the new vehicle
										 */
										vehicleFile = outputFolder + "Vehicles/" + inputString[fieldVehId] + ".txt";
										output = IOUtils.getAppendingBufferedWriter(vehicleFile);
										if(output == null){
											log.info("Stop here");
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

									logRecords.write(String.valueOf(lineCounter.getCounter()));
									logRecords.newLine();

									vehID = inputString[fieldVehId];
								}
							}
						}
					} 
				}

			} finally{
				logRecords.close();
				output.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		lineCounter.printCounter();
	}

	/**
	 * A method to create a new "Vehicles" folder in the output directory in which the
	 * individual vehicle files will be written out to.
	 * @param outputDirectory the path/directory where the "Vehicles" folder should be created
	 */
	public void createVehicleFolder(String outputDirectory) {
		File outputFolder = new File(outputDirectory + "Vehicles/");
		if(outputFolder.exists()){
			String s = "The folder already exists! Delete " + outputFolder.getPath() + " and rerun.";
			throw new RuntimeException(s);
		} else{
			boolean checkDirectory = outputFolder.mkdirs();
			if(!checkDirectory){
				log.warn("Could not make " + outputFolder.toString() + ", or it already exists!");
			}
		}
	}
	
	public long getEarliestTimestamp(){
		return this.earliest;
	}
	
	public long getLatestTimestamp(){
		return this.latest;
	}
	
	
	/**
	 * Converts a {@link GregorianCalendar} to more readable format.
	 * @param calendar
	 * @return
	 */
	private String calendarToString(GregorianCalendar calendar){
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH);
		String monthString = null;
		switch (month) {
		case Calendar.JANUARY:
			monthString = "January";
			break;
		case Calendar.FEBRUARY:
			monthString = "February";
			break;
		case Calendar.MARCH:
			monthString = "March";
			break;
		case Calendar.APRIL:
			monthString = "April";
			break;
		case Calendar.MAY:
			monthString = "May";
			break;
		case Calendar.JUNE:
			monthString = "June";
			break;
		case Calendar.JULY:
			monthString = "July";
			break;
		case Calendar.AUGUST:
			monthString = "August";
			break;
		case Calendar.SEPTEMBER:
			monthString = "September";
			break;
		case Calendar.OCTOBER:
			monthString = "October";
			break;
		case Calendar.NOVEMBER:
			monthString = "November";
			break;
		case Calendar.DECEMBER:
			monthString = "December";
			break;
		default:
			break;
		}
		int year = calendar.get(Calendar.YEAR);
		return day + " " + monthString + " " + year;
	}
	
}
