/* *********************************************************************** *
 * project: org.matsim.*
 * RunMatsim2Urbansim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class RunMatsim2Urbansim {
	private final static Logger log = Logger.getLogger(RunMatsim2Urbansim.class);
	private static String root; 
	private static String studyAreaName;
	private static String version;
	private static String hours;
	private static String percentage;
	private static String iteration;
	
	/**
	 * Class to read zones from shapefiles, and determining the interzonal 
	 * travel-time matrix for private cars. It is required that a successful 
	 * MATSim is available since the following input data is required:
	 * <ul>
	 * 	<li> a <code>output_network.xml.gz</code> file describing the network used;
	 * 	<li> a <code>output_plans.xml.gz</code> file describing the agent plans used;
	 * 	<li> the <code>*.linkstats.txt</code> file of the last iteration; and
	 * 	<li> the <code>*.events.txt.gz</code> file of the last iteration that 
	 * 		is used to build the travel time data required for the routing algorithm.
	 * </ul>
	 * 
	 * The following arguments are required:
	 * 
	 * @param args a String-array containing:
	 * <ol>
	 * 	<li> the root folder;
	 * 	<li> study area name. Currently allowed values are:
	 * 		<ul>
	 * 			<li> "eThekwini"
	 * 		</ul> 
	 * 	<li> version (year) of the study area to consider;
	 * 	<li> the specific hours (from link statistics file) to use, e.g. "6-7";
	 * 	<li> the population sample size, e.g. "10" if a 10% sample was used;
	 * 	<li> the final iteration number of the MATSim run used, e.g. "100" for the 100th iteration. 
	 * </ol>
	 * @author jwjoubert
	 */
	public static void main(String[] args){
		long tNow = System.currentTimeMillis();
		int numberOfArguments = 6;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		} else{
			root = args[0];
			studyAreaName = args[1];
			version = args[2];
			hours = args[3];
			percentage = args[4];
			iteration = args[5];			
		}
		M2UStringbuilder sb = new M2UStringbuilder(root, studyAreaName, version, percentage);
		/*===================================================================== 
		 * Read the final iteration plans to calculate the inter-zonal travel
		 * times as the average of the travel times of all the agents that did
		 * travel that origin-destination zone-combination. The process must 
		 * be executed in the following sequence:
		 * 	1. Read zones;
		 * 	2. generate a new scenario;
		 * 	3. read network into scenario;
		 * 	4. read plans into scenario;
		 * 	5. run process plans using MyPlansProcessor;
		 * 	6. get OD matrix from (5);
		 * 	7. read link statistics from file (for specific hour);
		 * 	8. create MyZoneToZoneRouter and prepare travel time (passing 
		 * 	   events file);
		 *  9. process zones;
		 *  10. write output to file.
		 *---------------------------------------------------------------------    
		 */
		
		// 1. Read the transportation zones. 
		MyZoneReader r = new MyZoneReader(sb.getTransportZoneShapefile());
		r.readZones(sb.getIdField());
		List<MyZone> zones = r.getZones();
		// 2. Create new scenario.
		Scenario s = new ScenarioImpl();
		// 3. Read the network.
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(sb.getEmmeNetworkFilename());
		// 4. Read plans file.
		MatsimPopulationReader mpr = new MatsimPopulationReader(s);
		mpr.readFile(sb.getPlansFile());
		// 5. Process plans.
		MyPlansProcessor mpp = new MyPlansProcessor(s, zones);
		mpp.processPlans();
		// 6. Get OD matrix.
		DenseDoubleMatrix2D matrix = mpp.getOdMatrix();
		// 7. Read link statistics.
		MyLinkStatsReader mlsr = new MyLinkStatsReader(sb.getIterationLinkstatsFile(iteration));
		Map<Id,Double> linkstats = mlsr.readSingleHour(hours);
		// 8. Prepare zone-to-zone travel time.
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(s, zones);
		mzzr.prepareTravelTimeData(sb.getIterationEventsFile(iteration));
		// 9. Do zone-to-zone calculations. 
		boolean empties = mzzr.processZones(matrix, linkstats);
		// 10. Write output to file.
		mzzr.writeOdMatrixToDbf(sb.getDbfOutputFile(), mzzr.getOdMatrix());
		
		String result = empties ? "Unsucessful" : "Sucessful";
		double time = (double)(System.currentTimeMillis() - tNow)/1000.0;
		log.info("-----------------------------------------------------");
		log.info(String.format("Process complete for %s (%s)", studyAreaName, result));
		log.info("-----------------------------------------------------");
		log.info(String.format("            Time taken: %04.2fs",time));
		log.info("                  Root: " + root);
		log.info("               Version: " + version);
		log.info("                 Hours: " + hours);
		log.info("           Sample size: " + percentage);
		log.info(" MATSim iteration used: " + iteration);
		log.info("=====================================================");
	}
	
}

