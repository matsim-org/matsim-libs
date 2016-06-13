/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench.santiago;

import playground.polettif.publicTransitMapping.mapping.PTMapperPseudoRouting;

public class Preparation {

	public static void main(String[] args) {

		// create unmapped schedule
//		TransitSchedule schedule = ScheduleTools.readTransitSchedule("original/transitschedule_simplified.xml.gz");
//		ScheduleCleaner.removeMapping(schedule);
//		ScheduleTools.writeTransitSchedule(schedule, "data/transitSchedule_unmapped.xml.gz");

		// schedule from GTFS
//		Gtfs2TransitSchedule.run("gtfs/", "mts/", "dayWithMostServices", "EPSG:32719");

		// create network from osm
//		OsmMultimodalNetworkConverter.run("osm/santiago.osm", "network/santiago_osm.xml.gz", "EPSG:32719");

		// filter provided network
//		Network provNetwork = NetworkTools.filterNetworkByLinkMode(NetworkTools.readNetwork("original/network_merged_cl.xml.gz"), Collections.singleton("car"));
//		NetworkTools.writeNetwork(provNetwork, "ptm/filtered_network.xml.gz");

		// population
//		PopulationUtils.readPopulation("input_original/plans_final.xml");
//
		// run PTMapper
		PTMapperPseudoRouting.run("ptm/ptm_santiago.xml");
	}
}
