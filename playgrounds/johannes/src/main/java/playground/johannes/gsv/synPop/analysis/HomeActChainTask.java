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
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class HomeActChainTask extends AnalyzerTask {

	public static final String KEY = "n.home.act";
	
	@Override
	public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<String> chains = new TObjectDoubleHashMap<String>();
		
		TDoubleDoubleHashMap tripCounts = new TDoubleDoubleHashMap();
		
		for(Person person : persons) {
			Episode plan = person.getEpisodes().get(0);
			
			List<String> achain = new ArrayList<>();
			for(int i = 0; i < plan.getActivities().size(); i++) {
				String type = (String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
				achain.add(type);
				if(type.equalsIgnoreCase(ActivityTypes.HOME) && i > 0) {
					String chain = StringUtils.join(achain, "-");
					chains.adjustOrPutValue(chain, 1, 1);
					tripCounts.adjustOrPutValue(achain.size() - 1, 1, 1);
					
					achain = new ArrayList<>();
					achain.add(type);
				}
			}
			
		}
		
		
		if (outputDirectoryNotNull()) {
			try {
				TXTWriter.writeMap(chains, "chain", "n", getOutputDirectory() + "/homeactchains.txt", true);
				
				TXTWriter.writeMap(tripCounts, "nTrips", "n", getOutputDirectory() + "/hometripcounts.txt", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
