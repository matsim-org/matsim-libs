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
package playground.jbischoff.wobscenario.preparation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TaxibusStopExtractor {
	public static void main(String[] args) {
		String transitStopsOutputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/taxibusstops.xml";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Set<Link> stopLinks = new HashSet<>();
		new TransitScheduleReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/transitschedule.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-av-mar17.xml");
		NetworkFilterManager nfm = new NetworkFilterManager(scenario.getNetwork());
		nfm.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) return true;
				else return false;
			}
		});
		Network net2 = nfm.applyFilters();
		
		for (TransitStopFacility f : scenario.getTransitSchedule().getFacilities().values()){
			Link l = NetworkUtils.getNearestLink(net2, f.getCoord());
			stopLinks.add(l);
		}
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = f.createTransitSchedule();
		for (Link l : stopLinks){
			TransitStopFacility fac = f.createTransitStopFacility(Id.create(l.getId().toString()+"_stop",TransitStopFacility.class), l.getCoord(), false);
			fac.setLinkId(l.getId());
			schedule.addStopFacility(fac);
			
		}
		
		new TransitScheduleWriter(schedule).writeFile(transitStopsOutputFile);

		
	}
}
