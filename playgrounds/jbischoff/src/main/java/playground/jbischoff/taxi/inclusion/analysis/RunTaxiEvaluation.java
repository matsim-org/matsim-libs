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

package playground.jbischoff.taxi.inclusion.analysis;

import java.util.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.*;
import org.matsim.core.network.*;
import org.matsim.core.network.io.MatsimNetworkReader;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.av.evaluation.flowpaper.TravelTimeAnalysis;
import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;
import playground.jbischoff.utils.JbUtils;

/**
 * @author jbischoff
 *
 */
public class RunTaxiEvaluation {

	public static void main(String[] args) {

		String networkFile = "D:/runs-svn/barrierFreeTaxi/v2/veh_300/veh_300.output_network.xml.gz";

		// Berlin
		String shapeFile = "../../../shared-svn/projects/audi_av/shp/Planungsraum.shp";
		Map<String, Geometry> geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "SCHLUESSEL");

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		// List<String> list = Arrays.asList(new String[]{"00.0k_AV1.0",
		// "02.2k_AV1.0",
		// "02.2k_AV1.5","02.2k_AV2.0","04.4k_AV1.0","04.4k_AV1.5",
		// "04.4k_AV2.0","06.6k_AV1.0","06.6k_AV1.5","06.6k_AV2.0","08.8k_AV1.0","08.8k_AV1.5","08.8k_AV2.0","11.0k_AV1.0","11.0k_AV1.5","11.0k_AV2.0"});
		// List<String> list = Arrays.asList(new String[]{"11.0k_AV1.0"});
		// List<String> list = Arrays.asList(new String[]{"00.0k_AV1.0",
		// "02.2k_AV1.0",
		// "02.2k_AV1.5","02.2k_AV2.0","04.4k_AV1.0","04.4k_AV1.5","04.4k_AV2.0","06.6k_AV1.0"});
		// List<String> list = Arrays.asList(new
		// String[]{"06.6k_AV1.5","06.6k_AV2.0","08.8k_AV1.0","08.8k_AV1.5","08.8k_AV2.0","11.0k_AV1.0","11.0k_AV1.5","11.0k_AV2.0"});

		// List<String> list =
		// Collections.singletonList("00.0k_AV1.0_flowCap100");

		List<String> normalWaitTimes = new ArrayList<>();
		List<String> barrierfreeWaitTimes = new ArrayList<>();
		
		for (int i = 0; i <= 10; i ++) {
			String runId = "veh250_" + i;
			String dir = "D:/runs-svn/barrierFreeTaxi/v2/"+ runId + "/";
			System.out.println("pop " + runId);
			ZoneBasedBarrierFreeTaxiCustomerWaitHandler zoneBasedTaxiCustomerWaitHandlerBF = new ZoneBasedBarrierFreeTaxiCustomerWaitHandler(
					network, geo, true);
			ZoneBasedBarrierFreeTaxiCustomerWaitHandler zoneBasedTaxiCustomerWaitHandlerNonBF = new ZoneBasedBarrierFreeTaxiCustomerWaitHandler(
					network, geo, false);
			TravelDistanceTimeEvaluator travelDistanceTimeEvaluator = new TravelDistanceTimeEvaluator(network, 0);
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(zoneBasedTaxiCustomerWaitHandlerBF);
			events.addHandler(zoneBasedTaxiCustomerWaitHandlerNonBF);
			events.addHandler(travelDistanceTimeEvaluator);

			String eventsFile = dir + runId + ".output_events.xml.gz";

			new MatsimEventsReader(events).readFile(eventsFile);
			String outDir = dir+runId;
			zoneBasedTaxiCustomerWaitHandlerBF.writeCustomerStats(outDir);
			zoneBasedTaxiCustomerWaitHandlerNonBF.writeCustomerStats(outDir);
			travelDistanceTimeEvaluator.writeTravelDistanceStatsToFiles(outDir);
			normalWaitTimes.add(i + "\t" + zoneBasedTaxiCustomerWaitHandlerNonBF.getOverallAverageWaitTime());
			barrierfreeWaitTimes.add(i + "\t" + zoneBasedTaxiCustomerWaitHandlerBF.getOverallAverageWaitTime());
			
		}
		System.out.println("Barrierfree Taxi waittimes (Fleet, average Wait)");
		for (String s : barrierfreeWaitTimes){
			System.out.println(s);
		}
		System.out.println("Ordinary Taxi waittimes (Fleet, average Wait)");
		for (String s : normalWaitTimes){
			System.out.println(s);
		}

	}

}
