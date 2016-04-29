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

package playground.polettif.multiModalMap.validation;

import org.geotools.factory.GeoTools;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class Schedule2ShapeFileConverter {

	TransitSchedule schedule;
	Network network;

	public Schedule2ShapeFileConverter(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public static void main(final String[] args) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

		Schedule2ShapeFileConverter s2s = new Schedule2ShapeFileConverter(schedule, network);

//		s2s.write("");
	}

	public void convert(String transitRouteId) {

		NetworkLinkFilter carOnlyFilter = new LinkFilterCarOnly();

		NetworkFilterManager filterManager = new NetworkFilterManager(network);

	}


	private class LinkFilterCarOnly implements NetworkLinkFilter {

		@Override
		public boolean judgeLink(Link l) {
			return l.getAllowedModes().contains(TransportMode.car);
		}
	}
}