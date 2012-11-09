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
package playground.vsp.analysis.modules.level1.enterLeaveVehicle2Activity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * This module collects all <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEvent</code> with their
 * corresponding <code>ActivityEndEvent</code> and <code>ActivityStartEvent</code> ignoring <code>pt interaction</code> events
 * not differentiating between public and private vehicles.
 * 
 * @author ikaddoura
 *
 */
public class EnterLeaveVehicle2ActivityAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(EnterLeaveVehicle2ActivityAnalyzer.class);
	private ScenarioImpl scenario;
	private EnterLeaveVehicle2ActivityHandler enterLeaveHandler;
	private Map<PersonEntersVehicleEvent, ActivityEndEvent> enterVehicleEvent2ActivityEndEvent;
	private Map<PersonLeavesVehicleEvent, ActivityStartEvent> leaveVehicleEvent2ActivityStartEvent;
			
	public EnterLeaveVehicle2ActivityAnalyzer(String ptDriverPrefix) {
		super(EnterLeaveVehicle2ActivityAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.enterLeaveHandler = new EnterLeaveVehicle2ActivityHandler(this.ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.enterLeaveHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.enterVehicleEvent2ActivityEndEvent = this.enterLeaveHandler.getPersonEntersVehicleEvent2ActivityEndEvent();
		this.leaveVehicleEvent2ActivityStartEvent = this.enterLeaveHandler.getPersonLeavesVehicleEvent2ActivityStartEvent();
	}

	@Override
	public void writeResults(String outputFolder) {

		for (PersonEntersVehicleEvent enterEvent : this.enterVehicleEvent2ActivityEndEvent.keySet()){
			ActivityEndEvent actend = this.enterVehicleEvent2ActivityEndEvent.get(enterEvent);
			log.info(enterEvent.getPersonId() + " enters vehicle " + enterEvent.getVehicleId() + " (Time:" + enterEvent.getTime() + ") after " + actend.getPersonId() + " ends activity " + actend.getActType() + " (Time: "+ actend.getTime()+")");
		}
		
		// ...
		
	}

	public Map<PersonEntersVehicleEvent, ActivityEndEvent> getEnterVehicleEvent2ActivityEndEvent() {
		return enterVehicleEvent2ActivityEndEvent;
	}

	public Map<PersonLeavesVehicleEvent, ActivityStartEvent> getLeaveVehicleEvent2ActivityStartEvent() {
		return leaveVehicleEvent2ActivityStartEvent;
	}

}
