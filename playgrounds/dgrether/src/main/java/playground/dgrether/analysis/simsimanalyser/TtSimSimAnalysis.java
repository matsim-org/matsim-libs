/* *********************************************************************** *
 * project: org.matsim.*
 * SimSimAnalysis
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
package playground.dgrether.analysis.simsimanalyser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;


/**
 * difference to SimSimAnalysis: 
 * 		creates Map<Id, List<CountSimComparison>> object for accurate time bins and not per hour
 * 		creates Map<Id, List<CountSimComparison>> object for link enter and link leave counts and not only for link leave counts
 * 
 * @author tthunig
 *
 */
public class TtSimSimAnalysis {
	private static final Logger log = Logger.getLogger(TtSimSimAnalysis.class);
	
	public Map<Id, List<CountSimComparison>> createCountSimLinkLeaveComparisonByLinkId(Network network, TtVolumesAnalyzer vaCounts, TtVolumesAnalyzer vaSim){
		return createCountSimComparisonByLinkId(network, vaCounts, vaSim, true);
	}
	
	public Map<Id, List<CountSimComparison>> createCountSimLinkEnterComparisonByLinkId(Network network, TtVolumesAnalyzer vaCounts, TtVolumesAnalyzer vaSim){
		return createCountSimComparisonByLinkId(network, vaCounts, vaSim, false);
	}
		
	private Map<Id, List<CountSimComparison>> createCountSimComparisonByLinkId(Network network, TtVolumesAnalyzer vaCounts, TtVolumesAnalyzer vaSim, boolean linkLeave){
		Map<Id, List<CountSimComparison>> countSimComp = new HashMap<Id, List<CountSimComparison>>(network.getLinks().size());

		for (Link l : network.getLinks().values()) {
			int[] volumesCounts, volumesSim;
			if (linkLeave){
				volumesCounts = vaCounts.getLinkLeaveVolumesForLink(l.getId());
				volumesSim = vaSim.getLinkLeaveVolumesForLink(l.getId());
			}
			else{
				volumesCounts = vaCounts.getLinkEnterVolumesForLink(l.getId());
				volumesSim = vaSim.getLinkEnterVolumesForLink(l.getId());
			}
			
			if ((volumesCounts.length == 0) || (volumesSim.length == 0)) {
				log.warn("No volumes for link: " + l.getId().toString());
				continue;
			}
			ArrayList<CountSimComparison> cscList = new ArrayList<CountSimComparison>();
			countSimComp.put(l.getId(), cscList);
			for (int timebin = 1; timebin <= volumesCounts.length; timebin++) {
				double countValue=volumesCounts[timebin-1];
				double simValue=volumesSim[timebin-1];
				countSimComp.get(l.getId()).add(new CountSimComparisonImpl(l.getId(), timebin, countValue, simValue));
			}
			//sort the list
			Collections.sort(cscList, new Comparator<CountSimComparison>() {
				@Override
				public int compare(CountSimComparison c1, CountSimComparison c2) {
					return new Integer(c1.getHour()).compareTo(c2.getHour());
				}
			});
		}
		return countSimComp;
	}
	
}
