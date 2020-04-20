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

package playground.vsp.andreas.utils.ana;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Returns the activities of BVG3
 * 
 * @author aneumann
 *
 */
public class BVG3ActsScheme {

	public static SortedMap<String, List<String>> createBVG3ActsScheme(){
		SortedMap<String, List<String>> activityCluster = new TreeMap<String, List<String>>();
		
		List<String> activities;
		
		activities = new ArrayList<String>();
		activities.add("home");
		activityCluster.put("home", activities);

		activities = new ArrayList<String>();
		activities.add("work");
		activityCluster.put("work", activities);

		activities = new ArrayList<String>();
		activities.add("educ_primary");
		activities.add("educ_secondary");
		activities.add("educ_higher");
		activities.add("kiga");
		activityCluster.put("education", activities);

		activities = new ArrayList<String>();
		activities.add("leisure");
		activityCluster.put("leisure", activities);	

		activities = new ArrayList<String>();
		activities.add("shop_daily");
		activities.add("shop_other");
		activityCluster.put("shopping", activities);

		activities = new ArrayList<String>();
		activities.add("other");
		activities.add("service");
		activityCluster.put("other", activities);

		activities = new ArrayList<String>();
		activities.add("bbiEnd");
		activities.add("bbiStart");
		activities.add("fhtoevEnd");
		activities.add("fhtoevStart");
		activities.add("fhtmivEnd");
		activities.add("fhtmivStart");
		activities.add("fhmivEnd");
		activities.add("fhmivStart");
		activities.add("fhoevEnd");
		activities.add("fhoevStart");
		activityCluster.put("airport", activities);

		activities = new ArrayList<String>();
		activities.add("wvEnd");
		activities.add("wvStart");
		activities.add("lkwEnd");
		activities.add("lkwStart");
		activityCluster.put("wv", activities);

		activities = new ArrayList<String>();
		activities.add("fernoevEnd");
		activities.add("fernoevStart");
		activityCluster.put("fernoev", activities);

		activities = new ArrayList<String>();
		activities.add("tmivEnd");
		activities.add("tmivStart");
		activities.add("toevEnd");
		activities.add("toevStart");
		activityCluster.put("tourist", activities);

		return activityCluster;
	}
}
