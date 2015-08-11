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

import gnu.trove.TDoubleDoubleHashMap;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ActivityLoadTask implements ProxyAnalyzerTask {
	
	String outDir = "/home/johannes/gsv/mid2008/";

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.ProxyAnalyzerTask#analyze(java.util.Collection)
	 */
	@Override
	public void analyze(Collection<PlainPerson> persons) {
		Set<String> purposes = new HashSet<String>();
		for(PlainPerson person : persons) {
			Episode plan = person.getPlan();
			for(int i = 0; i < plan.getActivities().size(); i++) {
				purposes.add((String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE));
			}
		}
		
		for(String purpose : purposes) {
			TDoubleDoubleHashMap load = activityLoad(persons, purpose);
			try {
				TXTWriter.writeMap(load, "t", "freq", String.format("%1$s/actload.%2$s.txt", outDir, purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TDoubleDoubleHashMap load = activityLoad(persons, null);
		try {
			TXTWriter.writeMap(load, "t", "freq", String.format("%1$s/actload.all.txt", outDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final double resolution = 60;

	private TDoubleDoubleHashMap activityLoad(Collection<PlainPerson> persons, String type) {
		TDoubleDoubleHashMap loadMap = new TDoubleDoubleHashMap();
		
		for(PlainPerson person : persons) {
			Episode plan = person.getPlan();
			
			if(plan.getActivities().size() > 1) {
			for(Attributable act : plan.getActivities()) {
				
				if(type == null || ((String)act.getAttribute(CommonKeys.ACTIVITY_TYPE)).equalsIgnoreCase(type)) {
					int start = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_START_TIME));
					start = (int) (start/resolution);
					
					int end = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_END_TIME));
					end = (int) (end/resolution);
					
					for(int time = start; time < end; time++) {
						loadMap.adjustOrPutValue(time, 1, 1);
					}
				}
			}
			}
		}
		
		return loadMap;
	}
}
