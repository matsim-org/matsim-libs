/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFilter.java
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
package playground.johannes.socialnetworks.utils;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author illenberger
 *
 */
public class FacilityFilter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities3.xml");
		
		Set<Id> remove = new HashSet<Id>();
		ActivityFacilities facilities = scenario.getActivityFacilities();
		TObjectIntHashMap<String> hist = new TObjectIntHashMap<String>();
		for(Entry<Id, ? extends ActivityFacility> entry : facilities.getFacilities().entrySet()) {
			ActivityFacilityImpl facility = (ActivityFacilityImpl) entry.getValue();
			
			String desc = facility.getDesc();
			String[] tokens = desc.split("\\)");
			for(String token : tokens) {
				token = token.replace("(", "");
				hist.adjustOrPutValue(token, 1, 1);
			}
			
			if(desc.contains("gastro")) {
				replaceType("gastro", facility);
			} else if(desc.contains("culture")) {
				replaceType("culture", facility);
			} else if(desc.contains("sports")) {
				replaceType("sports", facility);
			}
//			if(!facility.getDesc().contains("leisure")) {
//				remove.add(entry.getKey());
//			}
		}
		
		TObjectIntIterator<String> it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			System.out.println(String.format("%1$s = %2$s", it.key(), it.value()));
		}
//		for(Id id : remove) {
//			facilities.getFacilities().remove(id);
//		}
		
		FacilitiesWriter writer = new FacilitiesWriter((ActivityFacilitiesImpl) facilities);
		writer.write("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities4.xml");
	}
	
	private static void replaceType(String type, ActivityFacilityImpl facility) {
		String remove = null;
		for(Entry<String, ActivityOption> entry : facility.getActivityOptions().entrySet()) {
			ActivityOption option = entry.getValue();
			if(option.getType().equals("leisure")) {
//				((ActivityOptionImpl)option).setType(type);
			} else {
				remove = entry.getKey();
			}
		}
		
		if(remove != null)
			facility.getActivityOptions().remove(remove);
		
	}

}
