/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.run;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.wagonSim.schedule.mapping.ApplyToNetwork;
import org.matsim.contrib.wagonSim.schedule.mapping.ApplyToTransitSchedule;
import org.matsim.contrib.wagonSim.schedule.mapping.NetworkAdaption;
import org.matsim.contrib.wagonSim.schedule.mapping.NetworkEdit;
import org.matsim.contrib.wagonSim.schedule.mapping.NetworkEditsReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;

/**
 * @author balmermi
 *
 */
public class MATSimNetworkScheduleMergerMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(MATSimNetworkScheduleMergerMain.class);
	
	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public MATSimNetworkScheduleMergerMain() {
		scenario.getConfig().transit().setUseTransit(true);
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void mergeSchedule(String networkFilename, String transitScheduleFilename, String netEditsFilename) {
		log.info("Load network " + networkFilename);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		log.info("Load transit schedule " + transitScheduleFilename);
		new TransitScheduleReader(scenario).readFile(transitScheduleFilename);

		log.info("Load network edits " + netEditsFilename);
		List<NetworkEdit> edits = new ArrayList<NetworkEdit>();
		new NetworkEditsReader(edits).readFile(netEditsFilename);

		log.info("apply edits to network");
		new ApplyToNetwork(scenario.getNetwork()).applyEdits(edits);
		log.info("apply edits to transit schedule");
		new ApplyToTransitSchedule(scenario.getTransitSchedule()).applyEdits(edits);

		log.info("change all links to mode pt");
		Set<String> ptMode = new HashSet<String>();
		ptMode.add(TransportMode.pt);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setAllowedModes(ptMode);
		}

		log.info("fix link lengths…");
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double dist = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
			if (link.getLength() < dist) {
				link.setLength(dist);
			}
		}

		log.info("set all transit lines to wait at stop for departure time…");
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					stop.setAwaitDepartureTime(true);
				}
			}
		}

		log.info("set default link speeds and capacities…");
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(10.0);
			link.setCapacity(3000.0);
		}

		log.info("adapt network to schedule");
		NetworkAdaption na = new NetworkAdaption(scenario);
		na.adaptLinkFreespeeds();
		na.adaptLinkCapacities();
	}
	
	//////////////////////////////////////////////////////////////////////

	public final Scenario getMergedScenario() {
		return this.scenario;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		args = new String[] {
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/merged/edits00.txt",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/merged/network.merged.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitSchedule.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/merged/network.ott.performance.merged.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/merged/transitSchedule.ott.performance.merged.xml.gz",
//		};
		
		if (args.length != 5) {
			log.error(MATSimNetworkScheduleMergerMain.class.getCanonicalName()+" viaNetwotkEditsFile networkMergedFile scheduleFile outputNetworkFile outputScheduleFile");
			System.exit(-1);
		}
		
		String viaNetwotkEditsFile = args[0];
		String networkMergedFile = args[1];
		String scheduleFile = args[2];
		String outputNetworkFile = args[3];
		String outputScheduleFile = args[4];
		
		log.info("Main: "+MATSimNetworkScheduleMergerMain.class.getCanonicalName());
		log.info("viaNetwotkEditsFile: "+viaNetwotkEditsFile);
		log.info("networkMergedFile: "+networkMergedFile);
		log.info("scheduleFile: "+scheduleFile);
		log.info("outputNetworkFile: "+outputNetworkFile);
		log.info("outputScheduleFile: "+outputScheduleFile);
		
		MATSimNetworkScheduleMergerMain scheduleMerger = new MATSimNetworkScheduleMergerMain();
		scheduleMerger.mergeSchedule(networkMergedFile, scheduleFile, viaNetwotkEditsFile);
		
		new NetworkWriter(scheduleMerger.getMergedScenario().getNetwork()).write(outputNetworkFile);
		new TransitScheduleWriter(scheduleMerger.getMergedScenario().getTransitSchedule()).writeFile(outputScheduleFile);
		
		TransitScheduleValidator.printResult(TransitScheduleValidator.validateAll(scheduleMerger.getMergedScenario().getTransitSchedule(), scheduleMerger.getMergedScenario().getNetwork()));
	}
}
