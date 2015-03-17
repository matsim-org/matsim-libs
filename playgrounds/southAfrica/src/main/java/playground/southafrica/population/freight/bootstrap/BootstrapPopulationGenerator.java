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
package playground.southafrica.population.freight.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to generate a population of commercial vehicles where the plans are
 * bootstrap sampled from observed activity chains for a given set of vehicles
 * on a given day (type).
 * 
 * @author jwjoubert
 */
public class BootstrapPopulationGenerator {
	final private static Logger LOG = Logger.getLogger(BootstrapPopulationGenerator.class);
	
	private Map<String, Integer> chainMap = new TreeMap<String, Integer>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(BootstrapPopulationGenerator.class.toString(), args);
		
		String xmlFolder = args[0];
		String vehicleIds = args[1];
		int dayType = Integer.parseInt(args[2]);
		String abnormalDaysFile = args[3];
		String outputFolder = args[4];
		
		/* Read the list of vehicle files */
		try {
			List<File> vehicles = DigicoreUtils.readDigicoreVehicleIds(vehicleIds, xmlFolder);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get the necessary vehicle files.");
		}
		
		/* Read the list of abnormal days. */
		List<Integer> abnormalDays = DigicoreUtils.readDayOfYear(abnormalDaysFile);
		
		Header.printFooter();
	}
	
	public BootstrapPopulationGenerator() {

	}
	
	/**
	 * Procedure to check if a given activity chain is, or should be considered
	 * or not. For now (JWJ, March 2015) it just checks if the start day of the
	 * activity chain is the same day type specified as argument.
	 * 
	 * @param chain
	 * @return
	 */
	public boolean useActivityChain(DigicoreChain chain, int dayType, List<Integer> abnormalDays){
		boolean result = false;
		if(dayType == chain.getChainStartDay(abnormalDays)){
			result = true;
		}
		return result;
	}

}
