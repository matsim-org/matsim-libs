/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.stopZoneOccupancyAnalysis;

import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CadytsSchedule {
	
	private static TransitScheduleImpl cadytsSchedule= null;
	
	
protected static TransitScheduleImpl getCadytsSchedule (final TransitSchedule schedule){
	
		if(cadytsSchedule ==null){
			//create an alternative scenario layer for cadyts
			Config cadytsConfig = ConfigUtils.createConfig();
			cadytsConfig.transit().setUseTransit(true);
			cadytsConfig.scenario().setUseVehicles(true);
			Scenario cadytsScn = ScenarioUtils.createScenario(cadytsConfig);
			cadytsSchedule = (TransitScheduleImpl) cadytsScn.getTransitSchedule();
			TransitScheduleFactory sBuilder = cadytsSchedule.getFactory();
			
			for (Entry<Id<TransitStopFacility>, TransitStopFacility> entry:   schedule.getFacilities().entrySet()  ){
				Id<TransitStopFacility> cadytsId = FacilityUtils.convertFacilitytoZoneId(entry.getKey());
				if(!cadytsSchedule.getFacilities().keySet().contains(cadytsId)){
					TransitStopFacility cadytsStop = sBuilder.createTransitStopFacility(cadytsId, entry.getValue().getCoord() , false);
					cadytsSchedule.addStopFacility(cadytsStop);	
				}
			}
		}
		
		return cadytsSchedule;
	}
	
}
