/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.wobscenario.peoplemover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreatePeoplemoverStops {
	public static void main(String[] args) {

		String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-feb.xml";
		String networkModeDesignator = "vw";
		double averageStopDistance_meters = 300;
		String transitStopsOutputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/stops.xml";
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		NetworkFilterManager nfm = new NetworkFilterManager(network);
		nfm.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains(networkModeDesignator)) return true;
				else
				return false;
			}
		});
		
		Network stopNetwork = nfm.applyFilters();
		
		int stopNumber = estimateStopNumbers(stopNetwork, averageStopDistance_meters);
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = f.createTransitSchedule();
		List<Link> allLinks = new ArrayList<>();
		allLinks.addAll(stopNetwork.getLinks().values());
		Random r = MatsimRandom.getRandom();
		Collections.shuffle(allLinks, r);
		for (int i = 0; i< stopNumber; i++)
		{
			int draw = r.nextInt(allLinks.size());
			Link l = allLinks.remove(draw);
			TransitStopFacility stop = f.createTransitStopFacility(Id.create(l.getId().toString()+"_stop", TransitStopFacility.class), ParkingUtils.getRandomPointAlongLink(r, l), false);
			stop.setLinkId(l.getId());
			schedule.addStopFacility(stop);
		}
		new TransitScheduleWriter(schedule).writeFile(transitStopsOutputFile);
		
		
	}

	/**
	 * @param stopNetwork
	 * @param averageStopDistance
	 * @return
	 */
	private static int estimateStopNumbers(Network stopNetwork, double averageStopDistance) {
		double networkLength = 0;
		for (Link l : stopNetwork.getLinks().values()){
			networkLength+=l.getLength();
		}
		Logger.getLogger(CreatePeoplemoverStops.class).info("Overall network length for stops " + networkLength);
		Logger.getLogger(CreatePeoplemoverStops.class).info("Overall number of links for stops " + stopNetwork.getLinks().size());

		int stopNumbers = (int) (networkLength / averageStopDistance);
		if (stopNumbers > stopNetwork.getLinks().size())
		{
			Logger.getLogger(CreatePeoplemoverStops.class).warn("Stop density requires more stops than there are links in the network. This will not provide meaningful results. Will continue with one stop per Link.");
			stopNumbers = stopNetwork.getLinks().size();
		}
		return stopNumbers;
	}
}
