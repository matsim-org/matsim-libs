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
package playground.vsp.analysis.modules.ptAccessibility.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;


/**
 * @author droeder
 *
 */
public class LocationMap {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(LocationMap.class);
	private Map<String, List<String>> activityCluser;
	
	private SortedMap<String, List<ActivityLocation>> type2locations;

	public LocationMap(Map<String, List<String>> activityCluster) {
		this.activityCluser = activityCluster;
		this.type2locations = new TreeMap<String, List<ActivityLocation>>();
		for(String s: activityCluster.keySet()){
			this.type2locations.put(s, new ArrayList<ActivityLocation>());
		}
		this.type2locations.put("unknown", new ArrayList<ActivityLocation>());
	}
	
	public void addActivity(Activity activity){
		String type = "unknown";
		for(Entry<String, List<String>> e: this.activityCluser.entrySet()){
			if(e.getValue().contains(activity.getType())){
				type = e.getKey();
				break;
			}
		}
		ActivityLocation loc = new ActivityLocation(activity.getCoord(), type);
		this.type2locations.get(type).add(loc);
//		if(!this.type2locations.get(type).contains(loc)){
//		}
	}
	
	public Map<String, List<ActivityLocation>> getType2Locations(){
		return this.type2locations;
	}
	
	
	public static void main(String[] args) {
		Map<String, List<String>> cluster = new HashMap<String, List<String>>();
		List<String> types = new ArrayList<String>();
		
		types.add("1");
		types.add("2");
		cluster.put("a", types);
		
		types = new ArrayList<String>();
		types.add("3");
		cluster.put("b", types);
		
		LocationMap map = new LocationMap(cluster);
		map.addActivity(PopulationUtils.createActivityFromCoord("1", new Coord((double) 1, (double) 2)));
		map.addActivity(PopulationUtils.createActivityFromCoord("1", new Coord((double) 2, (double) 2)));
		map.addActivity(PopulationUtils.createActivityFromCoord("2", new Coord((double) 1, (double) 3)));
		map.addActivity(PopulationUtils.createActivityFromCoord("1", new Coord((double) 1, (double) 2)));

		map.addActivity(PopulationUtils.createActivityFromCoord("3", new Coord((double) 1, (double) 2)));

		map.addActivity(PopulationUtils.createActivityFromCoord("4", new Coord((double) 1, (double) 2)));
		
		for(Entry<String, List<ActivityLocation>> e: map.getType2Locations().entrySet()){
			System.out.println(e.getValue().size() + " ActivityLocations of Type " + e.getKey() + " at:");
			for(ActivityLocation l: e.getValue()){
				System.out.println(l.getType() + " " + l.getCoord().toString());
			}
			System.out.println();
			System.out.println();
		}
		
	}
	
}

