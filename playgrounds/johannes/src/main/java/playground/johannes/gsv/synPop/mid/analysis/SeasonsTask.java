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

package playground.johannes.gsv.synPop.mid.analysis;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class SeasonsTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.AnalyzerTask#analyze(java.util.Collection, java.util.Map)
	 */
	@Override
	public void analyze(Collection<PlainPerson> persons, Map<String, DescriptiveStatistics> results) {
		TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
		
		for(PlainPerson person : persons) {
			String month = person.getAttribute(MiDKeys.PERSON_MONTH);
			String season = "NA";
			if(MiDValues.NOVEMBER.equalsIgnoreCase(month)) {
				season = "win";
			} else if(MiDValues.DECEMBER.equalsIgnoreCase(month)) {
				season = "win";
			} else if(MiDValues.JANUARY.equalsIgnoreCase(month)) {
				season = "win";
			} else if(MiDValues.FEBRUARY.equalsIgnoreCase(month)) {
				season = "win";
			} else if(MiDValues.MARCH.equalsIgnoreCase(month)) {
				season = "win";
			} else if(month != null) {
				season = "sum";
			}
			
			String day = person.getAttribute(CommonKeys.DAY);
			String week = "wkday";
			if(CommonValues.SATURDAY.equalsIgnoreCase(day)) {
				week = "wkend";
			} else if(CommonValues.SUNDAY.equalsIgnoreCase(day)) {
				week = "wkend";
			}
			
			Set<String> modes = new HashSet<String>();
			for(Attributable leg : person.getPlan().getLegs()) {
				modes.add(leg.getAttribute(CommonKeys.LEG_MODE));
			}
			
			for(String mode : modes) {
				StringBuilder key = new StringBuilder(100);
				key.append(season);
				key.append(".");
				key.append(day);
				key.append(".");
				key.append(mode);
				
				map.adjustOrPutValue(key.toString(), 1, 1);
				
				key = new StringBuilder(100);
				key.append(season);
				key.append(".");
				key.append(week);
				key.append(".");
				key.append(mode);
				
				map.adjustOrPutValue(key.toString(), 1, 1);
			}
		}
		
		TObjectIntIterator<String> it = map.iterator();
		for(int i = 0; i < map.size(); i++) {
			it.advance();
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(it.value());
			results.put(it.key(), stats);
		}

	}

}
