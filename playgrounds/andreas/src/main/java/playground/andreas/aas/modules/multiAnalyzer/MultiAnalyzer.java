/* *********************************************************************** *
 * project: org.matsim.*
 * MulitAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.andreas.aas.modules.multiAnalyzer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.andreas.aas.modules.AbstractAnalyisModule;
import playground.andreas.aas.modules.multiAnalyzer.emissions.events.EmissionEventsReader;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.EmissionUtils;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.filter.UserGroup;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonColdEventHandler;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonWarmEventHandler;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.zurich.analysis.MoneyEventHandler;

/**
 * WARNING! This is not completely integrated, yet. This class uses the {@link EmissionEventsReader}, which needs to be removed.
 * 
 * 
 * @author aneumann, benjamin
 *
 */
public class MultiAnalyzer extends AbstractAnalyisModule{
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(MultiAnalyzer.class);

	private ScenarioImpl scenario;

	// FROM calculateUserWelfareAndTollRevenueStatisticsByUserGroup
	private MoneyEventHandler moneyEventHandler;
	
	// FROM calculateDistanceTimeStatisticsByUserGroup
	private CarDistanceEventHandler carDistanceEventHandler;
	private TravelTimePerModeEventHandler ttHandler;
	
	// FROM calculateEmissionStatisticsByUserGroup TODO Not integrated, yet
	private EmissionsPerPersonWarmEventHandler warmHandler;
	private EmissionsPerPersonColdEventHandler coldHandler;

	public MultiAnalyzer(String ptDriverPrefix){
		super(MultiAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario){
		this.scenario = scenario;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		
		handler.add(this.moneyEventHandler);
		handler.add(this.carDistanceEventHandler);
		handler.add(this.ttHandler);
		
		return handler;
	}

	@Override
	public void preProcessData() {
			this.moneyEventHandler = new MoneyEventHandler();
			
			this.carDistanceEventHandler = new CarDistanceEventHandler(this.scenario.getNetwork());
			this.ttHandler = new TravelTimePerModeEventHandler();

			// FROM calculateEmissionStatisticsByUserGroup
			// TODO This cannot be integrated since it uses an own implementation of EventsReader. Is this really necessary?
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			
			// THAT EmissionEventsReader is the problem - Could it be solved by putting the handler into another handler, so it can preprocess the events, enrich them and finally pass them down?
			EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
			
			this.warmHandler = new EmissionsPerPersonWarmEventHandler();
			this.coldHandler = new EmissionsPerPersonColdEventHandler();
			
			eventsManager.addHandler(this.warmHandler);
			eventsManager.addHandler(this.coldHandler);
			
			emissionReader.parse("SOME DIRECTORY POINTING TO THE EMISSIONS EVENTS FILE" +  ".emission.events.xml.gz");
	}

	@Override
	public void postProcessData() {
		// nothing to do here?
	}

	@Override
	public void writeResults(String outputFolder) {
		MultiAnalyzerWriter writer = new MultiAnalyzerWriter();
		
		// FROM calculateUserWelfareAndTollRevenueStatisticsByUserGroup
		writer.writeWelfareTollInformation(outputFolder, this.scenario.getConfig(), this.scenario.getPopulation(), this.moneyEventHandler.getPersonId2TollMap());
		
		// FROM calculateDistanceTimeStatisticsByUserGroup
		writer.writeAvgCarDistanceInformation(outputFolder, this.carDistanceEventHandler.getPersonId2CarDistance(), this.carDistanceEventHandler.getUserGroup2carTrips());
		writer.writeDetailedCarDistanceInformation(outputFolder, this.carDistanceEventHandler.getPersonId2CarDistance());
		writer.writeAvgTTInformation(outputFolder, this.ttHandler.getMode2personId2TravelTime(), this.ttHandler.getUserGroup2mode2noOfTrips());
		
		// FROM calculateEmissionStatisticsByUserGroup
		EmissionUtils summarizer = new EmissionUtils();
		Map<Id, SortedMap<String, Double>> person2totalEmissions = summarizer.sumUpEmissionsPerId(this.warmHandler.getWarmEmissionsPerPerson(), this.coldHandler.getColdEmissionsPerPerson());
		SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissions = summarizer.getEmissionsPerGroup(person2totalEmissions);
		writer.writeEmissionInformation(outputFolder, group2totalEmissions);
	}
}