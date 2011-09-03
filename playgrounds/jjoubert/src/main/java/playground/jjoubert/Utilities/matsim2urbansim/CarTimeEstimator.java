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
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class CarTimeEstimator {
	private final static Logger log = Logger.getLogger(CarTimeEstimator.class);
//	public M2UStringbuilder sb;
	private DenseDoubleMatrix2D odMatrix;
	private List<MyZone> zones;
	private Map<Id, Integer> mapZoneIdToListEntry;
	private Map<Integer, Id> mapListEntryToZoneId;
	
	
	/**
	 * Class to read zones from shapefiles, and determining the interzonal 
	 * travel-time matrix for private cars. It is required that a successful 
	 * MATSim is available since the following input data is required:
	 * <ul>
	 * 	<li> a <code>*.network.xml.gz</code> file describing the network used;
	 * 	<li> the <code>*.plans.xml.gz</code> file of the last iteration;
	 * 	<li> the <code>*.linkstats.txt</code> file of the last iteration (<b><i>Note: file must be unzipped</i></b>); and
	 * 	<li> the <code>*.events.txt.gz</code> file of the last iteration that 
	 * 		is used to build the travel time data required for the routing algorithm.
	 * </ul>
	 * 
	 * The following arguments are required:
	 * 
	 * @author jwjoubert
	 */
	public CarTimeEstimator() {
//		sb = new M2UStringbuilder(root, studyAreaName, version, percentage);
	}

	
//	/**
//	 * Implements the private car drive time estimator.
//	 * @param args a String-array containing:
//	 * <ol>
//	 * 	<li> the root folder;
//	 * 	<li> study area name. Currently allowed values are:
//	 * 		<ul>
//	 * 			<li> "eThekwini"
//	 * 		</ul> 
//	 * 	<li> version (year) of the study area to consider;
//	 * 	<li> the population sample size, e.g. "10" if a 10% sample was used;
//	 * 	<li> the final iteration number of the MATSim run used, e.g. "100" for the 100th iteration. 
//	 * 	<li> the specific hours (from link statistics file) to use, e.g. "6-7";
//	 * </ol>
//	 */
	/**
	 * Implements the private car drive time estimator. 
	 * @param args The following arguments are required, and in the following 
	 * 		  order:
	 * <ol>
	 * 	<li> <b>network</b> absolute path of the network to be used;
	 * <br><br>The following files should be from the same MATSim run that used 
	 * 	the network provided in the first argument:<br><br>
	 *  <li> <b>linkstats</b> absolute path of the linkstats file to use (should
	 *  		be associated with the network provided in the previous argument);
	 *  <li> <b>population</b> absolute path of the population (plans) file;
	 *  <li> <b>events</b> absolute path of the events file;  
	 *  <br><br>
	 *  <li> <b>shapefile</b> absolute path of the zonal shapefile for which the
	 *  		inter-zonal travel time must be estimated;
	 *  <li> <b>idField</b> the field number that must be used as zone Id: known 
	 *  		values are:
	 *  	<ul>
	 *  		<li> eThekwini: <code>eThekwini_TZ_UTM.shp</code>, field value "1";
	 *  		<li> Nelson Mandela Bay: <code>NMBM_Zone_SA-Albers.shp</code>, 
	 *  			field value "6";
	 *  		<li> Joburg: <code>Joburg_Joburg_TAZ_UTM35S.shp</code>, field value "3"
	 *    	</ul>
	 *    <li> <b>hour</b> the hour for which travel time must be estimated, for 
	 *    		example "6-7";
	 *    <li> <b>output</b> absolute path of the comma-separated file to which 
	 *    		travel time estimations are written.
	 * </ol>
	 */
	public static void main(String[] args){
		CarTimeEstimator cte = null;
		long tNow = System.currentTimeMillis();
		int numberOfArguments = 8;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		} else{
			cte = new CarTimeEstimator();
		}
		boolean empties = cte.estimateCarTime(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], true);
		
		String result = empties ? "Unsuccessful" : "Successful";
		double time = (double)(System.currentTimeMillis() - tNow)/1000.0;
		log.info("----------------------------------------------------------");
		log.info(String.format("Process complete for %s (%s)", args[1], result));
		log.info("----------------------------------------------------------");
		log.info(String.format("        Time taken: %04.2fs",time));
		log.info("           Network: " + args[0]);
		log.info("         Linkstats: " + args[1]);
		log.info("        Population: " + args[2]);
		log.info("            Events: " + args[3]);
		log.info("             Zones: " + args[4] + " (Field " + args[5] + ")");
		log.info("             Hours: " + args[6]);
		log.info(" Output written to: " + args[7]);
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
	 * @param write indicating if the output must be written to file.
	 */
	public boolean estimateCarTime(
			String networkFilename, 
			String linkstatsFilename,
			String plansFilename, 
			String eventsFilename,
			String zoneShapefile, 
			String zoneIdField, 
			String hours, 
			String outputFilename,
			boolean write){
		/*
		 * Phase 1: calculate actual travel times from MATSim output.
		 */
		// 1a. Read the transportation zones. 
		MyZoneReader r = new MyZoneReader(zoneShapefile);
		r.readZones(Integer.parseInt(zoneIdField));
		zones = r.getZoneList();
		// 1b. Create new scenario.
		Scenario s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// 1c. Read the network.
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(networkFilename);
		// 1d. Read plans file.
		MatsimPopulationReader mpr = new MatsimPopulationReader(s);
		mpr.readFile(plansFilename);
		// 1e. Process plans.
		MyPlansProcessor mpp = new MyPlansProcessor(s, zones);
		mpp.processPlans();
		DenseDoubleMatrix2D partialMatrix = mpp.getOdMatrix();

		/*
		 * Phase 2: calculate missing intra- and inter-zonal travel times.
		 */
		// 2a. Read link statistics.
		MyLinkStatsReader mlsr = new MyLinkStatsReader(linkstatsFilename);
		Map<Id,Double> linkstats = mlsr.readSingleHour(hours);
		// 2b. Prepare zone-to-zone travel time.
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(s, zones);
		mapZoneIdToListEntry = mzzr.getZoneToMatrixMap();
		mapListEntryToZoneId = mzzr.getMatrixToZoneMap();
		mzzr.prepareTravelTimeData(eventsFilename);
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
			mzzr.writeOdMatrixToCsv(outputFilename, odMatrix);
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

