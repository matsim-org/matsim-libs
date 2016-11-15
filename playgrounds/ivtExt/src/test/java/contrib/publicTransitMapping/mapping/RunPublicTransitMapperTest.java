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

package contrib.publicTransitMapping.mapping;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import contrib.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import contrib.publicTransitMapping.tools.NetworkTools;
import contrib.publicTransitMapping.tools.ScheduleCleaner;
import contrib.publicTransitMapping.tools.ScheduleTools;

import java.util.List;

/**
 * Takes the siouxfalls example schedule and network, removes the mapping and runs the PTMapper on the unmapped schedule.
 * The results are compared and given the simplicity of the network, in- and output should be the same
 *
 * @author polettif
 */
public class RunPublicTransitMapperTest {

	private Network network;
	private TransitSchedule schedule;
	private TransitSchedule originalSchedule;
	private PublicTransitMappingConfigGroup config;

	@Before
	public void prepare() {
		network = NetworkTools.readNetwork("test/scenarios/siouxfalls-2014-reduced/Siouxfalls_network_PT.xml");
		schedule = ScheduleTools.readTransitSchedule("test/scenarios/siouxfalls-2014-reduced/Siouxfalls_transitSchedule.xml");
		originalSchedule = ScheduleTools.readTransitSchedule("test/scenarios/siouxfalls-2014-reduced/Siouxfalls_transitSchedule.xml");
		config = PublicTransitMappingConfigGroup.createDefaultConfig();

		// remove Mapping
		ScheduleCleaner.removeMapping(schedule);
	}

	@Test
	public void run() throws Exception {
		// check inequality
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(route);
				List<Id<Link>> originalLinkIds = ScheduleTools.getTransitRouteLinkIds(originalSchedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()));

				Assert.assertNotEquals(linkIds, originalLinkIds);
			}
		}

		// run PTMapper
		PTMapper ptMapper = new PTMapperImpl(config, schedule, network);
		ptMapper.run();

		// check equality
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(route);
				List<Id<Link>> originalLinkIds = ScheduleTools.getTransitRouteLinkIds(originalSchedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId()));

				Assert.assertEquals(linkIds, originalLinkIds);
			}
		}
	}
}