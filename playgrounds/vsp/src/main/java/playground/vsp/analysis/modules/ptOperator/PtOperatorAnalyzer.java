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
package playground.vsp.analysis.modules.ptOperator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 *  Analyzes public transport parameters (such as vehicle-hours, vehicle-km, and number of public vehicles) which are relevant for operator cost calculations.
 * 
 * @author ikaddoura
 *
 */
public class PtOperatorAnalyzer extends AbstractAnalyisModule {
	private final static Logger log = Logger.getLogger(PtOperatorAnalyzer.class);
	private ScenarioImpl scenario;
	private TransitEventHandler transitHandler;
	private int numberOfPtVehicles;
	private double vehicleHours;
	private double vehicleKm;

	public PtOperatorAnalyzer(String ptDriverPrefix) {
		super(PtOperatorAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.transitHandler = new TransitEventHandler(this.scenario.getNetwork(), this.ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.transitHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.numberOfPtVehicles = this.transitHandler.getVehicleIDs().size();
		this.vehicleHours = this.transitHandler.getVehicleHours();
		this.vehicleKm = this.transitHandler.getVehicleKm();
		if (this.numberOfPtVehicles == 0.0) {
			log.warn("Missing transit specific events. No public transport vehicles identified.");
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "ptOperator.txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("total number of pt vehicles: " + this.numberOfPtVehicles);
			bw.newLine();
			bw.write("pt vehicle-hours: " + this.vehicleHours);
			bw.newLine();
			bw.write("pt vehicle-kilometers: " + this.vehicleKm);
			bw.newLine();
			bw.close();

			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
