/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdVehicleAssignmentReader.java
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

package playground.christoph.evacuation.vehicles;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;

public class HouseholdVehicleAssignmentReader {
	
	private static final Logger log = Logger.getLogger(HouseholdVehicleAssignmentReader.class);
	
	private final Charset charset = Charset.forName("UTF-8");
	private final String separator = "\t";
	
	private final Scenario scenario;
	
	private final Map<Id<Household>, HouseholdVehiclesInfo> map;
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new HouseholdVehicleAssignmentReader(scenario).parseFile("../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_ZH.txt");
	}
	
	public HouseholdVehicleAssignmentReader(Scenario scenario) {
		this.scenario = scenario;
		this.map = new HashMap<>();
	}
	
	public Map<Id<Household>, HouseholdVehiclesInfo> getAssignedVehicles() {
		return Collections.unmodifiableMap(this.map);
	}
	
	/*
	 * For cross-boarder households no vehicles are estimated.
	 * Therefore we assign them manually.
	 */
	public void createVehiclesForCrossboarderHouseholds() {
		Counter counter = new Counter("Handled cross-boarder households ");
		
		for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
			if (!map.containsKey(household.getId())) {

				HouseholdVehiclesInfo info = new HouseholdVehiclesInfo();

		    	int vehicleCount = household.getMemberIds().size();
		    	if (vehicleCount > 0) info.setFirstVehicle("U_00");
		    	if (vehicleCount > 1) info.setSecondVehicle("U_00");
		    	if (vehicleCount > 2) info.setThirdVehicle("U_00");
		    	
		    	map.put(household.getId(), info);
				counter.incCounter();
			}
		}
		log.info("Total handled cross-boarder households " + counter.getCounter());
	}
	
	public void parseFile(String file) {	
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
	    
		Counter lineCounter = new Counter("Parsed lines from file " + file + " ");

	    try {
			fis = new FileInputStream(file);
		    isr = new InputStreamReader(fis, charset);
		    br = new BufferedReader(isr);
		    
		    // skip Header
		    br.readLine();
		    
		    String line;
		    while((line = br.readLine()) != null) { 
		    	String[] cols = line.split(separator);
		    	
		    	Id<Household> id = Id.create(cols[1], Household.class);
		    	
		    	HouseholdVehiclesInfo info = new HouseholdVehiclesInfo();
		    	
		    	int vehicleCount = Integer.valueOf(cols[2]);
		    	if (vehicleCount > 0) info.setFirstVehicle(cols[3]);
		    	if (vehicleCount > 1) info.setSecondVehicle(cols[4]);
		    	if (vehicleCount > 2) info.setThirdVehicle(cols[5]);
		    	
		    	map.put(id, info);
		    	lineCounter.incCounter();
		    }
		    br.close();
		    isr.close();
		    fis.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}			

	    log.info("Total parsed lines: " + lineCounter.getCounter());
	}
}
