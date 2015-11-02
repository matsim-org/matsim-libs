/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.internalizationPt.TransferDelayInVehicleHandler;
import playground.ikaddoura.internalizationPt.MarginalCostPricingPtHandler;
import playground.ikaddoura.internalizationPt.TransferDelayWaitingHandler;
import playground.ikaddoura.optimization.io.TextFileWriter;
import playground.ikaddoura.optimization.users.MoneyDetailEventHandler;
import playground.ikaddoura.optimization.users.MoneyEventHandler;

/**
 * 
 * @author ikaddoura
 *
 */
public class DefaultAnalysisMain {
	
	private String configFile = "/Users/Ihab/Desktop/5min_output_config.xml";
	private String outputPath = "/Users/Ihab/Desktop";
	private String eventsFile = "/Users/Ihab/Desktop/ConstFare_5min_maxWelfare_events_noAmounts.xml";
	private String eventsFile_withExtDelayEvents = "/Users/Ihab/Desktop/eventsWithExtDelayEffects.xml";
	
	public static void main(String[] args) {
		
		DefaultAnalysisMain aM = new DefaultAnalysisMain();
		aM.writeExtDelayAndMoneyEvents();
		aM.analyseAmounts();
	}

	private void writeExtDelayAndMoneyEvents() {
		
		// other agentMoneyEvents than external delay costs not allowed!
		Config config = ConfigUtils.loadConfig(configFile);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		TransferDelayInVehicleHandler inVehDelayHandler = new TransferDelayInVehicleHandler(events, scenario);
		events.addHandler(inVehDelayHandler);
		TransferDelayWaitingHandler waitingDelayHandler = new TransferDelayWaitingHandler(events, scenario);
		events.addHandler(waitingDelayHandler);
		MarginalCostPricingPtHandler mcpHandler = new MarginalCostPricingPtHandler(events, scenario);
		events.addHandler(mcpHandler);
		
		EventWriterXML eventWriter = new EventWriterXML(eventsFile_withExtDelayEvents);
		events.addHandler(eventWriter);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		eventWriter.closeFile();
	}


	private void analyseAmounts() {
		
		// other agentMoneyEvents than external delay costs not allowed!
		EventsManager events = EventsUtils.createEventsManager();
		
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		events.addHandler(moneyHandler);
		MoneyDetailEventHandler moneyDetailEventHandler = new MoneyDetailEventHandler();
		events.addHandler(moneyDetailEventHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile_withExtDelayEvents);
		
		TextFileWriter writer = new TextFileWriter();
		writer.wrtieFarePerTime(outputPath, moneyDetailEventHandler.getAvgFarePerTripDepartureTime());		
		writer.writeFareData(outputPath, moneyHandler.getfareDataList());
		writer.writeTripFarePerId(outputPath, moneyDetailEventHandler.getPersonId2fareFirstTrip(), moneyDetailEventHandler.getPersonId2fareSecondTrip());
		System.out.println("total external Costs: " + moneyHandler.getRevenues());
		System.out.println("avg ext. cost per trip: " + moneyHandler.getAverageAmountPerPerson() * 8);
	}
	
}
