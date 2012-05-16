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
package playground.droeder.e.ePlans;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.DaFileReader;
import playground.droeder.e.EPostProcessor;
import playground.droeder.e.eVehicles.EVehicle;
import playground.droeder.e.eVehicles.EVehicles;

/**
 * @author droeder
 *
 */
public class EVehiclesPlanReader {

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
		Set<String[]> appointments = DaFileReader.readFileContent(vehiclePlans, "\t", true);
		EVehicle v = null;
		EVehiclePlan plan;
		List<EVehiclePlanElement> elements = null;
		Id id = null;
		Double initialSoC = null;
		EChargingAct ca;
		EDisChargingAct da;
		
		for(String[] s: appointments ){
			if(id == null){
				id = new IdImpl(EPostProcessor.IDENTIFIER + s[0]);
			}else if(!id.equals(new IdImpl(EPostProcessor.IDENTIFIER + s[0]))){
				plan = new EVehiclePlan(elements);
				v = new EVehicle(id, plan, initialSoC);
				this.vehicles.addVehicle(v);
				elements = null;
				id = new IdImpl(EPostProcessor.IDENTIFIER + s[0]);
			}
			if(elements == null){
				elements = new ArrayList<EVehiclePlanElement>();
				//dummy-activity
				elements.add(new EChargingAct(0, 0, new IdImpl("NONE"), new IdImpl("NONE"), new IdImpl("NONE")));
				initialSoC = Double.parseDouble(s[3]);
			}
			da = new EDisChargingAct(new IdImpl(s[8]), id);
			elements.add(da);
			ca = new EChargingAct(getTime(s[5]), getTime(s[6]), new IdImpl(s[7]), id, new IdImpl(s[4]));
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
	
}
