/* *********************************************************************** *
 * project: org.matsim.*
 * Benchmark.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.tnicolai.urbansim.utils.helperObjects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class Benchmark {
	
	// logger
	private static final Logger log = Logger.getLogger(Benchmark.class);
	// writes the results to the desired location
	private BufferedWriter benchmarkWriter = null;
	private String output = null;
	// counter in order to separate measurement tasks
	private static int measureID = 0;
	
	private ArrayList<MeasurementObject> measurements = null;
	
	public Benchmark(){
		log.info("Initializing ...");
		measurements = new ArrayList<MeasurementObject>();
	}
	
	public int addMeasure(String name){
		
		long startTime = System.currentTimeMillis();
		
		log.info("Added new measurement item (id=" + measureID + ").");
		MeasurementObject mo = new MeasurementObject(name, startTime, measureID);
		measurements.add(mo);
		measureID++;
		
		return measureID-1;
	}
	
	public int addMeasure(String name, String filePath, boolean readingFile){
		
		long startTime = System.currentTimeMillis();
		
		log.info("Added new measurement item (id=" + measureID + ").");
		MeasurementObject mo = new MeasurementObject(name, startTime, new File(filePath), readingFile, measureID);
		measurements.add(mo);
		measureID++;
		
		return measureID-1;
	}
	
	public void stoppMeasurement(int id){
		
		long endTime = System.currentTimeMillis();
		
		log.info("Stopping measurement (id=" + measureID + ").");
		if(id < measurements.size())
			measurements.get( id ).stopMeasurement( endTime );
	}
	
	public void dumpResults(String output){
		
		try {
			this.output = output;
			benchmarkWriter = IOUtils.getAppendingBufferedWriter( output );
		} catch (UncheckedIOException e) {
			e.printStackTrace();
			benchmarkWriter = null;
		}
		
		log.info("Dumping measurement results to: " + output );
		if(measurements != null && measurements.size() > 0 && benchmarkWriter != null){
			
			Iterator<MeasurementObject> it = measurements.iterator();
			
			// write header
			try {
				benchmarkWriter.write("Name\t Duration in ms\t Reading File\t Writing File\t File Size in bytes\t File Size in megabytes\t");
				benchmarkWriter.newLine();
				
				while(it.hasNext()){
					
					MeasurementObject mo = it.next();
					
					if(mo.getFile() == null)
						benchmarkWriter.write( mo.getName() + Constants.TAB + String.valueOf(mo.getDuration()) + Constants.TAB + "-" + Constants.TAB + "-" + Constants.TAB + "-" + Constants.TAB + "-");
					else
						benchmarkWriter.write( mo.getName() + Constants.TAB + String.valueOf(mo.getDuration()) + Constants.TAB + (mo.isReading()?mo.getFile().getCanonicalPath():"-") + Constants.TAB + (mo.isReading()?"-":mo.getFile().getCanonicalPath()) + Constants.TAB + String.valueOf(mo.getFileSize()) + Constants.TAB + String.valueOf(mo.getFileSize()/(1024*1024)) );
					benchmarkWriter.newLine();
				}
				benchmarkWriter.flush();
				benchmarkWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.info("Done with dumping measurement results.");
	}
	
	public double getDurationInMilliSeconds(int id){
		if(measurements != null && measurements.size() > id)
			return measurements.get(id).getDuration();
		return -1.;
	}
	public double getDurationInSeconds(int id){
		double duration = getDurationInMilliSeconds(id);
		if(duration < 0.)
			return -1.;
		return duration/1000.;
	}
	
	private class MeasurementObject {
		
		private String name;
		private long startTime, endtime, duration;	// in milliseconds
		private File file;
		private long fileSize;					// in bytes
		private boolean readingFile;				// false if a file is written ...
		private int id;
		
		public MeasurementObject(String name, long startTime, int id){
			this.name = name;
			this.startTime = startTime;
			this.file = null;
			this.fileSize = -1;
			this.id = id;
		}
		
		public MeasurementObject(String name, long startTime, File file, boolean readingFile, int id){
			this.name = name;
			this.startTime = startTime;
			this.file = file;
			this.readingFile = readingFile;
			this.id = id;
		}
		
		public void stopMeasurement(long endTime){
			this.endtime = endTime;
			this.duration = this.endtime - this.startTime;
			

			if(file != null && file.exists()){
				this.fileSize = file.length();
			}
		}
		
		// getter methods
		public long getDuration(){
			return this.duration;
		}
		public String getName(){
			return this.name;
		}
		public File getFile(){
			return this.file;
		}
		public long getFileSize(){
			return this.fileSize;
		}
		public boolean isReading(){
			return this.readingFile;
		}
		public int getID(){
			return this.id;
		}
	}
	
	
//	// testing Benchmark class ...
//	public static void main(String args[]){
//		
//		Benchmark bm = new Benchmark("/Users/thomas/Development/opus_home/opus_matsim/tmp/testmeasurement.txt");
//		
//		int dmid1 = bm.addMeasure("DummyMeasure1");
//		
//		int dmid2 = bm.addMeasure("FileMeasure1", "/Users/thomas/Development/MINworkspace.zip", true);
//		
//		try{
//			BufferedReader br = IOUtils.getBufferedReader("/Users/thomas/Development/MINworkspace.zip");
//			
//			int dmid3 = bm.addMeasure("FileMeasure2", "/Users/thomas/Development/MINworkspace2.zip", false);
//			
//			BufferedWriter bw = IOUtils.getBufferedWriter("/Users/thomas/Development/MINworkspace2.zip");
//			
//			String s = null;
//			while( (s=br.readLine()) != null){
//				bw.write(s);
//			}
//			bw.flush();
//			bw.close();
//			br.close();
//			
//			bm.stoppMeasurement(dmid3);
//			bm.stoppMeasurement(dmid2);
//			bm.stoppMeasurement(dmid1);
//			
//			bm.dumpResults();
//			
//		} catch(Exception e){
//			e.printStackTrace();
//		}
//
//
//		bm.stoppMeasurement(dmid2);
//		bm.stoppMeasurement(dmid1);
//			
//		bm.dumpResults();		
//	}
}

