/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.energy.ePlans;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.energy.EPostProcessor;
import playground.vsp.energy.eVehicles.EVehicle;
import playground.vsp.energy.eVehicles.EVehicles;
import playground.vsp.energy.energy.ChargingProfile;
import playground.vsp.energy.energy.DisChargingProfile;
import playground.vsp.energy.poi.Poi;

/**
 * @author droeder
 *
 */
public class EVehiclesPlanReader {
	
	private static final Logger log = Logger.getLogger(EVehiclesPlanReader.class);

	private EVehicles vehicles;

	/**
	 * @param vehicles
	 */
	public EVehiclesPlanReader(EVehicles vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * @param vehiclePlans
	 */
	public void read(String vehiclePlans) {
		Set<String[]> appointments = readFileContent(vehiclePlans, "\t", true);
		EVehicle v = null;
		EVehiclePlan plan;
		List<EVehiclePlanElement> elements = null;
		Id<Person> id = null;
		Double initialSoC = null;
		EChargingAct ca;
		EDisChargingAct da;
		
		for(String[] s: appointments ){
			if(id == null){
				id = Id.create(EPostProcessor.IDENTIFIER + s[0], Person.class);
			}else if(!id.equals(Id.create(EPostProcessor.IDENTIFIER + s[0], Person.class))){
				plan = new EVehiclePlan(elements);
				v = new EVehicle(id, plan, initialSoC);
				this.vehicles.addVehicle(v);
				elements = null;
				id = Id.create(EPostProcessor.IDENTIFIER + s[0], Person.class);
			}
			if(elements == null){
				elements = new ArrayList<EVehiclePlanElement>();
				//dummy-activity
				elements.add(new EChargingAct(0, 0, Id.create("NONE", ChargingProfile.class), Id.create("NONE", Person.class), Id.create("NONE", Poi.class)));
				initialSoC = Double.parseDouble(s[3]);
			}
			da = new EDisChargingAct(Id.create(s[8], DisChargingProfile.class), id);
			elements.add(da);
			ca = new EChargingAct(getTime(s[5]), getTime(s[6]), Id.create(s[7], ChargingProfile.class), id, Id.create(s[4], Poi.class));
			elements.add(ca);
		}
		if(!(elements == null)){
			plan = new EVehiclePlan(elements);
			v = new EVehicle(id, plan, initialSoC);
			this.vehicles.addVehicle(v);
		}
	}
	
	private Double getTime(String s){
		if(s == "NULL") return 0.;
		if(s.length() != 8 ) return 0.;
		Double time = null;
		String[] t = s.split(":");
		time = Double.parseDouble(t[0]) * 3600 + Double.parseDouble(t[1]) * 60 + Double.parseDouble(t[2]);   
		return time;
	}
	
	private static Set<String[]> readFileContent(String inFile, String splitByExpr, boolean hasHeader){
		
		boolean first = hasHeader;
		Set<String[]> lines = new LinkedHashSet<String[]>();
		
		String line;
		try {
			log.info("start reading content of " + inFile);
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split(splitByExpr);
					if(first == true){
						first = false;
					}else{
						lines.add(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			log.info("finished...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
}
