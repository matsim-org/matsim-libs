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

package playground.benjamin.scenarios.munich.exposure;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;


/**
 * @author benjamin
 *
 */
public class InternalizeEmissionResponsibilityControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(InternalizeEmissionResponsibilityControlerListener.class);

	MatsimServices controler;
	EmissionModule emissionModule;
	EmissionResponsibilityCostModule emissionCostModule;
	String emissionEventOutputFile;
	EventWriterXML emissionEventWriter;
	EmissionResponsibilityInternalizationHandler emissionInternalizationHandler;
	
	int iteration;
	int firstIt;
	int lastIt;


	private Double timeBinSize; // = 60.*60.;

	private int noOfXCells; // = 160;

	private int noOfYCells; // = 120;

// TODO remove parameter
//	static double xMin = 4452550.25;
//	static double xMax = 4479483.33;
//	static double yMin = 5324955.00;
//	static double yMax = 5345696.81;
	
	private int noOfTimeBins; // = 30;
	
	private Map<Id<Link>, Integer> links2xCells;
	private Map<Id<Link>, Integer> links2yCells;
	
	private IntervalHandler intervalHandler;

	private ResponsibilityGridTools responsibilityGridTools;

	


	public InternalizeEmissionResponsibilityControlerListener(EmissionModule emissionModule, EmissionResponsibilityCostModule emissionCostModule, ResponsibilityGridTools rgt, Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells) {
		this.noOfTimeBins = rgt.getNoOfTimeBins();
		this.timeBinSize = rgt.getTimeBinSize();
		this.noOfXCells = rgt.getNoOfXCells();
		this.noOfYCells = rgt.getNoOfYCells();
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.responsibilityGridTools = rgt;
		this.links2xCells = links2xCells;
		this.links2yCells = links2yCells;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getServices();

		EventsManager eventsManager = controler.getEvents();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());			
		
		responsibilityGridTools.init(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		
		Double simulationEndtime = controler.getConfig().qsim().getEndTime();
		intervalHandler = new IntervalHandler(timeBinSize, simulationEndtime, noOfXCells, noOfYCells, links2xCells, links2yCells);
		eventsManager.addHandler(intervalHandler);
		
		firstIt = controler.getConfig().controler().getFirstIteration();
		lastIt = controler.getConfig().controler().getLastIteration();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		iteration = event.getIteration();

		logger.info("creating new emission internalization handler...");
		emissionInternalizationHandler = new EmissionResponsibilityInternalizationHandler(controler, emissionCostModule);
		logger.info("adding emission internalization module to emission events stream...");
		emissionModule.getEmissionEventsManager().addHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
		

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		logger.info("removing emission internalization module from emission events stream...");
		emissionModule.getEmissionEventsManager().removeHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			logger.info("removing emission events writer from emission events stream...");
			emissionModule.getEmissionEventsManager().removeHandler(emissionEventWriter);
			logger.info("closing emission events file...");
			emissionEventWriter.closeFile();
		}
		
		logger.info("calculating relative duration factors from this/last iteration...");
		// calc relative duration factors/density from last iteration
		responsibilityGridTools.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());
		logger.info("done calculating relative duration factors");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}

}