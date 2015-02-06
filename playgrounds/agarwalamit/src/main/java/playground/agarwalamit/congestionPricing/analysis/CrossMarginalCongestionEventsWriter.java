/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.analysis;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.vsp.congestion.events.MarginalCongestionEvent;
import playground.vsp.congestion.handlers.MarginalCongestionHandlerImplV4;
import playground.vsp.congestion.handlers.MarginalCostPricingCarHandler;

/**
 * Basically the idea is to read the events file from implementation A and then write congestion events of implementation B.
 * This is required in order to compare the two implementation approach.
 * @author amit
 */

public class CrossMarginalCongestionEventsWriter {

	public CrossMarginalCongestionEventsWriter(Scenario sc) {
		this.sc = sc;
		lastIt = sc.getConfig().controler().getLastIteration();
		this.inputEventsFile = this.sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";
	}

	private String inputEventsFile = "";
	private Scenario sc;
	private int lastIt;
	private MarginalCongestionHandlerImplV4 eh = null;

	/**
	 * @param congestionImpl One for which marginal congestion events need to be evaluated.
	 */
	public void readAndWrite(String congestionImpl){
		
		String outputEventsFile = sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events_"+congestionImpl+".xml.gz";

		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);


		switch (congestionImpl){
//		case "implV3" : eh = new MarginalCongestionHandlerImplV3(manager, (ScenarioImpl)this.sc); break;
		case "implV4" : eh = new MarginalCongestionHandlerImplV4(manager, sc); break;
//		case "implV5" : eh = new MarginalCongestionHandlerImplV5(manager, sc); break;
//		case "implV6" : eh = new MarginalCongestionHandlerImplV6(manager, sc); break;
		default : throw new RuntimeException(congestionImpl+ "is not supported. Available implementations are implV3, implV4, implV5, implV6. Aborting ...");
		}
		manager.addHandler(eh);
		EventWriterXML writer = new EventWriterXML(outputEventsFile);
		manager.addHandler(writer);

		MarginalCostPricingCarHandler moneyEventHandler = new MarginalCostPricingCarHandler(manager,  (ScenarioImpl)this.sc);
		manager.addHandler(moneyEventHandler);

		reader.readFile(this.inputEventsFile);
		
		writer.closeFile();

	}
	
	public List<MarginalCongestionEvent> getCongestionEventsList(){
		return this.eh.getCongestionEventsAsList();
	}

	public static void main(String[] args) {

		String runDir = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/";
		Scenario scenario = LoadMyScenarios.loadScenarioFromOutputDir(runDir);
		
		new CrossMarginalCongestionEventsWriter(scenario).readAndWrite("implV4");;
	}

}
