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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.urbansim.ersa.ERSAControlerListener;

/**
 * @author thomas
 *
 */
public class Benchmark {
	
	// logger
	private static final Logger log = Logger.getLogger(Benchmark.class);
	// writes the results to the desired location
	private BufferedWriter benchmarkWriter = null;
	
	/**
	 * constructor
	 * 
	 * @param resultPath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Benchmark(String resultPath) throws FileNotFoundException, IOException{
		benchmarkWriter = IOUtils.getBufferedWriter( resultPath );
	}
	
	public void addItem(){
		
	}
	
	private class BenchmarkItem{
		
		private String filePath = null;
		private double duration = 0.;
		private double fileSize = 0.;
		
		/**
		 * constructor
		 * 
		 * @param filePath
		 * @param fileSize
		 * @param duration
		 */
		public BenchmarkItem(String filePath, double fileSize, double duration){
			this.filePath = filePath;
			this.fileSize = fileSize;
			this.duration = duration;
		}
		
		protected String getFilePath(){
			return filePath;
		}
		protected double getFileSize(){
			return fileSize;
		}
		protected double getDuration(){
			return duration;
		}
		}

}

