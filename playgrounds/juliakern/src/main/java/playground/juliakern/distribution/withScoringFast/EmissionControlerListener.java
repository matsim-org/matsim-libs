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
package playground.juliakern.distribution.withScoringFast;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
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
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import playground.juliakern.distribution.EmActivity;
import playground.juliakern.distribution.EmPerCell;
import playground.juliakern.distribution.GridTools;
import playground.juliakern.newInternalization.IntervalHandler;
import playground.juliakern.responsibilityOffline.EmCarTrip;

import java.util.ArrayList;
import java.util.Map;

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
	IntervalHandler intervalHandler;
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
	Map<Id<Link>, Integer> links2xcells;
	Map<Id<Link>, Integer> links2ycells;

	private int maximalDistance =3;

	private Map<Id, Double> person2causedEmCosts;

	private String eventsFile;

	private ArrayList<EmActivity> activities;

	private ArrayList<EmCarTrip> carTrips;
	Network network;

	private String emissionFile1;

	private Map<Double, ArrayList<EmPerCell>> emissionsPerCell;


	public EmissionControlerListener(Controler controler) {
		this.controler = controler;
        setMinMax(controler.getScenario().getNetwork());
        this.gt = new GridTools(controler.getScenario().getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		
		Scenario scenario = controler.getScenario() ;
		emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		this.emissionFile1="./output/emissionFile.txt";
	}
	
	public EmissionControlerListener(Controler controler, Network network, String eventsFile, String emissionFile){
		this.controler = controler;
		setMinMax(network);
		this.gt = new GridTools(network.getLinks(), xMin, xMax, yMin, yMax);
		this.eventsFile = eventsFile;
		this.network=network;
		this.emissionFile1=emissionFile;
		
		Scenario scenario = controler.getScenario() ;
		emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
	}
	
	private void setMinMax(Network network2) {
		for(Link link: network2.getLinks().values()){
			Double xCoord = link.getCoord().getX();
			Double yCoord = link.getCoord().getY();
			if(xMin>xCoord)xMin=xCoord;
			if(xMax<xCoord)xMax=xCoord;
			if(yMin>yCoord)yMin=yCoord;
			if(yMax<yCoord)yMax=yCoord;
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getConfig().controler().getLastIteration();
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
		Double simulationEndTime = controler.getConfig().qsim().getEndTime();
		simulationEndTime = 60*60*30.0; // TODO
		timeBinSize = simulationEndTime/noOfTimeBins;
		
		logger.info("timebinsize----------------" + timeBinSize);
		intervalHandler = new IntervalHandler(timeBinSize, simulationEndTime, noOfXCells, noOfYCells, links2xcells, links2ycells);
				//new IntervalHandler(timeBinSize, simulationEndTime, noOfXCells, noOfYCells, links2xcells, links2ycells, gt, network );
		
		eventsManager.addHandler(intervalHandler);
		intervalHandler.reset(0);
		logger.info("size durations" + intervalHandler.getDuration().size());
		
		if(eventsFile!=null){
			logger.info("start parsing");
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		}
		logger.info("done parsing");
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		geh = new GeneratedEmissionsHandler(0.0, timeBinSize, links2xcells, links2ycells);
		emissionModule.emissionEventsManager.addHandler(geh);
		
	
		EmissionEventsReader emissionReader = new EmissionEventsReader(emissionModule.emissionEventsManager);
		
//		SimpleWarmEmissionEventHandler weeh = new SimpleWarmEmissionEventHandler();
//		eventsManager.addHandler(weeh);
//		SimpleColdEmissionEventHandler ceeh = new SimpleColdEmissionEventHandler();
//	eventsManager.addHandler(ceeh);
		emissionReader.parse(emissionFile1);
		
		logger.info("done parsing emission file");
//		emissionsPerCell = geh.emissionPerCell;
		
		/*
		 * 		IntervalHandler intervalHandler = new IntervalHandler();
		eventsManager.addHandler(intervalHandler);
		
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
				
		intervalHandler.addActivitiesToTimetables(activities, link2xbin, link2ybin, simulationEndTime);
		intervalHandler.addCarTripsToTimetables(carTrips, simulationEndTime);
		
		eventsManager.removeHandler(intervalHandler);
		 */
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
		
		Double simulationEndTime = 60*60*30.0;
		timeBinSize = simulationEndTime/noOfTimeBins;

		//intervalHandler.calculateDuration(links2xcells, links2ycells, simulationEndTime, timeBinSize, noOfXCells, noOfYCells);
		
		if(intervalHandler.getDuration().size()==0)logger.warn("No activities recorded.");
		
		if(geh.getEmissionsPerCell().isEmpty()) logger.warn("No emissions per cell calculated.");
		
		ResponsibilityUtils reut = new ResponsibilityUtils(maximalDistance , noOfXCells, noOfYCells);
		
		person2causedEmCosts = reut.calculateCausedEmissionCosts(intervalHandler.getDuration(), geh.getEmissionsPerCell());
		
	}
	
	public Map<Id, Double> getCausedEmCosts(){
		return this.person2causedEmCosts;
	}
	
}