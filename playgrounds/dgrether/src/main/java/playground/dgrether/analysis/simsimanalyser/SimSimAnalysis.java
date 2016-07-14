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
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;


/**
 * @author dgrether
 *
 */
public class SimSimAnalysis {
	private static final Logger log = Logger.getLogger(SimSimAnalysis.class);
	
	public Map<Id<Link>, List<CountSimComparison>> createCountSimComparisonByLinkId(Network network, VolumesAnalyzer vaCounts, VolumesAnalyzer vaSim){
		Map<Id<Link>, List<CountSimComparison>> countSimComp = new HashMap<Id<Link>, List<CountSimComparison>>(network.getLinks().size());

		for (Link l : network.getLinks().values()) {
			double[] volumesCounts = vaCounts.getVolumesPerHourForLink(l.getId());
			double[] volumesSim = vaSim.getVolumesPerHourForLink(l.getId());

			if ((volumesCounts.length == 0) || (volumesSim.length == 0)) {
				log.warn("No volumes for link: " + l.getId().toString());
				continue;
			}
			ArrayList<CountSimComparison> cscList = new ArrayList<CountSimComparison>();
			countSimComp.put(l.getId(), cscList);
			for (int hour = 1; hour <= 24; hour++) {
				double countValue=volumesCounts[hour-1];
				double simValue=volumesSim[hour-1];
				countSimComp.get(l.getId()).add(new CountSimComparisonImpl(l.getId(), hour, countValue, simValue));
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
