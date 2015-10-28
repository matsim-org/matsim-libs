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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.bvgAna.anaLevel1.vehId2PersonEntersLeavesVehicle;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author ikaddoura
 *
 */
public class VehId2PersonEnterLeaveVehicleAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(VehId2PersonEnterLeaveVehicleAnalyzer.class);
	private MutableScenario scenario;
	
	private VehId2PersonEnterLeaveVehicleHandler handler;
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> vehId2PersonEnterEvent;
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> vehId2PersonLeaveEvent;
	
	public VehId2PersonEnterLeaveVehicleAnalyzer() {
		super(VehId2PersonEnterLeaveVehicleAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		this.handler = new VehId2PersonEnterLeaveVehicleHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();
		allEventHandler.add(this.handler);
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		
	}

	@Override
	public void postProcessData() {
		this.vehId2PersonEnterEvent = this.handler.getVehId2PersonEnterEventMap();
		this.vehId2PersonLeaveEvent = this.handler.getVehId2PersonLeaveEventMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getVehId2PersonEnterEvent() {
		return vehId2PersonEnterEvent;
	}

	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getVehId2PersonLeaveEvent() {
		return vehId2PersonLeaveEvent;
	}
	
}
