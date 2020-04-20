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
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * This module calculates the public transport parameters vehicle-hours, vehicle-km, number of public vehicles.
 * These parameters can be used for operator cost calculations.
 *  
 * @author ikaddoura
 *
 */
public class PtOperatorAnalyzer extends AbstractAnalysisModule {
	private final static Logger log = Logger.getLogger(PtOperatorAnalyzer.class);
	private MutableScenario scenario;
	
	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private TransitEventHandler transitHandler;
	private int numberOfPtVehicles;
	private double vehicleHours;
	private double vehicleKm;

	public PtOperatorAnalyzer() {
		super(PtOperatorAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.anaModules.clear();
		this.ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		this.ptDriverIdAnalyzer.init(scenario);
		this.anaModules.add(ptDriverIdAnalyzer);		
		
		this.transitHandler = new TransitEventHandler(this.scenario.getNetwork(), this.ptDriverIdAnalyzer);
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
		allEventHandler.add(this.transitHandler);
				
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
