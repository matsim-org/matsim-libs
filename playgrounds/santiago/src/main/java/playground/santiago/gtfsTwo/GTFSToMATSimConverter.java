/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.santiago.gtfsTwo;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

/**
 * @author nagel
 *
 */
public class GTFSToMATSimConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		args = new String[2] ;
		args[0] = "/Users/nagel/kw/output_schedule.xml.gz" ;
		args[1] = "/Users/nagel/public-svn/matsim/scenarios/countries/cl/santiago/v1/santiago/input/network_merged_cl.xml.gz" ;
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		Network network = scenario.getNetwork();
		//Convert lengths of the link from km to m and speeds from km/h to m/s
		for(Link link:network.getLinks().values()) {
			link.setLength(link.getLength()*1000);
			link.setFreespeed(link.getFreespeed()/3.6);
		}
		
		String filebase = "/Users/nagel/Downloads/santiago" ;
		
		//Construct conversion object
		GTFS2MATSimTransitSchedule g2m = new GTFS2MATSimTransitSchedule(
				new File[]{new File( filebase )},
				new String[]{"road"}, 
				network, 
				new String[]{"weekday"}, 
				TransformationFactory.WGS84_UTM47S
				);
		//Convert
		(new TransitScheduleWriter(g2m.getTransitSchedule())).writeFile(args[0]);
		//Write modified network
//		((NetworkImpl)network).setName("santiago");
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write(args[2]);
	}

}
