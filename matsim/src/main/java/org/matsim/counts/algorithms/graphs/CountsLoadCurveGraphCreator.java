/* *********************************************************************** *
 * project: org.matsim.*
 * CountsLoadCurveGraphCreator.java
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

package org.matsim.counts.algorithms.graphs;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.helper.MyURL;

public class CountsLoadCurveGraphCreator extends CountsGraphsCreator {


	public CountsLoadCurveGraphCreator(final String sectionTitle) {
		super(sectionTitle);
	}


	@Override
	public List<CountsGraph> createGraphs(final List<CountSimComparison> ccl, final int iteration) {

		List<CountsGraph> graphList=new Vector<CountsGraph>();

		Iterator<CountSimComparison> l_it = ccl.iterator();
		CountSimComparison cc_last=null;
		while (l_it.hasNext()) {
			CountsLoadCurveGraph lcg=new CountsLoadCurveGraph(ccl, iteration,  "dummy");
			if (cc_last!=null) {
				lcg.add2LoadCurveDataSets(cc_last);
			}
			CountSimComparison cc= l_it.next();
			Id<Link> linkId = cc.getId();
			while (cc.getId().equals(linkId)) {
				if (l_it.hasNext()) {
					lcg.add2LoadCurveDataSets(cc);
					cc= l_it.next();
				}
				else {
					lcg.add2LoadCurveDataSets(cc);
					break;
				}
			}
			lcg.setChartTitle("Link "+linkId);
			lcg.setFilename("link"+linkId);
			lcg.setLinkId(linkId.toString());
			lcg.createChart(0);
			graphList.add(lcg);
			this.section.addURL(new MyURL("link"+linkId+".html", "link"+linkId));
			cc_last=cc;
		}//while


		return graphList;
	}
}
