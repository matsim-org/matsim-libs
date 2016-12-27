/* *********************************************************************** *
 * project: org.matsim.*
 * ChainCounter.java
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
package playground.southafrica.freight.digicore.analysis.chain;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.Header;

/**
 * Class to read a container of {@link DigicoreVehicles} and count the number
 * of unique vehicles, and the total number of activity chains. The original
 * reason for this was to see if the number of chains differ substantially when
 * looking at vehicles over a 12 month-period on a month to month basis, as
 * opposed to a container of vehicles covering the entire 12 month-period.
 * 
 * @author jwjoubert
 */
public class ChainCounter {
	final private static Logger LOG = Logger.getLogger(ChainCounter.class);
	
	/** Hide constructor */
	private ChainCounter() {
	}
	
	public static void main(String[] args){
		Header.printHeader(ChainCounter.class.toString(), args);
		String path = args[0];
		path += path.endsWith("/") ? "" : "/";
		checkMonthByMonth(path);
		checkWholeYear(path);
		
		Header.printFooter();
	}
	
	private static void checkMonthByMonth(String path){
		int chains = 0;
		DigicoreVehicles vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201306.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201307.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201308.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201309.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201310.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201311.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201312.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201401.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201402.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201403.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201404.xml.gz");
		chains += countChains(vehicles);
		
		vehicles = null; vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201405.xml.gz");
		chains += countChains(vehicles);
		LOG.info("Total number of chains on month-by-month basis: " + chains);
	}
	
	private static void checkWholeYear(String path){
		int chains = 0;
		DigicoreVehicles vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201306_201405.xml.gz");
		chains += countChains(vehicles);
		LOG.info("Total number of chains on yearly basis: " + chains);
	}
	
	private static int countChains(DigicoreVehicles vehicles){
		int total = 0;
		Counter counter = new Counter("  vehicles # ");
		for(DigicoreVehicle vehicle : vehicles.getVehicles().values()){
			total += vehicle.getChains().size();
			counter.incCounter();
		}
		counter.printCounter();
		return total;
	}
}
