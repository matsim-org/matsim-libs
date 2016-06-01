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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.*;
import org.matsim.core.network.*;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.av.evaluation.flowpaper.TravelTimeAnalysis;
import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class RunAVEvaluation {

	public static void main(String[] args) {

		String networkFile = "D:/runs-svn/avsim/flowpaper/00.0k_AV1.0/00.0k_AV1.0.output_network.xml.gz";
		
//		Wolfsburg:
//		String shapeFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/zones.shp";
//		Map<String,Geometry> geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "plz");

//		Berlin
		String shapeFile = "../../../shared-svn/projects/audi_av/shp/Planungsraum.shp";
		Map<String,Geometry> geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "SCHLUESSEL");
		
		Network network = NetworkUtils.createNetwork() ;
		new MatsimNetworkReader(network).readFile(networkFile);
		
//		List<String> list = Arrays.asList(new String[]{"00.0k_AV1.0", "02.2k_AV1.0", "02.2k_AV1.5","02.2k_AV2.0","04.4k_AV1.0","04.4k_AV1.5",
//				"04.4k_AV2.0","06.6k_AV1.0","06.6k_AV1.5","06.6k_AV2.0","08.8k_AV1.0","08.8k_AV1.5","08.8k_AV2.0","11.0k_AV1.0","11.0k_AV1.5","11.0k_AV2.0"}); 
//		List<String> list = Arrays.asList(new String[]{"11.0k_AV1.0"}); 
//		List<String> list = Arrays.asList(new String[]{"00.0k_AV1.0", "02.2k_AV1.0", "02.2k_AV1.5","02.2k_AV2.0","04.4k_AV1.0","04.4k_AV1.5","04.4k_AV2.0","06.6k_AV1.0"});
		List<String> list = Arrays.asList(new String[]{"06.6k_AV1.5","06.6k_AV2.0","08.8k_AV1.0","08.8k_AV1.5","08.8k_AV2.0","11.0k_AV1.0","11.0k_AV1.5","11.0k_AV2.0"}); 
		
		for (String run : list){
		System.out.println("run "+ run);
		ZoneBasedTaxiCustomerWaitHandler zoneBasedTaxiCustomerWaitHandler = new ZoneBasedTaxiCustomerWaitHandler(network, geo);
		ZoneBasedTaxiStatusAnalysis zoneBasedTaxiStatusAnalysis = new ZoneBasedTaxiStatusAnalysis(network, geo);
		TravelDistanceTimeEvaluator travelDistanceTimeEvaluator = new TravelDistanceTimeEvaluator(network, 0);
		TravelTimeAnalysis timeAnalysis = new TravelTimeAnalysis();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(zoneBasedTaxiCustomerWaitHandler);
		events.addHandler(zoneBasedTaxiStatusAnalysis);
		events.addHandler(travelDistanceTimeEvaluator);
		events.addHandler(timeAnalysis);
		
			String outputFolder = "D:/runs-svn/avsim/flowpaper_0.15fc/"+run+"/";
			String eventsFile = outputFolder+run+".output_events.xml.gz";
			
			new MatsimEventsReader(events).readFile(eventsFile);
			zoneBasedTaxiCustomerWaitHandler.writeCustomerStats(outputFolder);
			zoneBasedTaxiStatusAnalysis.evaluateAndWriteOutput(outputFolder);
			travelDistanceTimeEvaluator.writeTravelDistanceStatsToFiles(outputFolder);
			timeAnalysis.writeStats(outputFolder);
		}
		

		
	}

}
