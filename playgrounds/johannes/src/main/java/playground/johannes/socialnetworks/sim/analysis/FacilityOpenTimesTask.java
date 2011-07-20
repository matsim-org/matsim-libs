/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityOpenTimesTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author illenberger
 *
 */
public class FacilityOpenTimesTask {

	private String output;

	public FacilityOpenTimesTask(String output) {
		this.output = output;
	}
	
	public void analyze(ActivityFacilities facilities) {
		FacilityOpenTimes openTimes = new FacilityOpenTimes();
		Map<String, Map<String, TDoubleDoubleHashMap>> statistics = openTimes.statistics(facilities);
		
		for(Entry<String, Map<String, TDoubleDoubleHashMap>> entry : statistics.entrySet()) {
			for(Entry<String, TDoubleDoubleHashMap> entry2 : entry.getValue().entrySet()) {
				TDoubleDoubleHashMap hist = entry2.getValue();
				
				try {
					TXTWriter.writeMap(hist, "t", "n", String.format("%1$s/openTime.%2$s.%3$s.txt", output, entry.getKey(), entry2.getKey()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String args[]) {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.xml");
		
		FacilityOpenTimesTask task = new FacilityOpenTimesTask("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/analysis/facilities");
		task.analyze(scenario.getActivityFacilities());
	}
}
