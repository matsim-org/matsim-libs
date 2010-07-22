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

public class CarTimeEstimator {
	private final static Logger log = Logger.getLogger(CarTimeEstimator.class);
	public M2UStringbuilder sb;
	private DenseDoubleMatrix2D odMatrix;
	private List<MyZone> zones;
	private Map<Id, Integer> mapZoneIdToListEntry;
	private Map<Integer, Id> mapListEntryToZoneId;
	
	
	/**
	 * Class to read zones from shapefiles, and determining the interzonal 
	 * travel-time matrix for private cars. It is required that a successful 
	 * MATSim is available since the following input data is required:
	 * <ul>
	 * 	<li> an <code>output_network.xml.gz</code> file describing the network used;
	 * 	<li> an <code>output_plans.xml.gz</code> file describing the agent plans used;
	 * 	<li> the <code>*.linkstats.txt</code> file of the last iteration; and
	 * 	<li> the <code>*.events.txt.gz</code> file of the last iteration that 
	 * 		is used to build the travel time data required for the routing algorithm.
	 * </ul>
	 * 
	 * The following arguments are required:
	 * 
	 * @author jwjoubert
	 */
	public CarTimeEstimator(String root, String studyAreaName, String version, String percentage) {
		sb = new M2UStringbuilder(root, studyAreaName, version, percentage);
	}

	
	/**
	 * Implements the private car drive time estimator.
	 * @param args a String-array containing:
	 * <ol>
	 * 	<li> the root folder;
	 * 	<li> study area name. Currently allowed values are:
	 * 		<ul>
	 * 			<li> "eThekwini"
	 * 		</ul> 
	 * 	<li> version (year) of the study area to consider;
	 * 	<li> the population sample size, e.g. "10" if a 10% sample was used;
	 * 	<li> the final iteration number of the MATSim run used, e.g. "100" for the 100th iteration. 
	 * 	<li> the specific hours (from link statistics file) to use, e.g. "6-7";
	 * </ol>
	 */
	public static void main(String[] args){
		CarTimeEstimator cte = null;
		long tNow = System.currentTimeMillis();
		int numberOfArguments = 6;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		} else{
			cte = new CarTimeEstimator(args[0], args[1], args[2], args[3]);
		}
		boolean empties = cte.estimateCarTime(args[4], args[5], true);
		
		String result = empties ? "Unsuccessful" : "Successful";
		double time = (double)(System.currentTimeMillis() - tNow)/1000.0;
		log.info("----------------------------------------------------------");
		log.info(String.format("Process complete for %s (%s)", args[1], result));
		log.info("----------------------------------------------------------");
		log.info(String.format("            Time taken: %04.2fs",time));
		log.info("                  Root: " + args[0]);
		log.info("               Version: " + args[2]);
		log.info("           Sample size: " + args[3] + "%");
		log.info(" MATSim iteration used: " + args[4]);
		log.info("                 Hours: " + args[5]);
		log.info("==========================================================");
	}
	
	
	/**
	 * Estimates the private car travel time for all origin-destination zone
	 * combinations. This is achieved in two phases:
	 * <ol>
	 * 		<li> calculate actual travel times from MATSim output;
	 * 		<li> estimate the travel times for missing origin-destination pairs.
	 * 			 This is handled separately for:
	 * 		<ul>
	 * 			<li> diagonals: estimate the intra-zonal travel time as the 
	 * 				 average travel time of all links associated with that zone;
	 * 			<li> non-diagonals: estimate the inter-zone travel time as the 
	 * 				 shortest-path travel time between the two zone's centroids.
	 * </ol>
	 * The detailed step-by-step process is shown in the code through comments.	  
	 * @param iteration the specific MATSim iteration that must be used; 
	 * @param hours the specific hours that must be used from the 
	 * 		  <code>linkstats</code> file;
	 * @param write indicating if then output must be written to file.
	 */
	public boolean estimateCarTime(String iteration, String hours, boolean write){
		/*
		 * Phase 1: calculate actual travel times from MATSim output.
		 */
		// 1a. Read the transportation zones. 
		MyZoneReader r = new MyZoneReader(sb.getTransportZoneShapefile());
		r.readZones(sb.getIdField());
		zones = r.getZoneList();
		// 1b. Create new scenario.
		Scenario s = new ScenarioImpl();
		// 1c. Read the network.
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(sb.getEmmeNetworkFilename());
		// 1d. Read plans file.
		MatsimPopulationReader mpr = new MatsimPopulationReader(s);
		mpr.readFile(sb.getPlansFile());
		// 1e. Process plans.
		MyPlansProcessor mpp = new MyPlansProcessor(s, zones);
		mpp.processPlans();
		DenseDoubleMatrix2D partialMatrix = mpp.getOdMatrix();

		/*
		 * Phase 2: calculate missing intra- and inter-zonal travel times.
		 */
		// 2a. Read link statistics.
		MyLinkStatsReader mlsr = new MyLinkStatsReader(sb.getIterationLinkstatsFile(iteration));
		Map<Id,Double> linkstats = mlsr.readSingleHour(hours);
		// 2b. Prepare zone-to-zone travel time.
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(s, zones);
		mapZoneIdToListEntry = mzzr.getZoneToMatrixMap();
		mapListEntryToZoneId = mzzr.getMatrixToZoneMap();
		mzzr.prepareTravelTimeData(sb.getIterationEventsFile(iteration));
		// 2c. Do zone-to-zone calculations. 
		boolean empties = mzzr.processZones(partialMatrix, linkstats);

		if(empties){
			log.warn("OD Matrix contains empty values");
		} else{
			odMatrix = mzzr.getOdMatrix();
		}
		
		/*
		 * Write the odMatrix to file. Although there is a method to write to
		 * a DBF file, it is preferred to write to a comma-separated (CSV) file.
		 * Also, the CSV-file is much smaller.
		 */
		if(write){
//			mzzr.writeOdMatrixToDbf(sb.getDbfOutputFile(), odMatrix);
			mzzr.writeOdMatrixToCsv(sb.getCsvOutputFile(), odMatrix);
		}
		
		return empties;	
	}
	
	public DenseDoubleMatrix2D getOdMatrix(){
		return odMatrix;
	}
	
	public List<MyZone> getZones(){
		return zones;
	}
	
	public Map<Id, Integer> getZoneToMatrixMap() {
		return mapZoneIdToListEntry;
	}

	public Map<Integer, Id> getMatrixToZoneMap() {
		return mapListEntryToZoneId;
	}

	
}

