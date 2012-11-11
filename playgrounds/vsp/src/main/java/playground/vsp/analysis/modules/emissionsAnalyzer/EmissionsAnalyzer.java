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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.emissionsAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
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

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.emissions.events.EmissionEventsReader;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * This module requires an emissions events file
 * and calculates the total emissions.
 * 
 * @author ikaddoura, benjamin
 *
 */
public class EmissionsAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(EmissionsAnalyzer.class);
	private ScenarioImpl scenario;
	private String emissionEventsFile;
	private EmissionUtils emissionUtils;
	private EmissionsPerPersonWarmEventHandler warmHandler;
	private EmissionsPerPersonColdEventHandler coldHandler;
	private Map<Id, Map<WarmPollutant, Double>> person2warmEmissions;
	private Map<Id, Map<ColdPollutant, Double>> person2coldEmissions;
	private Map<Id, SortedMap<String, Double>> person2totalEmissions;
	private Map<String, Double> totalEmissions;
	
	public EmissionsAnalyzer(String ptDriverPrefix, String emissionsEventsFile) {
		super(EmissionsAnalyzer.class.getSimpleName(), ptDriverPrefix);
		this.emissionEventsFile = emissionsEventsFile;
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new EmissionsPerPersonWarmEventHandler();
		this.coldHandler = new EmissionsPerPersonColdEventHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		// the standard eventsFile is not read in this module
		return handler;
	}

	@Override
	public void preProcessData() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		
		emissionReader.parse(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.person2warmEmissions = this.warmHandler.getWarmEmissionsPerPerson();
		this.person2coldEmissions = this.coldHandler.getColdEmissionsPerPerson();
		this.person2totalEmissions = this.emissionUtils.sumUpEmissionsPerId(person2warmEmissions, person2coldEmissions);
		this.totalEmissions = this.emissionUtils.getTotalEmissions(this.person2totalEmissions);
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "emissions.txt";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for(String pollutant : emissionUtils.getListOfPollutants()){
				bw.write(pollutant + "\t");
			}
			bw.newLine();

			for(String pollutant : this.totalEmissions.keySet()){
				Double pollutantValue = this.totalEmissions.get(pollutant);
				bw.write(pollutantValue.toString() + "\t");
			}
			bw.newLine();
			
			bw.close();
			log.info("Finished writing output to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
