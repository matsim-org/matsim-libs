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
package playground.vsp.analysis.modules.bvgAna.anaLevel2.vehId2agentIDs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.VehId2PersonEnterLeaveVehicleHandler;

/**
 * 
 * @author ikaddoura, andreas
 *
 */
public class VehId2AgentIdsAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(VehId2AgentIdsAnalyzer.class);
	private ScenarioImpl scenario;
	private VehId2PersonEnterLeaveVehicleHandler enterLeaveHandler;
	
	public VehId2AgentIdsAnalyzer(String ptDriverPrefix) {
		super(VehId2AgentIdsAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.enterLeaveHandler = new VehId2PersonEnterLeaveVehicleHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.enterLeaveHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		
	}

	@Override
	public void postProcessData() {
		
		// ...
		
	}

	@Override
	public void writeResults(String outputFolder) {
		
		// ...
	
	}
	
	/**
	 * @return A set containing all agent ids traveling in a given vehicle at a given time
	 */
	public Set<Id> getAgentIdsInVehicle(Id vehId, double time){
		
		Set<Id> agentIdsInVehicle = new TreeSet<Id>();
		
		ArrayList<PersonEntersVehicleEvent> vem = this.enterLeaveHandler.getVehId2PersonEnterEventMap().get(vehId);
		ArrayList<PersonLeavesVehicleEvent> vlm = this.enterLeaveHandler.getVehId2PersonLeaveEventMap().get(vehId);		
		
		for (Iterator vemIterator = vem.iterator(); vemIterator.hasNext();) {
			PersonEntersVehicleEvent personEntersVehicleEvent = (PersonEntersVehicleEvent) vemIterator.next();
			
			for (Iterator vlmIterator = vlm.iterator(); vlmIterator.hasNext();) {
				PersonLeavesVehicleEvent personLeavesVehicleEvent = (PersonLeavesVehicleEvent) vlmIterator.next();
				
				while(personEntersVehicleEvent.getTime() < personLeavesVehicleEvent.getTime()){
					if(personEntersVehicleEvent.getTime() <= time){
						agentIdsInVehicle.add(personEntersVehicleEvent.getPersonId());
					}
					if(vemIterator.hasNext()){
						personEntersVehicleEvent = (PersonEntersVehicleEvent) vemIterator.next();
					} else {
						break;
					}
				}
				
				if(personLeavesVehicleEvent.getTime() <= personEntersVehicleEvent.getTime()){
					if(personLeavesVehicleEvent.getTime() <= time){
						agentIdsInVehicle.remove(personLeavesVehicleEvent.getPersonId());
					}
				}				
			}			
		}
		
		return agentIdsInVehicle;
	}


}
