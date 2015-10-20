/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * Class to bind the analyze tool (given in the constructor) and the writing
 * tool to the simulation. It works for all analyze tools that extend the
 * abstract analyze tool TtAbstractAnalyzeTool.
 * 
 * @author tthunig
 */
public class TtListenerToBindAndWriteAnalysis implements StartupListener, IterationEndsListener {

	private Scenario scenario;
	private TtAbstractAnalysisTool handler;
	private TtAnalyzedResultsWriter writer;
	
	public TtListenerToBindAndWriteAnalysis(Scenario scenario, TtAbstractAnalysisTool handler) {
		this.scenario = scenario;
		this.handler = handler;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// write analyzed data
		writer.addSingleItToResults(event.getIteration());
		
		// write final analysis for the last iteration
		if (event.getIteration() == scenario.getConfig().controler().getLastIteration()){
			writer.writeFinalResults();
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// add the analysis tool as events handler to the events manager
		event.getControler().getEvents().addHandler(handler);
		
		// prepare the results writer
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "analysis/";
		new File(outputDir).mkdir();
		this.writer = new TtAnalyzedResultsWriter(handler, outputDir);
	}

}
