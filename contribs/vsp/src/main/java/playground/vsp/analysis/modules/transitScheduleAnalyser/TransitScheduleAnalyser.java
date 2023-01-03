/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.transitScheduleAnalyser;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * Analyzes a given transit schedule and prints some characteristics to file.
 * 
 * @author mkillat
 */
public class TransitScheduleAnalyser extends AbstractAnalysisModule {
	private TransitSchedule schedule;
	private Network network;

	public TransitScheduleAnalyser(Scenario sc) {
		super(TransitScheduleAnalyser.class.getSimpleName());
		this.schedule = sc.getTransitSchedule();
		this.network = sc.getNetwork();
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return new ArrayList<EventHandler>();
	}

	@Override
	public void preProcessData() {	
	}

	@Override
	public void postProcessData() {	
	}

	@Override
	public void writeResults(String outputFolder) {
		TransitScheduleAnalyserToCSVandTEX.transitScheduleAnalyser(this.schedule, this.network, outputFolder);
	}
}