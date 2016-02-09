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
package org.matsim.contrib.signals.data;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.Lanes;

/**
 * Class to write the (x,y,t) signals csv file for via. 
 * 
 * @author tthunig
 *
 */
public class SignalsViaCSVWriter {

	private static final Logger log = Logger.getLogger(SignalsViaCSVWriter.class);
	
	private static final String SIGNAL_ID = "signal id";
	private static final String X_COORD = "x";
	private static final String Y_COORD = "y";
	private static final String TIME = "time";
	private static final String SIGNAL_STATE = "signal state";
	private static final String SIGNAL_STATE_GREEN = "GREEN";
	private static final String SIGNAL_STATE_RED = "RED";	
		
	/**
	 * distance between signal (the coordinate that will be drawn) 
	 * and node in direction of the link
	 */
	private static final double SIGNAL_COORD_NODE_OFFSET = 20.0;
	/**
	 * distance between signal (the coordinate that will be drawn) 
	 * and link in orthogonal direction to the link
	 */
	private static final double SIGNAL_COORD_LINK_OFFSET = 5.0;

	private SignalsData signalsData;
	private Scenario scenario;

	public SignalsViaCSVWriter(Scenario scenario){
		this.scenario = scenario;
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
	}
	
	public void writeSignalsCSV(String filename){		
		try{
//			log.info("Initializing SignalsCSVWriter ...");
			BufferedWriter signalsCSVWriter = IOUtils.getBufferedWriter( filename );
			log.info("Writing signals csv data for via to " + filename + " ...");
			
			// create header
			signalsCSVWriter.write(SIGNAL_ID + ";" + X_COORD + ";" + Y_COORD + ";" + TIME + ";" + SIGNAL_STATE);
			signalsCSVWriter.newLine();
			
			// iterate over all signal systems
			for (SignalSystemData signalSystem : signalsData.getSignalSystemsData().getSignalSystemData().values()){
				SignalSystemControllerData signalSystemControl = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(signalSystem.getId());
				// iterate over all signal plans. mostly, only one exists (with id 1)
				for (SignalPlanData signalPlan : signalSystemControl.getSignalPlanData().values()) {
					// iterate over all signal groups of the signal system
					for (SignalGroupData signalGroup : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						SignalGroupSettingsData signalGroupSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(signalGroup.getId());
						// iterate over all signals of the signal group
						for (Id<Signal> signalId : signalGroup.getSignalIds()) {
							SignalData signal = signalSystem.getSignalData().get(signalId);
							Coord signalCoord = calculateSignalCoordinate(signalSystem.getId(), signal.getId());

							// create lines for every signal state switch during
							// the qsim running time
							Double time = signalPlan.getStartTime();
							if (time == null) {
								// use start time of the simulation
								time = scenario.getConfig().qsim().getStartTime() + signalPlan.getOffset();
							}
//							log.info("Writing signal states for signal " + signal.getId() + " for the whole simulation time ...");
							Double endTime = signalPlan.getEndTime();
							if (endTime == null) {
								// use end time of the simulation
								endTime = scenario.getConfig().qsim().getEndTime();
							}
							while (time <= endTime) {
								signalsCSVWriter.write(signal.getId() + ";" + signalCoord.getX() + ";" + signalCoord.getY() + ";" + (time + signalGroupSetting.getOnset()) + ";" + SIGNAL_STATE_GREEN);
								signalsCSVWriter.newLine();
								signalsCSVWriter.write(signal.getId() + ";" + signalCoord.getX() + ";" + signalCoord.getY() + ";" + (time + signalGroupSetting.getDropping()) + ";" + SIGNAL_STATE_RED);
								signalsCSVWriter.newLine();

								time += signalPlan.getCycleTime();
							}
//							log.info("... done!");
						}
					}
				}
			}
			
			signalsCSVWriter.flush();
			signalsCSVWriter.close();
			log.info("... done!");
		}
		catch(Exception e){ 
			e.printStackTrace(); 
		}
	}
	
	private Coord calculateSignalCoordinate(Id<SignalSystem> signalSystemId, Id<Signal> signalId){
//		log.info("Calculating coordinate for signal " + signalId + " of signal system " + signalSystemId + " ...");
		
		SignalData signalData = signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId).getSignalData().get(signalId);
		Id<Link> signalLinkId = signalData.getLinkId();
		Link signalLink = scenario.getNetwork().getLinks().get(signalLinkId);
		Coord toNodeCoord = signalLink.getToNode().getCoord();
		Coord fromNodeCoord = signalLink.getFromNode().getCoord();
		
		int stepNumber = 0;
		if (signalData.getLaneIds() != null){
			// if the signal belongs to lanes, different signals may exist for one link -> determine step number for visualization
			Lanes lanes = scenario.getLanes();
			Id<Lane> firstSignalLaneId = (Id<Lane>) signalData.getLaneIds().toArray()[0];
			Lane firstSignalLane = lanes.getLanesToLinkAssignments().get(signalLinkId).getLanes().get(firstSignalLaneId);
			stepNumber = firstSignalLane.getAlignment();
		}
		
		//calculate delta X and Y depending on the node offset
		double deltaX = 0;
		double deltaY = 0;
		// handle different cases of links
		if (toNodeCoord.getX() == fromNodeCoord.getX()){
			// vertical link 
			deltaX = 0;
			if (toNodeCoord.getY() < fromNodeCoord.getY()){
				deltaY = -SIGNAL_COORD_NODE_OFFSET;
			} else {
				deltaY = SIGNAL_COORD_NODE_OFFSET;
			}
			
		} else {
			// this case includes the case when the link is horizontal
			double m = (toNodeCoord.getY() - fromNodeCoord.getY()) / (toNodeCoord.getX() - fromNodeCoord.getX());
			deltaX = SIGNAL_COORD_NODE_OFFSET / (Math.sqrt(1 + m * m));
			deltaY = m * deltaX;
		}
		
		// calculate x and y coord where the signal should be drawn
		double x = toNodeCoord.getX() - deltaX + stepNumber * deltaY * (SIGNAL_COORD_LINK_OFFSET / SIGNAL_COORD_NODE_OFFSET);
		double y = toNodeCoord.getY() - deltaY - stepNumber * deltaX * (SIGNAL_COORD_LINK_OFFSET / SIGNAL_COORD_NODE_OFFSET);
		
//		log.info("... done!");
		return new Coord(x, y);
	}
}
