/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;

public class MainJDEPSimZurich30km {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Todo change these three paths and try run.

		// String
		// plansFile="Z:/data/experiments/TRBAug2012/input/census2000v2_ZhCut8km_10pct_ivtch.xml/plans_1.xml";
		String plansFile = "c:/data/parkingSearch/zurich/ktiRun24/output_plans.xml.gz";
		String networkFile = "c:/data/parkingSearch/zurich/ktiRun24/output_network.xml.gz";
		String facilititiesPath = "c:/data/parkingSearch/zurich/ktiRun24/output_facilities.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		String outputFolder="C:/data/parkingSearch/psim/zurich/output/";
		
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(outputFolder + "events.xml.gz");
		eventsManager.addHandler(eventsWriter);
		LegHistogram lh = new LegHistogram(300);
		eventsManager.addHandler(lh);

		eventsManager.resetHandlers(0);
		eventsWriter.init(outputFolder + "events.xml.gz");

		RunnableMobsim sim = new JDEPSim(scenario, eventsManager);
		// Mobsim sim = new PPSim(scenario, eventsManager);

		Message.ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.300secBin.ttMatrix.txt", scenario.getNetwork());
		sim.run();
		eventsManager.finishProcessing();
		LegHistogramChart.writeGraphic(lh, outputFolder + "legHistogram_all.png");
		LegHistogramChart.writeGraphic(lh, outputFolder + "legHistogram_car.png", TransportMode.car);
		LegHistogramChart.writeGraphic(lh, outputFolder + "legHistogram_pt.png", TransportMode.pt);
		LegHistogramChart.writeGraphic(lh, outputFolder + "legHistogram_ride.png", TransportMode.ride);
		LegHistogramChart.writeGraphic(lh, outputFolder + "legHistogram_walk.png", TransportMode.walk);
		eventsWriter.reset(0);
	}

}
