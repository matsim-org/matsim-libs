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

package playground.anhorni.locationchoice.analysis.facilities;

import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class CapacityCalculator {
	
	private String[] NOGA_shop = {
			"B015211A",
			"B015211B",	
			"B015211C",	
			"B015211D",	
			"B015211E",
			"B015212A",	
			"B015212B",	
			"B015221A",	
			"B015222A",	
			"B015223A",	
			"B015224A",	
			"B015225A",	
			"B015226A",	
			"B015227A",	
			"B015227B",	
			"B015231A",	
			"B015232A",	
			"B015233A",	
			"B015233B",	
			"B015241A",	
			"B015242A",	
			"B015242B",	
			"B015242C",	
			"B015242D",	
			"B015242E",	
			"B015243A",	
			"B015243B",	
			"B015244A",	
			"B015244B",	
			"B015244C",	
			"B015245A",	
			"B015245B",	
			"B015245C",	
			"B015245D",	
			"B015245E",	
			"B015246A",	
			"B015246B",	
			"B015247A",	
			"B015247B",	
			"B015247C",	
			"B015248A",	
			"B015248B",	
			"B015248C",	
			"B015248D",	
			"B015248E",	
			"B015248F",	
			"B015248G",	
			"B015248H",	
			"B015248I",	
			"B015248J",	
			"B015248K",	
			"B015248L",	
			"B015248M",	
			"B015248N",	
			"B015248O",	
			"B015248P",	
			"B015250A",	
			"B015250B",	
			"B015261A",	
			"B015262A",	
			"B015263A",	
			"B015271A",	
			"B015272A",	
			"B015273A",	
			"B015274A"
	};
	
	private String[] NOGA_leisure = {
			"B015511A",	
			"B015512A",	
			"B015521A",	
			"B015522A",	
			"B015523A",	
			"B015523B",	
			"B015523C",	
			"B015530A",	
			"B015540A",	
			"B015551A",	
			"B015552A",	
			"B019211A",	
			"B019212A",	
			"B019213A",	
			"B019220A",	
			"B019220B",	
			"B019231A",	
			"B019231B",	
			"B019231C",	
			"B019231D",	
			"B019232A",	
			"B019232B",	
			"B019233A",	
			"B019234A",	
			"B019234B",	
			"B019234C",	
			"B019234D",	
			"B019240A",	
			"B019240B",	
			"B019251A",	
			"B019252A",	
			"B019253A",	
			"B019261A",	
			"B019262A",	
			"B019262B"
	};
	
	private String[] NOGA_Grocery = {
			"B015211A",
			"B015211B",
			"B015211C",
			"B015211D",
			"B015211E",
			"B015212A",
			"B015221A",
			"B015222A",
			"B015223A",	
			"B015224A",	
			"B015225A",
			"B015227A",
			"B015227B"
	};
	
	private TreeMap<String, CapacityPerNOGAType> nogaShopFacilities = new TreeMap<String, CapacityPerNOGAType>();
	private TreeMap<String, CapacityPerNOGAType> nogaGroceryShopFacilities = new TreeMap<String, CapacityPerNOGAType>();
	private TreeMap<String, CapacityPerNOGAType> nogaLeisureFacilities = new TreeMap<String, CapacityPerNOGAType>();
	
	
	public void calcCapacities(ActivityFacilities facilities) {
		this.calculateCapacitiesPerType(facilities, nogaShopFacilities, NOGA_shop, "shop");
		this.calculateCapacitiesPerType(facilities, nogaGroceryShopFacilities, NOGA_Grocery, "shop");
		this.calculateCapacitiesPerType(facilities, nogaLeisureFacilities, NOGA_leisure, "leisure");
	}
	
	
	private void calculateCapacitiesPerType(ActivityFacilities facilities, TreeMap<String, CapacityPerNOGAType> nogaFacilities,
			String[] nogaTypes, String type) {
		Iterator<? extends ActivityFacility> facility_it = facilities.getFacilities().values().iterator();
		while (facility_it.hasNext()) {
			ActivityFacility facility = facility_it.next();	
			
			for (int i = 0; i < nogaTypes.length; i++) {						
				if (facility.getActivityOptions().get(nogaTypes[i]) != null) {
					Iterator<? extends ActivityOption> options_it = facility.getActivityOptions().values().iterator();
					while (options_it.hasNext()) {
						ActivityOption actOpt = options_it.next();
						
						// if there is a shopping or leisure act in the same buidling
						if (actOpt.getType().startsWith(type)) {
							if (!nogaFacilities.containsKey(nogaTypes[i])) {
								nogaFacilities.put(nogaTypes[i], new CapacityPerNOGAType(nogaTypes[i]));
							}
							nogaFacilities.get(nogaTypes[i]).addCapacity(actOpt.getCapacity());
						}
					}
				}
			}
		}
	}


	public TreeMap<String, CapacityPerNOGAType> getNogaShopFacilities() {
		return nogaShopFacilities;
	}


	public void setNogaShopFacilities(
			TreeMap<String, CapacityPerNOGAType> nogaShopFacilities) {
		this.nogaShopFacilities = nogaShopFacilities;
	}


	public TreeMap<String, CapacityPerNOGAType> getNogaGroceryShopFacilities() {
		return nogaGroceryShopFacilities;
	}


	public void setNogaGroceryShopFacilities(
			TreeMap<String, CapacityPerNOGAType> nogaGroceryShopFacilities) {
		this.nogaGroceryShopFacilities = nogaGroceryShopFacilities;
	}


	public TreeMap<String, CapacityPerNOGAType> getNogaLeisureFacilities() {
		return nogaLeisureFacilities;
	}


	public void setNogaLeisureFacilities(
			TreeMap<String, CapacityPerNOGAType> nogaLeisureFacilities) {
		this.nogaLeisureFacilities = nogaLeisureFacilities;
	}
}
