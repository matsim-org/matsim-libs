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
package org.matsim.contrib.signals.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

/**
 * Write a csv file for visualizing signals in via based on the events.
 * 
 * @author tthunig
 *
 */
public class SignalEvents2ViaCSVWriter implements SignalGroupStateChangedEventHandler, IterationEndsListener, IterationStartsListener {

	private static final Logger log = Logger.getLogger(SignalEvents2ViaCSVWriter.class);

	private static final String SIGNAL_ID = "signal id";
	private static final String X_COORD = "x";
	private static final String Y_COORD = "y";
	private static final String TIME = "time";
	private static final String SIGNAL_STATE = "signal state";
	
	private int countWarnings = 0;

	/**
	 * distance between signal (the coordinate that will be drawn) and node in direction of the link
	 */
	private static final double SIGNAL_COORD_NODE_OFFSET = 20.0;
	/**
	 * distance between signal (the coordinate that will be drawn) and link in orthogonal direction to the link
	 */
	private static final double SIGNAL_COORD_LINK_OFFSET = 5.0;

	private BufferedWriter signalsCSVWriter;
	private SignalsData signalsData;
	Scenario scenario;

	@Inject
	public SignalEvents2ViaCSVWriter(Scenario scenario, ControlerListenerManager clm, EventsManager em) {
		this.scenario = scenario;
		clm.addControlerListener(this);
		em.addHandler(this);
	}

	private Map<Id<Signal>, Coord> signal2Coord = new HashMap<>();

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == scenario.getConfig().controler().getFirstIteration()) {
			/*
			 * do all the stuff that is needed only once a simulation: - calculating coordinations for the via file - getting the signals data out of the scenario
			 */
			init();
		}

		String signalCSVFilename = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/signalEvents2Via.csv";

		// log.info("Initializing SignalsCSVWriter ...");
		signalsCSVWriter = IOUtils.getBufferedWriter(signalCSVFilename);
		log.info("Writing signal events of iteration " + event.getIteration() + " as csv file for via to " + signalCSVFilename + " ...");

		// create header
		try {
			signalsCSVWriter.write(SIGNAL_ID + ";" + X_COORD + ";" + Y_COORD + ";" + TIME + ";" + SIGNAL_STATE);
			signalsCSVWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Something went wrong while writing the header of the signals csv file.");
		}
	}

	private void init() {
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

		// calculate coordinates of all signals and remember them
		for (SignalSystemData signalSystem : signalsData.getSignalSystemsData().getSignalSystemData().values()) {
			for (SignalData signalData : signalSystem.getSignalData().values()) {
				Coord signalCoord = calculateSignalCoordinate(signalData);
				signal2Coord.put(signalData.getId(), signalCoord);
			}
		}
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		if (signalsData == null){
			if (countWarnings > 9){
				log.warn("You are using the SignalsModule without Controler. No output is written out. (This warning is only given 10 times.)");
				countWarnings++;
			}
			return;
		}
		SignalGroupData signalGroupData = signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(event.getSignalSystemId()).get(event.getSignalGroupId());
		// write a line for each signal of the group
		for (Id<Signal> signalId : signalGroupData.getSignalIds()) {
			Coord signalCoord = signal2Coord.get(signalId);
			try {
				signalsCSVWriter.write(signalId + ";" + signalCoord.getX() + ";" + signalCoord.getY() + ";" + event.getTime() + ";" + event.getNewState());
				signalsCSVWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
				log.error("Something went wrong while adding a line for the signal state switch of signal " + signalId);
			}
		}
	}

	private Coord calculateSignalCoordinate(SignalData signalData) {
		// log.info("Calculating coordinate for signal " + signalId + " of signal system " + signalSystemId + " ...");

		Id<Link> signalLinkId = signalData.getLinkId();
		Link signalLink = scenario.getNetwork().getLinks().get(signalLinkId);
		Coord toNodeCoord = signalLink.getToNode().getCoord();
		Coord fromNodeCoord = signalLink.getFromNode().getCoord();

		int stepNumber = 0;
		if (signalData.getLaneIds() != null) {
			// if the signal belongs to lanes, different signals may exist for one link -> determine step number for visualization
			Lanes lanes = scenario.getLanes();
			Id<Lane> firstSignalLaneId = (Id<Lane>) signalData.getLaneIds().toArray()[0];
			Lane firstSignalLane = lanes.getLanesToLinkAssignments().get(signalLinkId).getLanes().get(firstSignalLaneId);
			stepNumber = firstSignalLane.getAlignment();
		}

		// calculate delta X and Y depending on the node offset
		double deltaX = 0;
		double deltaY = 0;
		// handle different cases of links
		if (toNodeCoord.getX() == fromNodeCoord.getX()) {
			// vertical link
			deltaX = 0;
			if (toNodeCoord.getY() < fromNodeCoord.getY()) {
				deltaY = 1;
			} else {
				deltaY = -1;
			}

		} else {
			// this case includes the case when the link is horizontal
			double m = (toNodeCoord.getY() - fromNodeCoord.getY()) / (toNodeCoord.getX() - fromNodeCoord.getX());
			deltaX = 1 / (Math.sqrt(1 + m * m));
			if (toNodeCoord.getX() > fromNodeCoord.getX()) {
				// link is oriented to the right -> coordinates has to be shifted to the left
				deltaX *= -1;
			}
			deltaY = m * deltaX;
		}

		// calculate x and y coord where the signal should be drawn
		double x = toNodeCoord.getX() + SIGNAL_COORD_NODE_OFFSET * deltaX + stepNumber * SIGNAL_COORD_LINK_OFFSET * deltaY;
		double y = toNodeCoord.getY() + SIGNAL_COORD_NODE_OFFSET * deltaY - stepNumber * SIGNAL_COORD_LINK_OFFSET * deltaX;

		// log.info("... done!");
		return new Coord(x, y);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// close the stream of this iteration
		try {
			signalsCSVWriter.flush();
			signalsCSVWriter.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			log.error("Something went wrong while closing the signals csv writer stream.");
		}
		log.info("... done for this iteration");
	}

}
