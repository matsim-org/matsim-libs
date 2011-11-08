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
package playground.benjamin.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author benjamin
 *
 */
public class EmissionControlerListener implements StartupListener, IterationStartsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	Controler controler;
	String emissionEventOutputFile;
	Integer lastIteration;
	EmissionHandler emissionHandler;

	public EmissionControlerListener() {
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getLastIteration();
		emissionHandler = new EmissionHandler();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();
		
		if(lastIteration.equals(iteration)){
			EventsManager eventsManager = controler.getEvents();
			Network network = controler.getScenario().getNetwork();
			
			String outputPath = controler.getControlerIO().getIterationPath(iteration) + "/";
			String runId = controler.getConfig().controler().getRunId();
			
			if(runId != null){
				emissionEventOutputFile = outputPath + runId + "." + iteration + ".emission.events.xml.gz"; 
			} else {
				emissionEventOutputFile = outputPath + iteration + ".emission.events.xml.gz";
			}
			
			emissionHandler.installEmissionEventHandler(network, eventsManager, emissionEventOutputFile);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		EventWriterXML emissionEventWriter = emissionHandler.getEmissionEventWriter();
		emissionEventWriter.closeFile();
		logger.info("Vehicle-specific warm emission calculation was not possible in " + WarmEmissionAnalysisModule.getVehInfoWarnCnt() + " cases.");
		logger.info("Terminated. Output can be found in " + emissionEventOutputFile);
	}
}
