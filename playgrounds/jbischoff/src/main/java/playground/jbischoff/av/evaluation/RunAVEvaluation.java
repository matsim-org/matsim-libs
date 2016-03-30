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

import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class RunAVEvaluation {

	public static void main(String[] args) {

		String networkFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/scenario/networkpt-feb.xml.gz";
		String shapeFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/zones.shp";
		Map<String,Geometry> geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "plz");
		Network network = NetworkUtils.createNetwork() ;
		new MatsimNetworkReader(network).readFile(networkFile);
		
		for (int i = 21; i<27; i++){
		
		ZoneBasedTaxiCustomerWaitHandler zoneBasedTaxiCustomerWaitHandler = new ZoneBasedTaxiCustomerWaitHandler(network, geo);
		ZoneBasedTaxiStatusAnalysis zoneBasedTaxiStatusAnalysis = new ZoneBasedTaxiStatusAnalysis(network, geo);
		TravelDistanceTimeEvaluator travelDistanceTimeEvaluator = new TravelDistanceTimeEvaluator(network, 0);
		EventsManager events = EventsUtils.createEventsManager();
//		events.addHandler(zoneBasedTaxiCustomerWaitHandler);
//		events.addHandler(zoneBasedTaxiStatusAnalysis);
		events.addHandler(travelDistanceTimeEvaluator);
			String eventsFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/"+i+"000/"+i+"k_events.out.xml.gz";
			String outputFolder = "../../../shared-svn/projects/vw_rufbus/av_simulation/"+i+"000/";
			
			new MatsimEventsReader(events).readFile(eventsFile);
//			zoneBasedTaxiCustomerWaitHandler.writeCustomerStats(outputFolder);
//			zoneBasedTaxiStatusAnalysis.evaluateAndWriteOutput(outputFolder);
			travelDistanceTimeEvaluator.writeTravelDistanceStatsToFiles(outputFolder);
		}
		

		
	}

}
