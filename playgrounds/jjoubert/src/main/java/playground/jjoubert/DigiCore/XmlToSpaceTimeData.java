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

package playground.jjoubert.DigiCore;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.Header;

public class XmlToSpaceTimeData {
	private final static Logger LOG = Logger.getLogger(VehicleFilesToSpaceTimeData.class);
	private final static Double DISTANCE_FACTOR = 1.35;
	private final static String[] files = {
		"100002", "100003", "100004", "100005", "100008", "100010", "100012",
		"100013", "100015", "100016", 
		"100018", 
		"100019", "100020", "100021",
		"100023", "100024"
		};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(XmlToSpaceTimeData.class.toString(), args);
		
		for(String s : files){
			/* Read in the Digicore vehicle's xml file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(args[0] + s + ".xml.gz");
			DigicoreVehicle vehicle = dvr.getVehicle(); 
			
			/* Create the writer. */
			BufferedWriter bw = IOUtils.getBufferedWriter(args[0] + s + "_XmlChain.csv");
			try{
				for(DigicoreChain chain : vehicle.getChains()){
					double startTime = chain.getFirstMajorActivity().getEndTimeGregorianCalendar().getTimeInMillis()/1000;
					double endTime = chain.getLastMajorActivity().getStartTimeGregorianCalendar().getTimeInMillis()/1000;
					int activities = chain.getNumberOfMinorActivities();
					
					bw.write(String.format("%.0f,%.0f,%d\n", startTime, endTime, activities));
				}
			} catch (IOException e) {
				throw new RuntimeException("Cannot write to BufferedWriter.");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					throw new RuntimeException("Cannot close BufferedWriter.");
				}
			}
		}
		
		Header.printFooter();
	}

}
