/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionControlerListener.java
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
package playground.julia.distribution.disAndScoreByTimeArray;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;

import playground.julia.exposure.GridTools;

/**
 * @author benjamin
 *
 */
public class EmissionControlerListener implements StartupListener, IterationStartsListener, ShutdownListener, ScoringListener{
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	Controler controler;
	String emissionEventOutputFile;
	Integer lastIteration;
	EmissionModule emissionModule;
	EventWriterXML emissionEventWriter;
	IntervalHandler intervalHandler = new IntervalHandler();
	GeneratedEmissionsHandler geh;
	GridTools gt;

	Double xMin = 4452550.25;
	Double xMax = 4479483.33;
	Double yMin = 5324955.00;
	Double yMax = 5345696.81;
	
	Integer noOfXCells = 160;
	Integer noOfYCells = 120;

	Double timeBinSize;
	Integer noOfTimeBins =30;
	Map<Id, Integer> links2xcells;
	Map<Id, Integer> links2ycells;

	private int maximalDistance =3;

	private Map<Id, Double> person2causedEmCosts;

	public EmissionControlerListener() {
	}

	public EmissionControlerListener(Controler controler) {
		this.controler = controler;
		this.gt = new GridTools(controler.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		
		Scenario scenario = controler.getScenario() ;
		emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getLastIteration();
		logger.info("emissions will be calculated for iteration " + lastIteration);
		
		logger.info("mapping links to cells");

		links2xcells = gt.mapLinks2Xcells(noOfXCells);
		links2ycells = gt.mapLinks2Ycells(noOfYCells);
		if(links2xcells.isEmpty() || links2ycells.isEmpty()){
			logger.warn("Something went wrong while mapping links to cells.");
		}
		
//		Scenario scenario = controler.getScenario() ;
//		emissionModule = new EmissionModule(scenario);
//		emissionModule.createLookupTables();
//		emissionModule.createEmissionHandler();
		
		EventsManager eventsManager = controler.getEvents();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		eventsManager.addHandler(intervalHandler);
		
		Double simulationEndTime = controler.getConfig().qsim().getEndTime();
		timeBinSize = simulationEndTime/noOfTimeBins;
		
		geh = new GeneratedEmissionsHandler(0.0, timeBinSize, links2xcells, links2ycells);
		emissionModule.emissionEventsManager.addHandler(geh);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		Integer iteration = event.getIteration();
		geh.reset(iteration);
		
		if(lastIteration.equals(iteration)){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		logger.info("closing emission events file...");
		try {
			emissionEventWriter.closeFile();
		} catch (NullPointerException e) {
			logger.warn("No file to close. Is this intended?");
		}
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
		
		
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		logger.info("before scoring. starting resp calc.");
		
		Double simulationEndTime = controler.getConfig().qsim().getEndTime();
		timeBinSize = simulationEndTime/noOfTimeBins;

		intervalHandler.calculateDuration(links2xcells, links2ycells, simulationEndTime, timeBinSize, noOfXCells, noOfYCells);
		
		if(intervalHandler.getDuration().size()==0)logger.warn("No activities recorded.");
		
		if(geh.getEmissionsPerCell().isEmpty()) logger.warn("No emissions per cell calculated.");
		
		ResponsibilityUtils reut = new ResponsibilityUtils(maximalDistance , noOfXCells, noOfYCells);
		
		person2causedEmCosts = reut.calculateCausedEmissionCosts(intervalHandler.getDuration(), geh.getEmissionsPerCell());
		
	}
	
	public Map<Id, Double> getCausedEmCosts(){
		return this.person2causedEmCosts;
	}
	
}