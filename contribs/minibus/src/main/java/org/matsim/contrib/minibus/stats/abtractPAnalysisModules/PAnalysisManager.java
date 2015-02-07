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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Plugs in all analysis.
 * 
 * @author aneumann
 *
 */
public final class PAnalysisManager implements StartupListener, IterationStartsListener, IterationEndsListener{
	private final static Logger log = Logger.getLogger(PAnalysisManager.class);
	
	private final String pIdentifier;
	private final List<PAnalysisModule> pAnalyzesList = new LinkedList<>();
	private final HashMap<String, BufferedWriter> pAnalyis2Writer = new HashMap<>();
	private boolean firstIteration = true;
	private final PtMode2LineSetter lineSetter;

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
        this.pAnalyzesList.add(new CountVehicleMeterPerMode(event.getControler().getScenario().getNetwork()));
        this.pAnalyzesList.add(new AverageTripDistanceMeterPerMode(event.getControler().getScenario().getNetwork()));
		this.pAnalyzesList.add(new AverageInVehicleTripTravelTimeSecondsPerMode());
		this.pAnalyzesList.add(new AverageWaitingTimeSecondsPerMode());
		this.pAnalyzesList.add(new AverageNumberOfStopsPerMode());
		this.pAnalyzesList.add(new CountTransfersPerModeModeCombination());
		this.pAnalyzesList.add(new CountTripsPerPtModeCombination());
		this.pAnalyzesList.add(new AverageLoadPerDeparturePerMode());
		this.pAnalyzesList.add(new CountDeparturesWithNoCapacityLeftPerMode());
		this.pAnalyzesList.add(new CountDeparturesPerMode());

        CountPassengerMeterPerMode countPassengerMeterPerMode = new CountPassengerMeterPerMode(event.getControler().getScenario().getNetwork());
		this.pAnalyzesList.add(countPassengerMeterPerMode);
        CountCapacityMeterPerMode countCapacityMeterPerMode = new CountCapacityMeterPerMode(event.getControler().getScenario().getNetwork());
		this.pAnalyzesList.add(countCapacityMeterPerMode);
		this.pAnalyzesList.add(new AverageLoadPerDistancePerMode(countPassengerMeterPerMode, countCapacityMeterPerMode));
		
		// register all analyzes
		for (PAnalysisModule ana : this.pAnalyzesList) {
			event.getControler().getEvents().addHandler(ana);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// update pt mode for each line in schedule
		updateLineId2ptModeMap(event.getControler().getScenario().getTransitSchedule());
		updateVehicleTypes(event.getControler().getScenario().getTransitVehicles());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.firstIteration) {
			// create the output folder for this module
			String outFilename = event.getControler().getControlerIO().getOutputPath() + PConstants.statsOutputFolder + PAnalysisManager.class.getSimpleName() + "/";
			new File(outFilename).mkdir();
			
			// create one output stream for each analysis
			for (PAnalysisModule ana : this.pAnalyzesList) {
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
		for (PAnalysisModule ana : this.pAnalyzesList) {
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
		HashMap<Id<TransitLine>, String> lineIds2ptModeMap = this.lineSetter.getLineId2ptModeMap();
		
		for (PAnalysisModule ana : this.pAnalyzesList) {
			ana.setLineId2ptModeMap(lineIds2ptModeMap);
		}
	}

	private void updateVehicleTypes(Vehicles vehicles) {
		for (PAnalysisModule ana : this.pAnalyzesList) {
			ana.updateVehicles(vehicles);
		}		
	}
}
