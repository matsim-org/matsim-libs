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
package playground.agarwalamit.analysis.congestion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;

/**
 * Basically the idea is to read the events file from implementation A and then write congestion events of implementation B.
 * This is required in order to compare the two implementation approach.
 * @author amit
 */

public class CrossMarginalCongestionEventsWriter {
	private final String inputEventsFile ;
	private final Scenario sc;
	private final int lastIt;
	private final List<CongestionEvent> conEvents = new ArrayList<>();
	
	public CrossMarginalCongestionEventsWriter(final Scenario sc) {
		this.sc = sc;
		this.lastIt = sc.getConfig().controler().getLastIteration();
		this.inputEventsFile = this.sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";
	}

	/**
	 * @param congestionImpl One for which marginal congestion events need to be evaluated.
	 */
	public void readAndWrite(final String congestionImpl){
		
		String outputEventsFile = sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events_"+congestionImpl+".xml.gz";

		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		EventHandler eh = null;

		switch (congestionImpl){
		case "implV3" : eh = new CongestionHandlerImplV3(manager, this.sc); break;
		case "implV4" : eh = new CongestionHandlerImplV4(manager, sc); break;
//		case "implV6" : eh = new CongestionHandlerImplV6(manager, sc); break;
		default : throw new RuntimeException(congestionImpl+ "is not supported. Available implementations are implV3, implV4, implV6. Aborting ...");
		}
		manager.addHandler(eh);
//		ZZ_TODO : this is stupid, it will write multiple congestion and money events. See EventsFilterAndWriter.java
		EventWriterXML writer = new EventWriterXML(outputEventsFile);
		manager.addHandler(writer);
		manager.addHandler(new CongestionEventHandler() {
			
			@Override
			public void reset(int iteration) {
			}
			
			@Override
			public void handleEvent(CongestionEvent event) {
				conEvents.add(event);
			}
		});
		MarginalCongestionPricingHandler moneyEventHandler = new MarginalCongestionPricingHandler(manager,  (MutableScenario)this.sc);
		manager.addHandler(moneyEventHandler);
		reader.readFile(this.inputEventsFile);
		writer.closeFile();
	}
	
	public List<CongestionEvent> getCongestionEventsList(){
		return conEvents;
	}

	public static void main(String[] args) {
		String runDir = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/";
		Scenario scenario = LoadMyScenarios.loadScenarioFromOutputDir(runDir);
		
		new CrossMarginalCongestionEventsWriter(scenario).readAndWrite("implV4");
	}
}