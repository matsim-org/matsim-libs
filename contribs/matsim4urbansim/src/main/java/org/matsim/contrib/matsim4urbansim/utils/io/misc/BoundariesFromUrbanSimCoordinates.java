package org.matsim.contrib.matsim4urbansim.utils.io.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.io.HeaderParser;
import org.matsim.core.utils.io.IOUtils;


public class BoundariesFromUrbanSimCoordinates {
	// Logger
	private static final Logger log = Logger.getLogger(BoundariesFromUrbanSimCoordinates.class);
	
	private static final String inputFile = "/Users/thomas/Development/opus_home/matsim4opus/tmp/zone__dataset_table__exported_indicators__2001.tab";
	
	private static double xmin = Double.MAX_VALUE;
	private static double ymin = Double.MAX_VALUE;
	private static double xmax = -Double.MAX_VALUE;
	private static double ymax = -Double.MAX_VALUE;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);
		log.info( "Starting to read urbansim parcels table from " + inputFile );
		
		try{
			
			// read header of facilities table
			String line = reader.readLine();
		
			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, InternalConstants.TAB_SEPERATOR );
			final int indexXCoodinate 	= idxFromKey.get( InternalConstants.X_COORDINATE );
			final int indexYCoodinate 	= idxFromKey.get( InternalConstants.Y_COORDINATE );
			
			String[] parts;
			
			//
			while ( (line = reader.readLine()) != null ) {
				parts = line.split(InternalConstants.TAB_SEPERATOR);
				
				double x = Double.parseDouble( parts[indexXCoodinate] );
				double y = Double.parseDouble( parts[indexYCoodinate] );
				
				xmin = Math.min(xmin, x);
				ymin = Math.min(ymin, y);
				xmax = Math.max(xmax, x);
				ymax = Math.max(ymax, y);
			}
			
			log.info("xmin:" + xmin);
			log.info("ymin:" + ymin);
			log.info("xmax:" + xmax);
			log.info("ymax:" + ymax);
			
			reader.close();
			
			log.info("This took " + (System.currentTimeMillis()-startTime)/1000 + " seconds (" + (System.currentTimeMillis()-startTime)/6000 + " minutes).");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
