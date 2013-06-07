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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.PtRoutes2PaxAnalysis;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.PtRoutes2PaxAnalysisHandler;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.TransitLineContainer;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.TransitRouteContainer;

/**
 * @author sfuerbas after droeder
 *
 */

public class PtLines2PaxAnalysis extends AbstractAnalyisModule {

	private static final Logger log = Logger
			.getLogger(PtLines2PaxAnalysis.class);
	private PtLines2PaxAnalysisHandler handler;
	private Map<Id, TransitLine> lines;
	
	public PtLines2PaxAnalysis(Map<Id, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices) {
		super(PtLines2PaxAnalysis.class.getSimpleName());
		this.handler = new PtLines2PaxAnalysisHandler(interval, maxSlices, lines, vehicles);
		this.lines = lines;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postProcessData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeResults(String outputFolder) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
