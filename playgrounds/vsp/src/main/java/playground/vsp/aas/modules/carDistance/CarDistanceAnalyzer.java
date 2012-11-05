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
package playground.vsp.aas.modules.carDistance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.aas.modules.AbstractAnalyisModule;

/**
 * @author ikaddoura
 *
 */
public class CarDistanceAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(CarDistanceAnalyzer.class);
	private ScenarioImpl scenario;
	
	private CarDistanceEventHandler carDistanceEventHandler;
	private Map<Id, Double> personId2carDistance;
	private int carTrips;
	private double avgCarDistancePerCarUser_km;
	private double avgCarDistancePerTrip_km;
	
	public CarDistanceAnalyzer(String ptDriverPrefix) {
		super(CarDistanceAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		
		this.carDistanceEventHandler = new CarDistanceEventHandler(scenario.getNetwork(), this.ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.carDistanceEventHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		double totalCarDistance_km = 0.0;
		int numberOfPersons = 0;
		this.personId2carDistance = this.carDistanceEventHandler.getPersonId2CarDistance();
		this.carTrips = this.carDistanceEventHandler.getCarTrips();
		
		for(Id personId : this.personId2carDistance.keySet()){
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
			
			for(Id personId : this.personId2carDistance.keySet()){
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
