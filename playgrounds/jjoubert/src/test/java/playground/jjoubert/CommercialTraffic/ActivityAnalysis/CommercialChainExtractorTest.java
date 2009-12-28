/* *********************************************************************** *
 * project: org.matsim.*
 * CommercialChainExtractorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialTraffic.ActivityAnalysis;


import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.GPSPoint;
import playground.jjoubert.Utilities.Clustering.ClusterPoint;
import playground.jjoubert.Utilities.Clustering.DJCluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class CommercialChainExtractorTest extends MatsimTestCase{
	private Integer t = Integer.valueOf(0);

	/**
	 * This test builds a chain of activities:
	 * <ul> m - m - M - m - m - M - M - m - M - m - m </ul>
	 * 
	 *  where `m' denotes a <i>minor</i> activity, and `M' a <i>major</i> activity. We
	 *  test to see whether:
	 *  <ul>
	 *  <li> the leading <i>minor</i> activities are dropped;
	 *  <li> consecutive <i>major</i> activities are not considered a chain; and
	 *  <li> trailing <i>minor</i> activities are also omitted.
	 *  </ul>
	 *  the result should only be two chains: one with two <i>minor</i> activities, and
	 *  the other with a single <i>minor</i> activitiy. 
	 */
	public void testCommercialChainExtractor(){
		
		List<Activity> activities = buildActivityList1();
		CommercialChainExtractor ce = new CommercialChainExtractor(5);
		List<Chain> chains = ce.extractChains(activities);
		
		assertEquals("Wrong number of chains extracted.", 2, chains.size());
		assertEquals("Wrong chain extracted", 4, chains.get(0).getActivities().size());
		assertEquals("Wrong chain extracted", 3, chains.get(1).getActivities().size());
	}
	
	/**
	 * The method tests if a list of activities are <i>cleaned</i> correctly. All 
	 * consecutive activities that are in the same cluster should be merged into a
	 * single activity. Also, the locations of the activities should be set to the
	 * cluster centroids. The test data set created looks something like:
	 * <ul><code>
	 * M - [m - m] - [M - m] - m - [m - M]
	 * </code></ul>
	 *  where `m' denotes a <i>minor</i> activity, and `M' a <i>major</i> activity. 
	 *  Consecutive activities that are in the same cluster are indicated with square
	 *  brackets.
	 */
	public void testActivityThinning(){
		
		List<Activity> activities = buildActivityList2();
		
		// Convert the activities to points.
		CommercialActivityExtractor cae = new CommercialActivityExtractor();
		List<Point> points = cae.convertActivityToPoint(activities);
		// Cluster the points.
		DJCluster djc = new DJCluster(2,2,points);
		djc.clusterInput();

		QuadTree<ClusterPoint> qt = djc.getClusteredPoints();
		
		// Extract the chains.
		CommercialChainExtractor cce = new CommercialChainExtractor(5);
		cce.cleanActivityList(activities, qt);
		
		assertEquals("Wrong number of activities in the list.", 5, activities.size());
		assertEquals("Wrong duration for activity 1.", 10, activities.get(0).getDuration());
		assertEquals("Wrong duration for activity 2.", 2, activities.get(1).getDuration());
		assertEquals("Wrong duration for activity 3.", 11, activities.get(2).getDuration());
		assertEquals("Wrong duration for activity 4.", 1, activities.get(3).getDuration());
		assertEquals("Wrong duration for activity 5.", 11, activities.get(4).getDuration());
		
		Coordinate c1 = new Coordinate(0,Double.valueOf(1.0/3.0));
		Coordinate c2 = new Coordinate(5.5,0);
		Coordinate c3 = new Coordinate(5.5,5);
		Coordinate c4 = new Coordinate(0,5);
		
		assertEquals("Wrong location for activity 1.", true, c1.equals(activities.get(0).getLocation().getCoordinate()));
		assertEquals("Wrong location for activity 2.", true, c2.equals(activities.get(1).getLocation().getCoordinate()));
		assertEquals("Wrong location for activity 3.", true, c3.equals(activities.get(2).getLocation().getCoordinate()));
		assertEquals("Wrong location for activity 4.", true, c4.equals(activities.get(3).getLocation().getCoordinate()));	
		assertEquals("Wrong location for activity 5.", true, c1.equals(activities.get(4).getLocation().getCoordinate()));	

		// Check that the one chain end is the same as the start of the next chain
		
		List<Chain> chains = cce.extractChains(activities);
		assertEquals("Last major activity of one chain is not the first major activity of the subsequent chain.", true, 
				chains.get(0).getActivities().get(chains.get(0).getActivities().size()-1).getLocation().getCoordinate().equals(chains.get(1).getActivities().get(0).getLocation().getCoordinate()));
	}
	
	
	/**
	 * Builds a list of activities with leading <code>minor</code> activities; consecutive 
	 * <code>major</code> activities; and trailing <code>minor</code> activities. The 
	 * sequence of the activity types is:
	 * <ul> m - m - M - m - m - M - M - m - M - m - m </ul>
	 * 
	 *  where `m' denotes a <i>minor</i> activity, and `M' a <i>major</i> activity.
	 * @return a <code>List</code> of <code>Activity</code>s.
	 */
	private List<Activity> buildActivityList1(){
		List<Activity> list = new ArrayList<Activity>();
		t=0;
		
		list.add(addActivity(0, 0, 1));
		list.add(addActivity(1, 0, 1));
		list.add(addActivity(2, 0, 2));
		list.add(addActivity(3, 0, 1));
		list.add(addActivity(4, 0, 1));
		list.add(addActivity(5, 0, 2));
		list.add(addActivity(6, 0, 2));
		list.add(addActivity(7, 0, 1));
		list.add(addActivity(8, 0, 2));
		list.add(addActivity(9, 0, 1));
		list.add(addActivity(10, 0, 1));
		
		return list;
	}
	
	/**
	 * This method builds an activity list with specific locations.
	 * 		 _________ 
	 * 		| m    mM |
	 * 		|		  |
	 * 		| m		  |
	 * 		| M    mm |
	 * 		 ---------
	 * 
	 * where `m' denotes a <i>minor</i> activity, and `M' a <i>major</i> activity.
	 * @return a <code>List</code> of <code>Activity</code>s.
	 */
	private List<Activity> buildActivityList2(){
		List<Activity> list = new ArrayList<Activity>(8);
		t = 0;
		
		list.add(addActivity(0, 0, 2));
		list.add(addActivity(5, 0, 1));
		list.add(addActivity(6, 0, 1));
		list.add(addActivity(6, 5, 2));
		list.add(addActivity(5, 5, 1));
		list.add(addActivity(0, 5, 1));
		list.add(addActivity(0, 1, 1));
		list.add(addActivity(0, 0, 2));
		
		return list;
	}
	
	private Activity addActivity(double x, double y, int type){
		int duration;
		if(type==1){
			duration = 1;
		} else{
			duration = 10;
		}
		Activity a = new Activity(new GregorianCalendar(0,0,0,0,t), 
				new GregorianCalendar(0,0,0,0,t+duration), 
				new GPSPoint(1, 1, 1, new Coordinate(x,y)));
		t += duration;
		return a;
	}

}
