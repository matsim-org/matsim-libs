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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * This module requires an emissions events file.
 * 
 * It then provides:
 * - total emissions per emission type
 * - warm emissions per person and emission type
 * - cold emissions per person and emission type
 * - sum of warm and cold emissions per person and emission type
 * 
 * @author ikaddoura, benjamin
 *
 */
public class EmissionsAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(EmissionsAnalyzer.class);
	private MutableScenario scenario;
	private final String emissionEventsFile;
	private EmissionUtils emissionUtils;
	private EmissionsPerPersonWarmEventHandler warmHandler;
	private EmissionsPerPersonColdEventHandler coldHandler;
	private Map<Id<Person>, Map<WarmPollutant, Double>> person2warmEmissions;
	private Map<Id<Person>, Map<ColdPollutant, Double>> person2coldEmissions;
	private Map<Id<Person>, SortedMap<String, Double>> person2totalEmissions;
	private SortedMap<String, Double> totalEmissions;
	
	public EmissionsAnalyzer(String emissionsEventsFile) {
		super(EmissionsAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionsEventsFile;
	}
	
	public void init(MutableScenario scenario) {
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
		
		emissionReader.readFile(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.person2warmEmissions = this.warmHandler.getWarmEmissionsPerPerson();
		this.person2coldEmissions = this.coldHandler.getColdEmissionsPerPerson();
		this.person2totalEmissions = this.emissionUtils.sumUpEmissionsPerId(person2warmEmissions, person2coldEmissions);
		this.totalEmissions = this.emissionUtils.getTotalEmissions(this.person2totalEmissions);
	}

	// TODO: should probably also write out person2totalEmissions...
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
	
	public SortedMap<String, Double> getTotalEmissions() {
		return totalEmissions;
	}

	public Map<Id<Person>, Map<WarmPollutant, Double>> getPerson2warmEmissions() {
		return person2warmEmissions;
	}

	public Map<Id<Person>, Map<ColdPollutant, Double>> getPerson2coldEmissions() {
		return person2coldEmissions;
	}

	public Map<Id<Person>, SortedMap<String, Double>> getPerson2totalEmissions() {
		return person2totalEmissions;
	}
	
}
