/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class TripDayVolumeTask extends AnalyzerTask {

	private final String mode;
	
	public TripDayVolumeTask(String mode) {
		this.mode = mode;
	}
	
	@Override
	public void analyze(Collection<ProxyPerson> persons, Map<String, DescriptiveStatistics> results) {
		TObjectIntHashMap<String> values = new TObjectIntHashMap<>();
		
		for(ProxyPerson person : persons) {
			String day = person.getAttribute(CommonKeys.DAY);
			for(ProxyPlan plan : person.getPlans()) {
				
				int cnt = 0;
				for(ProxyObject leg : plan.getLegs()) {
					if(mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
						cnt++;
					}
				}
				values.adjustOrPutValue(day, cnt, cnt);
			}
		}
		
		TObjectIntIterator<String> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			DescriptiveStatistics tmp = new DescriptiveStatistics();
			tmp.addValue(it.value());
			results.put(it.key(), tmp);
		}

	}

}
