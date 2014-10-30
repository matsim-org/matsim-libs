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
import org.matsim.core.mobsim.framework.Mobsim;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.DummyTTMatrix;

public class MainJDEPSimEquil {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String plansFile="C:/data/workspace3/matsim/src/test/resources/test/scenarios/equil/plans100.xml";
		String networkFile="C:/data/workspace3/matsim/src/test/resources/test/scenarios/equil/network.xml";
		String facilititiesPath="C:/data/workspace3/matsim/src/test/resources/test/scenarios/equil/facilities.xml";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(
				"events.xml");
		eventsManager.addHandler(eventsWriter);
		LegHistogram lh=new LegHistogram(300);
		eventsManager.addHandler(lh);
		
		
		eventsManager.resetHandlers(0);
		eventsWriter.init("events.xml");
		

		Mobsim sim = new JDEPSim(scenario, eventsManager);
		//Mobsim sim = new PPSim(scenario, eventsManager);
		
		Message.ttMatrix=new DummyTTMatrix();
		sim.run();
		eventsManager.finishProcessing();
		LegHistogramChart.writeGraphic(lh, "legHistogram.png");
		lh.write("legHistogram.txt");
		eventsWriter.reset(0);

	}

}

