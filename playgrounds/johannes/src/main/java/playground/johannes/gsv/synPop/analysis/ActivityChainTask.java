/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Collection;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.util.TXTWriter;

/**
 * @author johannes
 *
 */
public class ActivityChainTask implements ProxyAnalyzerTask {
	
	String outDir = "/home/johannes/gsv/mid2008/";

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.ProxyAnalyzerTask#analyze(java.util.Collection)
	 */
	@Override
	public void analyze(Collection<ProxyPerson> persons) {
//		DescriptiveStatistics stats = new DescriptiveStatistics();
		TObjectDoubleHashMap<String> chains = new TObjectDoubleHashMap<String>();
		
		for(ProxyPerson person : persons) {
			ProxyPlan trajectory = person.getPlan();
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < trajectory.getActivities().size(); i++) {
				String type = (String) trajectory.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
				builder.append(type);
				builder.append("-");
			}
			
			String chain = builder.toString();
			chains.adjustOrPutValue(chain, 1, 1);
		}
		
		try {
			TXTWriter.writeMap(chains, "chain", "n", outDir + "/actchains.txt", true);
				
//			writeHistograms(stats, new DummyDiscretizer(), "actchain", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
