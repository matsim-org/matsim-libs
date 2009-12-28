/* *********************************************************************** *
 * project: org.matsim.*
 * CommercialChainExtractor.java
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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.Activity;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.Utilities.Clustering.Cluster;
import playground.jjoubert.Utilities.Clustering.ClusterPoint;

/**
 * A class to extract a <code>List</code> of activity <code>Chain</code>s from a 
 * <code>List</code> of vehicle <code>Activity</code>s.
 * 
 * @author jwjoubert
 */
public class CommercialChainExtractor {
	private final Logger log = Logger.getLogger(CommercialChainExtractor.class);
	private final double threshold;
	
	/**
	 * Constructs an instance of the object.
	 * @param threshold the duration threshold (expressed in minutes) for <i>major</i>
	 * 		<code>Activities</code>, such that the duration split can be expressed as
	 * 		<ul>
	 * 		0 < <i>minor</i> duration < threshold <= <i>major</i> duration <= <code>Inf</code>
	 * 		</ul> 
	 * @author jwjoubert
	 */
	public CommercialChainExtractor(double threshold){
		this.threshold = threshold;
	}
	
	/**
	 * The method converts a <code>List</code> of <code>Activity</code>s into a 
	 * <code>List</code> of <code>Chain</code>s, each <code>Chain</code> starting and
	 * ending with a <i>major</i> activity. The process is roughly as follows:
	 * <ul>
	 * <li> leading <i>minor</i> activities are deleted, i.e. all <i>minor</i> activities 
	 * 		until the first <i>major</i> activity;
	 * <li> a new <code>Chain</code> is created starting with the first <i>major</i> 
	 * 		activity;
	 * <li> <i>minor</i> activities are added to the current <code>List</code> until the
	 * 		next <i>major</i> <code>Activity</code> is found, which is added as the 
	 * 		last <code>Activity</code> of the <code>Chain</code>;
	 * <li> a new <code>Chain</code> is created, starting with the same activity that
	 * 		ended the previous <code>Chain</code>, and the process repeats until the 
	 * 		end of the <code>List</code> of <code>Activity</code>s  is reached;  
	 * <li> the trailing chain is deleted, i.e. the last chain extracted from the 
	 * 		<code>List</code> that does not end with a <i>major</i> activity. 
	 * </ul> 
	 * @param activityList a <code>List</code> of (preferrable <i>cleaned</i>) 
	 * 		<code>Activity</code>s. With <i>cleaned</i> is meant that the method
	 * 		<code>cleanActivityList</code> of this same class has been applied to
	 * 		the <code>List</code>.
	 * @return a <code>List<code> containing complete <code>Chain</code>s.
	 */
	public List<Chain> extractChains(List<Activity> activityList){
		List<Chain> result = new ArrayList<Chain>();
		
		// Find the first 'major' activity
		boolean firstFound = false;
		while(activityList.size() > 0 && !firstFound){
			if(activityList.get(0).getDuration() > threshold){
				firstFound = true;
			} else{
				activityList.remove(0);
			}
		}
		
		while(activityList.size() > 0){
			List<Activity> chainCandidate = new ArrayList<Activity>();
			// Add the first major location
			chainCandidate.add(activityList.get(0));
			activityList.remove(0);
			
			// Search for the end-of-chain major activity
			while(activityList.size() > 0){
				if(activityList.get(0).getDuration() > threshold){
					chainCandidate.add(activityList.get(0));
					
					// Build the chain if it contains at least one 'minor' activity.
					if(chainCandidate.size() > 2){
						Chain chain = new Chain();
						chain.getActivities().addAll(chainCandidate);
						chain.setDayStart(chainCandidate.get(0).getEndTime());
						chain.setDistance();
						chain.setDuration();
						result.add(chain);
					} else{
						// Discard the chain as a `relocation'
					}
					/*
					 * Create a new chain, and add the last 'major' location of the 
					 * previous chain as the first 'major' location of the new chain. 
					 */
					chainCandidate = new ArrayList<Activity>();		
					chainCandidate.add(activityList.get(0));
					activityList.remove(0);
				} else{
					chainCandidate.add(activityList.get(0));
					activityList.remove(0);
				}

			}
		}
		/*
		 * If the end is reached without the last chain to be complete, i.e. without an
		 * ending 'major' activity, it will be discarded. This is actually correct, since
		 * we only want to evaluate complete activity chains.
		 */
		return result;
	}
	
	/**
	 * This method merges all consecutive activities that share the same cluster, and 
	 * then adjust the activity locations to that of the cluster to which it belongs, if
	 * the activity actually do belong to a cluster. Activities not belonging to any 
	 * cluster will thus <i>not</i> be discarded.
	 * @param list the list of all <code>Activity</code>s.
	 * @param qt a <code>QuadTree</code> with all the activities represented as 
	 * 		<code>ClusterPoint</code>s. The <code>QuadTree</code> is usually the result 
	 * 		of a <code>DJCluster</code> density-based clustering object, hence all 
	 * 		<code>Activity</code>s, both clustered and unclustered are represented. 
	 * 		Unclustered <code>Activity</code>s are represented as a <code>ClusterPoint</code> 
	 * 		with <code>cluster</code> attribute set to <code>null</code>.
	 */
	public void cleanActivityList(List<Activity> list, QuadTree<ClusterPoint> qt){		
		// Merge activities that share the same cluster.
		if(list.size() > 1){
			int index = 1;
			while(index < list.size()){
				Activity a = list.get(index-1);
				Cluster ca = ((List<ClusterPoint>) qt.get(a.getLocation().getCoordinate().x, a.getLocation().getCoordinate().y, 0)).get(0).getCluster();

				Activity b = list.get(index);
				Cluster cb = ((List<ClusterPoint>) qt.get(b.getLocation().getCoordinate().x, b.getLocation().getCoordinate().y, 0)).get(0).getCluster();
	
				// Merge if they are in the SAME cluster.
				if(ca != null && cb != null){
					if(ca.getClusterId().equalsIgnoreCase(cb.getClusterId())){
						a.setEndTime(b.getEndTime());
						list.remove(index);
					} else{
						index++;
					}
				} else{
					index++;
				}
			}
		} else{
			log.warn("Could not thin the list. There is only one element in the list.");
		}
		
		// Set the activity locations to cluster centroids.
		for (Activity a : list) {
			Cluster c = ((List<ClusterPoint>) qt.get(a.getLocation().getCoordinate().x, a.getLocation().getCoordinate().y, 0)).get(0).getCluster();
			if(c != null){
				a.getLocation().setCoordinate(c.getCenterOfGravity().getCoordinate());
			}
		}
	}
	
}
