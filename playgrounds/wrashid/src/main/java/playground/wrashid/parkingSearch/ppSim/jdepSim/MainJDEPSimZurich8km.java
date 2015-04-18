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
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;

public class MainJDEPSimZurich8km {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Todo change these three paths and try run.
		
//		String plansFile="Z:/data/experiments/TRBAug2012/input/census2000v2_ZhCut8km_10pct_ivtch.xml/plans_1.xml";
		String plansFile="Z:/data/experiments/TRBAug2012/input/census2000v2_ZhCut8km_10pct_ivtch.xml.gz";
		String networkFile="H:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml";
		String facilititiesPath="Z:/data/experiments/TRBAug2012/input/facilties_8kmCut_10pct.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(
				"events.xml");
		eventsManager.addHandler(eventsWriter);
		LegHistogram lh=new LegHistogram(300);
		eventsManager.addHandler(lh);
		
		
		eventsManager.resetHandlers(0);
		eventsWriter.init("events.xml");
		

		RunnableMobsim sim = new JDEPSim(scenario, eventsManager);
		//Mobsim sim = new PPSim(scenario, eventsManager);
		
		Message.ttMatrix=new TTMatrixFromStoredTable("c:/tmp2/table3.txt", scenario.getNetwork());
		sim.run();
		eventsManager.finishProcessing();
		LegHistogramChart.writeGraphic(lh, "legHistogram.png");
		lh.write("legHistogram.txt");
		eventsWriter.reset(0);
	}

}

