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

package playground.johannes.studies.matrix2014.counts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class OSM2MatsimCounts {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String osmCountsFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.osm20140909.xml";
		String networkFile = "/home/johannes/gsv/germany-scenario/osm/network/germany-20140909.5.xml";
		String outFile = "/home/johannes/gsv/matrix2014/counts/counts.2014.net20140909.5.xml";

		Counts<Link> counts = new Counts();
		CountsReaderMatsimV1 cReader = new CountsReaderMatsimV1(counts);
		cReader.parse(osmCountsFile);

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(scenario.getNetwork());
		nReader.parse(networkFile);

		Network network = scenario.getNetwork();

		Map<String, Link> mapping = new HashMap<String, Link>();

		for (Link link : network.getLinks().values()) {
			String tokens[] = ((LinkImpl) link).getOrigId().split(",");
			for (String token : tokens) {
				mapping.put(token, link);
			}
		}

		Counts newCounts = new Counts();

		System.out.println(String.format("Number of original counts: %s", counts.getCounts().size()));
		
		for (Count<Link> count : counts.getCounts().values()) {
			Link link = mapping.get(count.getLocId().toString());
			if (link == null) {
				System.err.println(String.format("Cannot find link with id %s.", count.getLocId().toString()));
			} else {
				Count<Link> newCount = newCounts.createAndAddCount(link.getId(), count.getCsId());
				if (newCount == null) {
					System.err.println(String.format("There is already a count station on link %s.", link.getId().toString()));
				} else {
					newCount.setCoord(count.getCoord());
					for (Entry<Integer, Volume> entry : count.getVolumes().entrySet()) {
						newCount.createVolume(entry.getKey(), entry.getValue().getValue());
					}
				}
			}

		}

		System.out.println(String.format("Number of new counts: %s", newCounts.getCounts().size()));
		
		CountsWriter writer = new CountsWriter(newCounts);
		writer.write(outFile);
	}

}
