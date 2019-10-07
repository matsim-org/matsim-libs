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
package playground.vsp.analysis.modules.carDistance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * This module calculates the total car distance for each person
 * as well as the avg. distance per car user and the avg. distance per trip.
 * 
 * @author ikaddoura
 *
 */
public class CarDistanceAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(CarDistanceAnalyzer.class);
	private MutableScenario scenario;
	
	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private CarDistanceEventHandler carDistanceEventHandler;
	private Map<Id<Person>, Double> personId2carDistance;
	private int carTrips;
	private double avgCarDistancePerCarUser_km;
	private double avgCarDistancePerTrip_km;
	
	public CarDistanceAnalyzer() {
		super(CarDistanceAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		this.ptDriverIdAnalyzer.init(scenario);
		this.anaModules.add(ptDriverIdAnalyzer);
		
		this.carDistanceEventHandler = new CarDistanceEventHandler(scenario.getNetwork(), this.ptDriverIdAnalyzer);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();

		// from (sub-)modules
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
		
		// own handler
		allEventHandler.add(this.carDistanceEventHandler);
		
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		log.info("Preprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
	}

	@Override
	public void postProcessData() {
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		// own postProcessing
		double totalCarDistance_km = 0.0;
		int numberOfPersons = 0;
		this.personId2carDistance = this.carDistanceEventHandler.getPersonId2CarDistance();
		this.carTrips = this.carDistanceEventHandler.getCarTrips();
		
		for(Id<Person> personId : this.personId2carDistance.keySet()){
			totalCarDistance_km += this.personId2carDistance.get(personId) / 1000.;
			numberOfPersons++;
		}
		
		this.avgCarDistancePerCarUser_km = totalCarDistance_km / numberOfPersons;
		this.avgCarDistancePerTrip_km = totalCarDistance_km / this.carTrips;
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "carDistances.txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("average car distance [km] per car user : " + this.avgCarDistancePerCarUser_km);
			bw.newLine();
			bw.write("average car distance [km] per trip  " + this.avgCarDistancePerTrip_km);
			bw.newLine();
			bw.newLine();
			
			bw.write("person id \t total car distance [km]");
			bw.newLine();
			
			for(Id<Person> personId : this.personId2carDistance.keySet()){
				Double individualCarDistance_km = this.personId2carDistance.get(personId) / 1000.;
				bw.write(personId.toString() + "\t");
				bw.write(individualCarDistance_km.toString());
				bw.newLine();
			}
			
			bw.close();

			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
