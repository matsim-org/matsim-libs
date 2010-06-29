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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;

public class RunMatsim2Urbansim {
	private final static Logger log = Logger.getLogger(RunMatsim2Urbansim.class);
	private static String root; 
	private static String studyAreaName;
	private static String version;
	private static String hours;
	
	/**
	 * Class to read zones from shapefiles, and determining the interzone 
	 * travel-time matrix. The following arguments are required:
	 * 
	 * @param args a String-array containing:
	 * <ol>
	 * 	<li> the root folder;
	 * 	<li> study area name. Currently allowed values are:
	 * 		<ul>
	 * 			<li> "eThekwini"
	 * 		</ul> 
	 * 	<li> version (year) of the study area to consider;
	 * 	<li> the specific hours (from link statistics file) to use, eg. "6-7".
	 * </ol>
	 */
	public static void main(String[] args){
		int numberOfArguments = 4;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		} else{
			root = args[0];
			studyAreaName = args[1];
			version = args[2];
			hours = args[3];
		}
		M2UStringbuilder sb = new M2UStringbuilder(root, studyAreaName, version);
		
		// Read the transportation zones. 
		MyZoneReader r = new MyZoneReader(sb.getShapefile());
		r.readZones(sb.getIdField());
		Collection<MyZone> zones = r.getZones();
		
		/* 
		 * TODO Write procedure.
		 * Read the final iteration plans to calculate the inter-zonal travel
		 * times as the average of the travel times of all the agents that did
		 * travel that origin-destination zone-combination.
		 * TODO Check if only specific hours must be calculated. 
		 */
		Scenario s = new ScenarioImpl();
		Population pop = s.getPopulation();
		MatsimPopulationReader mpr = new MatsimPopulationReader(s);
		mpr.readFile(sb.getPlansFile("10"));
		Map<Id, ? extends Person> persons = pop.getPersons();
		int personCounter = 0;
		int personMultiplier = 1;
		for(Person p : persons.values()){
			List<PlanElement> pe = p.getSelectedPlan().getPlanElements();
			// TODO Alright, now handle the plans.
			
			// Report progress.
			if(++personCounter == personMultiplier){
				log.info("   persons processed: " + personCounter);
				personMultiplier *= 2;
			}
		}
		log.info("   persons processed: " + personCounter + " (Done)");
		
		// Read the network.
		Network n = s.getNetwork();
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(sb.getEmmeNetworkFilename());
		
		// Read the link statistics;
		String linkStatsFilename = root + studyAreaName + "/" + version + "/linkstats.txt.gz";
		MyLinkStatsReader mlsr = new MyLinkStatsReader(linkStatsFilename, hours);
		mlsr.readLinkStatsTravelTime();
		Map<Id,Double> tt = mlsr.getTravelTimeMap();
		
		log.info("Process complete.");
	}
	
}

