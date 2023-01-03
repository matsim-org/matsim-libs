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
package playground.vsp.buildingEnergy.energyCalculation;

import java.util.HashMap;
import java.util.Map;




/**
 * @author droeder
 *
 */
public interface BuildingEnergyConsumptionRule{
	
	/**
	 * 
	 * @param maxSize
	 * @param currentOccupancy
	 * @return energy consumption in kWh
	 */
	public double getEnergyConsumption_kWh(double maxSize, double currentOccupancy);

	
	
	public class BuildingEnergyConsumptionRuleFactory{
		
		private Map<String, BuildingEnergyConsumptionRule> rules = new HashMap<String, BuildingEnergyConsumptionRule>();   

		public void setRule(String actType, BuildingEnergyConsumptionRule rule){
			this.rules.put(actType, rule);
		}
		
		BuildingEnergyConsumptionRule getRule(String actType){
			return rules.get(actType);
		}
	}
	
	
	
	
	
}

