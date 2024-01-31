/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;


/**
 * Calculates at the end of each iteration mode statistics, based on the main mode identifier of a trip chain.
 * For multi-modal trips, this is only as accurate as your main mode identifier.
 * The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author mrieser
 */
public final class ModeStatsControlerListener implements StartupListener, IterationEndsListener {

	private final static String FILENAME_MODESTATS = "modestats";

	private final Population population;

	private final String modeFileName;
	private final String delimiter;

	private final ControllerConfigGroup controllerConfigGroup;

	Map<String,Map<Integer,Double>> modeHistories = new HashMap<>();
	private int minIteration = 0;
	private MainModeIdentifier mainModeIdentifier;
	private Map<String,Double> modeCnt = new TreeMap<>();
	private int firstIteration = -1;

	// Keep all modes encountered so far in a sorted set to ensure output is written for modes sorted by mode.
	private final Set<String> modes = new TreeSet<>();

	private final static Logger log = LogManager.getLogger(ModeStatsControlerListener.class);

	@Inject
	ModeStatsControlerListener(ControllerConfigGroup controllerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
														 GlobalConfigGroup globalConfigGroup, AnalysisMainModeIdentifier mainModeIdentifier) {
		this.controllerConfigGroup = controllerConfigGroup;
		this.population = population1;
		this.modeFileName = controlerIO.getOutputFilename(FILENAME_MODESTATS);
		this.delimiter = globalConfigGroup.getDefaultDelimiter();
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.minIteration = this.controllerConfigGroup.getFirstIteration();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		collectModeShareInfo(event);
		writeOutput(event);
	}

	private void collectModeShareInfo(final IterationEndsEvent event) {
		if (firstIteration < 0) {
			firstIteration = event.getIteration();
		}
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<Trip> trips = TripStructureUtils.getTrips(plan);
			for ( Trip trip : trips ) {
				String mode = this.mainModeIdentifier.identifyMainMode(trip.getTripElements());
				// yy as stated elsewhere, the "computer science" mode identification may not be the same as the "transport planning"
				// mode identification.  Maybe revise.  kai, nov'16

				Double cnt = this.modeCnt.get( mode );
				if (cnt == null) {
					cnt = 0.;
				}
				this.modeCnt.put( mode, cnt + 1 );
			}
		}

		double sum = 0 ;
		for ( Double val : this.modeCnt.values() ) {
			sum += val ;
		}

		// add new modes not encountered in previous iterations
		this.modes.addAll(modeCnt.keySet());

		// calculate and save this iteration's mode shares
		log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
		for ( String mode : modes ) {
			Double cnt = this.modeCnt.getOrDefault(mode, 0.0) ;
			double share = 0. ;
			if ( cnt!=null ) {
				share = cnt/sum;
			}
			log.info("-- mode share of mode " + mode + " = " + share );

			Map<Integer, Double> modeHistory = this.modeHistories.get(mode) ;
			if ( modeHistory == null ) {
				modeHistory = new TreeMap<>() ;
				for (int iter = firstIteration; iter < event.getIteration(); iter++) {
					modeHistory.put(iter, 0.0);
				}
				this.modeHistories.put(mode, modeHistory) ;
			}
			modeHistory.put( event.getIteration(), share ) ;
		}
		modeCnt.clear();
	}

	void writeOutput(IterationEndsEvent event) {
		writeCsv(event);

		if (isWriteGraphs(event)) {
			writePngs();
		}
	}

	private boolean isWriteGraphs(IterationEndsEvent event) {
		return this.controllerConfigGroup.getCreateGraphsInterval() > 0 &&
			event.getIteration() % this.controllerConfigGroup.getCreateGraphsInterval() == 0 &&
			event.getIteration() > this.minIteration;
	}

	private void writePngs() {
		// create chart when data of more than one iteration is available.
		XYLineChart chart = new XYLineChart("Mode Statistics", "iteration", "mode");
		for ( Entry<String, Map<Integer, Double>> entry : this.modeHistories.entrySet() ) {
			String mode = entry.getKey() ;
			Map<Integer, Double> history = entry.getValue() ;
			chart.addSeries(mode, history ) ;
		}
		chart.addMatsimLogo();
		chart.saveAsPng(this.modeFileName + ".png", 800, 600);

		/////// EDIT: STACKED_BAR ///////////////////////////////////////////////////////
		// create chart when data of more than one iteration is available.
		StackedBarChart chart2 = new StackedBarChart("Mode Statistics", "iteration", "share");
		for (Entry<String, Map<Integer, Double>> entry : this.modeHistories.entrySet()) {
			String mode = entry.getKey();
			Map<Integer, Double> history = entry.getValue();
			double[] historyArray = new double[history.size()];
			int i = 0;
			for ( Entry<Integer,Double> entryHistory : history.entrySet() ) {
				historyArray[i] = entryHistory.getValue();
				i++;
			}
			chart2.addSeries(mode, historyArray);
		}
		chart2.addMatsimLogo();
		chart2.saveAsPng(this.modeFileName + "_stackedbar.png", 800, 600);
	}

	private void writeCsv(IterationEndsEvent event) {
		try (BufferedWriter modeOut = IOUtils.getBufferedWriter(this.modeFileName + ".csv")) {
			modeOut.write("iteration");
			for ( String mode : modes ) {
				modeOut.write(this.delimiter);
				modeOut.write(mode);
			}
			modeOut.write("\n");
			for (int iter = firstIteration; iter <= event.getIteration(); iter++) {
				modeOut.write( String.valueOf(iter) ) ;
				for ( String mode : modes ) {
					modeOut.write(this.delimiter + modeHistories.get(mode).get(iter));
				}
				modeOut.write( "\n" ) ;
			}
			modeOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
			throw new UncheckedIOException(e);
		}
	}
}
