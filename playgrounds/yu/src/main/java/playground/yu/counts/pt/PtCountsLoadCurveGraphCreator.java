/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountsLoadCurveGraphCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.counts.pt;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsGraphsCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.counts.algorithms.graphs.helper.MyURL;

/**
 * @author yu
 *
 */
public class PtCountsLoadCurveGraphCreator extends CountsGraphsCreator {

	/**
	 * @param sectionTitle
	 */
	public PtCountsLoadCurveGraphCreator(String sectionTitle) {
		super(sectionTitle);
	}
	@Override
	public List<CountsGraph> createGraphs(List<CountSimComparison> ccl,
			int iteration) {
		List<CountsGraph> graphList=new Vector<CountsGraph>();

		Iterator<CountSimComparison> l_it = ccl.iterator();
		CountSimComparison cc_last=null;
		while (l_it.hasNext()) {
			CountsLoadCurveGraph lcg=new CountsLoadCurveGraph(ccl, iteration,  "dummy");
			if (cc_last!=null) {
				lcg.add2LoadCurveDataSets(cc_last);
			}
			CountSimComparison cc= l_it.next();
			Id stopId = cc.getId();
			while (cc.getId().equals(stopId)) {
				if (l_it.hasNext()) {
					lcg.add2LoadCurveDataSets(cc);
					cc= l_it.next();
				}
				else {
					lcg.add2LoadCurveDataSets(cc);
					break;
				}
			}
			lcg.setChartTitle("Stop "+stopId);
			lcg.setFilename("stop"+stopId);
			lcg.setLinkId(stopId.toString());
			lcg.createChart(0);
			graphList.add(lcg);
			this.section.addURL(new MyURL("stop"+stopId+".html", "stop"+stopId));
			cc_last=cc;
		}//while


		return graphList;
	}

}
