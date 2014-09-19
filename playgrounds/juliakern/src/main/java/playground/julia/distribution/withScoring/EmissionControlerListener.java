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
package playground.julia.distribution.withScoring;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;

import playground.julia.distribution.GridTools;
import playground.julia.distribution.ResponsibilityEvent;
import playground.julia.distribution.withScoringFast.GeneratedEmissionsHandler;

/**
 * @author benjamin
 *
 */
public class EmissionControlerListener implements StartupListener, IterationStartsListener, 
ShutdownListener, ScoringListener, AfterMobsimListener{
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	Controler controler;
	String emissionEventOutputFile;
	Integer lastIteration;
	EmissionModule emissionModule;
	EventWriterXML emissionEventWriter;
	IntervalHandler intervalHandler;
	GeneratedEmissionsHandler geh;
	GridTools gt;

	private ArrayList<ResponsibilityEvent> resp;
//
//	Double xMin = 4452550.25;
//	Double xMax = 4479483.33;
//	Double yMin = 5324955.00;
//	Double yMax = 5345696.81;
//	
	Double xMin = 0.0;
	Double xMax = 20000.0;
	Double yMin = 0.0;
	Double yMax = 12500.0;
	
	Integer noOfXCells = 32;
	Integer noOfYCells = 20;

	Double timeBinSize;
	Integer noOfTimeBins =1;
	Map<Id<Link>, Integer> links2xcells;
	Map<Id<Link>, Integer> links2ycells;

	public EmissionControlerListener(Controler controler) {
		this.controler = controler;
		this.gt = new GridTools(controler.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		this.intervalHandler = new IntervalHandler();
		
		logger.warn(controler.getNetwork().getLinks().size() + " number of links");
		
		Scenario scenario = controler.getScenario() ;
		emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getConfig().controler().getLastIteration();
		logger.info("emissions will be calculated for iteration " + lastIteration);
		
		logger.info("mapping links to cells");

		this.links2xcells = gt.mapLinks2Xcells(noOfXCells);
		this.links2ycells = gt.mapLinks2Ycells(noOfYCells);
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


	public ArrayList<ResponsibilityEvent> getResp() {
		return resp;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
//		logger.info("before scoring. starting resp calc.");
//		
//		Double simulationEndTime = controler.getConfig().qsim().getEndTime();
//		timeBinSize = simulationEndTime/noOfTimeBins;
//
//		this.intervalHandler.addActivitiesToTimetables(links2xcells, links2ycells, simulationEndTime);
//
//		ResponsibilityUtils reut = new ResponsibilityUtils();
//		resp = new ArrayList<ResponsibilityEvent>();
//		
//		if(this.intervalHandler.getActivities().size()==0)logger.warn("No activities recorded.");
//		
//		if(this.geh.getEmissionsPerCell().isEmpty()) logger.warn("No emissions per cell calculated.");
//		
//		reut.addExposureAndResponsibilityBinwise(intervalHandler.getActivities(), geh.getEmissionsPerCell(), resp, timeBinSize, controler.getConfig().qsim().getEndTime());
//		
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		logger.info("before scoring. starting resp calc.");
		
		Double simulationEndTime = controler.getConfig().qsim().getEndTime();
		timeBinSize = simulationEndTime/noOfTimeBins;

		this.intervalHandler.addActivitiesToTimetables(links2xcells, links2ycells, simulationEndTime);

		ResponsibilityUtils reut = new ResponsibilityUtils();
		resp = new ArrayList<ResponsibilityEvent>();
		
		if(this.intervalHandler.getActivities().size()==0)logger.warn("No activities recorded.");
		
		if(this.geh.getEmissionsPerCell().isEmpty()) logger.warn("No emissions per cell calculated.");
		
		reut.addExposureAndResponsibilityBinwise(intervalHandler.getActivities(), geh.getEmissionsPerCell(), resp, timeBinSize, controler.getConfig().qsim().getEndTime());
		
		logger.info("done with resp calc");
	}
	
}