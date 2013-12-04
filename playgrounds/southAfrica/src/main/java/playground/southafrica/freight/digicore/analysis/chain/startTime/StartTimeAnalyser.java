/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package playground.southafrica.freight.digicore.analysis.chain.startTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to analyse the start time of {@link DigicoreChain}s as a function of
 * some of the observed variables, such as number of activities per chain and
 * the estimated chain distance.
 *  
 * @author jwjoubert
 */
public class StartTimeAnalyser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(StartTimeAnalyser.class.toString(), args);
		
		String xmlFolder = args[0];
		String outputFile = args[1];
		
		List<File> vehicleFiles = FileUtils.sampleFiles(new File(xmlFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		Counter counter = new Counter("   vehicles # ");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("startTime,numberOfActivites,distance,duration");
			bw.newLine();
			

			for(File f : vehicleFiles){
				DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
				dvr.parse(f.getAbsolutePath());
				DigicoreVehicle v = dvr.getVehicle();
				
				for(DigicoreChain chain : v.getChains()){
					bw.write( String.valueOf( getStartTimeInSeconds( chain.getFirstMajorActivity().getEndTimeGregorianCalendar() ) ) );
					bw.write(",");
					bw.write( String.valueOf( chain.getAllActivities().size() ) );
					bw.write(",");
					bw.write( String.format("%.0f", chain.getDistance() ) );
					bw.write(",");
					bw.write(String.format("%.0f", chain.getLastMajorActivity().getStartTime() - chain.getFirstMajorActivity().getEndTime() ) );
					bw.newLine();
				}
				counter.incCounter();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}
	
	private static int getStartTimeInSeconds(GregorianCalendar cal){
		int seconds = 0;
		
		seconds += cal.get(Calendar.HOUR_OF_DAY)*3600;
		seconds += cal.get(Calendar.MINUTE)*60;
		seconds += cal.get(Calendar.SECOND);
		
		return seconds;
	}

}
