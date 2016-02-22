/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.jbischoff.av.evaluation;

import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.*;
import org.matsim.core.network.*;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class RunAVEvaluation {

	public static void main(String[] args) {

		String networkFile = "../../../shared-svn/projects/audi_av/scenario/networkc.xml.gz";
		String eventsFile = "../../../shared-svn/projects/audi_av/runs/mobiltum/24-11k/nullevents.24-11k.xml.gz";
		String outputFolder = "../../../shared-svn/projects/audi_av/runs/mobiltum/24-11k/";
		String shapeFile = "../../../shared-svn/projects/audi_av/shp/Planungsraum.shp";
		Map<String,Geometry> geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile);
		Network network = NetworkUtils.createNetwork() ;
		new MatsimNetworkReader(network).readFile(networkFile);
		ZoneBasedTaxiCustomerWaitHandler zoneBasedTaxiCustomerWaitHandler = new ZoneBasedTaxiCustomerWaitHandler(network, geo);
//		TravelDistanceTimeEvaluator travelDistanceTimeEvaluator = new TravelDistanceTimeEvaluator(network, 0);
		ZoneBasedTaxiStatusAnalysis zoneBasedTaxiStatusAnalysis = new ZoneBasedTaxiStatusAnalysis(network, geo);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(zoneBasedTaxiCustomerWaitHandler);
//		events.addHandler(travelDistanceTimeEvaluator);
		events.addHandler(zoneBasedTaxiStatusAnalysis);
		new MatsimEventsReader(events).readFile(eventsFile);
		zoneBasedTaxiCustomerWaitHandler.writeCustomerStats(outputFolder);
//		travelDistanceTimeEvaluator.writeTravelDistanceStatsToFiles(outputFolder+"distanceStats.txt");
		zoneBasedTaxiStatusAnalysis.evaluateAndWriteOutput(outputFolder);
		
	}

}
