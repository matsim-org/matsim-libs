/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonLinkFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts.algorithms;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.CountSimComparison;

/**
 * This class can be used to return filtered views on a List of
 * CountSimComparison objects.
 *
 * @author anhorni
 *
 */
public class CountSimComparisonLinkFilter {

	private Hashtable<Id<Link>, List<CountSimComparison>> countSimComparisonLinkMap;
	
	/**
	 * The list containing the comparisons
	 */
	private final List<CountSimComparison> countSimComparisons;

	/**
	 * Fills the hash table for each link with the appropriate CountSimComparison objects
	 * from the list given as parameter.
	 * @param countSimComparisons
	 *
	 */
	public CountSimComparisonLinkFilter(final List<CountSimComparison> countSimComparisons) {
		this.countSimComparisons = countSimComparisons;
		this.countSimComparisonLinkMap=new Hashtable<>();
		
		if (this.countSimComparisons.size() < 1) {
			return;
		}

		List<CountSimComparison> countSimComparisonsPerLink=new Vector<CountSimComparison>();
		Id<Link> prevId=this.countSimComparisons.get(0).getId();
		
		Iterator<CountSimComparison> csc_it = this.countSimComparisons.iterator();		
		while (csc_it.hasNext()) {
			CountSimComparison csc= csc_it.next();	
						
			// how to compare IdIs ? Id:  ... this.id.compareTo(id.toString());
			if (csc.getId().compareTo(prevId)==0) {
				countSimComparisonsPerLink.add(csc);
			}
		
			// first element of next link id OR last element of last link id
			if (!(csc.getId().compareTo(prevId)==0) || (!csc_it.hasNext())) {
				countSimComparisonLinkMap.put(prevId, countSimComparisonsPerLink);
				countSimComparisonsPerLink=new Vector<CountSimComparison>();
				countSimComparisonsPerLink.add(csc);
			}
			prevId=csc.getId();
		}
	}

	/**
	 * @param linkfilter
	 * @return All counts if the parameter is null, else a subset of all counts for the given link
	 */
	public List<CountSimComparison> getCountsForLink(final Id<Link> linkfilter) {
		// only need to do this once
		if (linkfilter == null) {
			return this.countSimComparisons;
		}
		return this.countSimComparisonLinkMap.get(linkfilter);
	}
	
	public double getAggregatedCountValue(final Id<Link> linkfilter) {
		Iterator<CountSimComparison> csc_it = this.countSimComparisonLinkMap.get(linkfilter).iterator();		
		double countValue=0.0;
		while (csc_it.hasNext()) {
			CountSimComparison csc= csc_it.next();
			countValue+=csc.getCountValue();
		}
		return countValue;
	}
	
	public double getAggregatedSimValue(final Id<Link> linkfilter) {
		Iterator<CountSimComparison> csc_it = this.countSimComparisonLinkMap.get(linkfilter).iterator();		
		double simValue=0.0;
		while (csc_it.hasNext()) {
			CountSimComparison csc= csc_it.next();
			simValue+=csc.getSimulationValue();
		}	
		return simValue;
	}
	
	// sorting ok for IdIs ?
	public Vector<Id<Link>> getLinkIds() {
		Vector<Id<Link>> linkIds = new Vector<Id<Link>>(countSimComparisonLinkMap.keySet());
	    Collections.sort(linkIds);    
	    return linkIds;
	}
}
