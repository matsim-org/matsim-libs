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

package playground.andreas.P2.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants;
import playground.andreas.P2.stats.abtractPAnalysisModules.AbstractPAnalyisModule;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageInVehicleTripTravelTimeSecondsPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageLoadPerDeparturePerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageLoadPerDistancePerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageNumberOfStopsPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageTripDistanceMeterPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.AverageWaitingTimeSecondsPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountCapacityMeterPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountDeparturesPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountPassengerMeterPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountTransfersPerModeModeCombination;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountTripsPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountTripsPerPtModeCombination;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountVehPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountVehicleMeterPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.CountDeparturesWithNoCapacityLeftPerMode;
import playground.andreas.P2.stats.abtractPAnalysisModules.lineSetter.BVGLines2PtModes;
import playground.andreas.P2.stats.abtractPAnalysisModules.lineSetter.PtMode2LineSetter;

/**
 * Plugs in all analysis.
 * 
 * @author aneumann
 *
 */
public class PAnalysisManager implements StartupListener, IterationStartsListener, IterationEndsListener{
	private final static Logger log = Logger.getLogger(PAnalysisManager.class);
	
	private final String pIdentifier;
	private List<AbstractPAnalyisModule> pAnalyzesList = new LinkedList<AbstractPAnalyisModule>();
	private HashMap<String, BufferedWriter> pAnalyis2Writer = new HashMap<String, BufferedWriter>();
	private boolean firstIteration = true;
	private PtMode2LineSetter lineSetter;

	public PAnalysisManager(PConfigGroup pConfig, PtMode2LineSetter lineSetter){
		log.info("enabled");
		this.pIdentifier = pConfig.getPIdentifier();
		if (lineSetter == null) {
			this.lineSetter = new BVGLines2PtModes();
			log.info("using default PtMode2LineSetter " +  this.lineSetter.getClass().getSimpleName());
		} else {
			this.lineSetter = lineSetter;
		}
	}
	@Override
	public void notifyStartup(StartupEvent event) {
		// create all analyzes
		this.pAnalyzesList.add(new CountTripsPerMode());
		this.pAnalyzesList.add(new CountVehPerMode());
		this.pAnalyzesList.add(new CountVehicleMeterPerMode(event.getControler().getNetwork()));
		this.pAnalyzesList.add(new AverageTripDistanceMeterPerMode(event.getControler().getNetwork()));
		this.pAnalyzesList.add(new AverageInVehicleTripTravelTimeSecondsPerMode());
		this.pAnalyzesList.add(new AverageWaitingTimeSecondsPerMode());
		this.pAnalyzesList.add(new AverageNumberOfStopsPerMode());
		this.pAnalyzesList.add(new CountTransfersPerModeModeCombination());
		this.pAnalyzesList.add(new CountTripsPerPtModeCombination());
		this.pAnalyzesList.add(new AverageLoadPerDeparturePerMode());
		this.pAnalyzesList.add(new CountDeparturesWithNoCapacityLeftPerMode());
		this.pAnalyzesList.add(new CountDeparturesPerMode());
		
		CountPassengerMeterPerMode countPassengerMeterPerMode = new CountPassengerMeterPerMode(event.getControler().getNetwork());
		this.pAnalyzesList.add(countPassengerMeterPerMode);
		CountCapacityMeterPerMode countCapacityMeterPerMode = new CountCapacityMeterPerMode(event.getControler().getNetwork());
		this.pAnalyzesList.add(countCapacityMeterPerMode);
		this.pAnalyzesList.add(new AverageLoadPerDistancePerMode(countPassengerMeterPerMode, countCapacityMeterPerMode));
		
		// register all analyzes
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			event.getControler().getEvents().addHandler((EventHandler) ana);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// update pt mode for each line in schedule
		updateLineId2ptModeMap(event.getControler().getScenario().getTransitSchedule());
		updateVehicleTypes(event.getControler().getScenario().getVehicles());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.firstIteration) {
			// create the output folder for this module
			String outFilename = event.getControler().getControlerIO().getOutputPath() + PConstants.statsOutputFolder + PAnalysisManager.class.getSimpleName() + "/";
			new File(outFilename).mkdir();
			
			// create one output stream for each analysis
			for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
				try {
					String moduleOutFilename = outFilename + ana.getName() + ".txt";
					BufferedWriter writer = IOUtils.getBufferedWriter(moduleOutFilename);
					writer.write("# iteration" + ana.getHeader());
					writer.newLine();
					this.pAnalyis2Writer.put(ana.getName(), writer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.firstIteration = false;
		}
		
		// write results to corresponding files
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			BufferedWriter writer = this.pAnalyis2Writer.get(ana.getName());
			try {
				writer.write(event.getIteration() + ana.getResult());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void updateLineId2ptModeMap(TransitSchedule transitSchedule) {
		this.lineSetter.setPtModesForEachLine(transitSchedule, this.pIdentifier);
		HashMap<Id, String> lineIds2ptModeMap = this.lineSetter.getLineId2ptModeMap();
		
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			ana.setLineId2ptModeMap(lineIds2ptModeMap);
		}
	}

	private void updateVehicleTypes(Vehicles vehicles) {
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			ana.updateVehicles(vehicles);
		}		
	}
}
