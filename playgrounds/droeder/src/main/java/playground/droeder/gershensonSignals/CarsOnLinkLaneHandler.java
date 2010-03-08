/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.gershensonSignals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * @author droeder
 *
 */
public class CarsOnLinkLaneHandler implements LaneEnterEventHandler, LaneLeaveEventHandler, 
			LinkEnterEventHandler, LinkLeaveEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler{
	
	private Map<Id, Integer> vehOnLink = new HashMap<Id, Integer>();
	private Map<Id, Integer> vehInD = new HashMap<Id, Integer>();
	private Map<Id, Map<Id, Integer>> vehOnLinkLanes = new HashMap<Id, Map<Id, Integer>>(); 
	
	private Map<Id, Map<Id, CarLocator>> locateCars =  new HashMap<Id, Map<Id, CarLocator>>();
	private Map<Id, CarLocator> m;
	
	private QNetwork net;
	private double d = 100;
	
	
	public CarsOnLinkLaneHandler(Map<Id, SignalGroupDefinition> groups, double d){
		this.d = d;
		
		for (SignalGroupDefinition sd : groups.values()){
			vehOnLink.put(sd.getLinkRefId(), 0);
			for (Id id : sd.getToLinkIds()){
				if (!vehOnLink.containsKey(id)){
					vehOnLink.put(id, 0);
				}
				if (!vehInD.containsKey(id)){
					vehInD.put(id, 0);
				}
				
			}
			Map<Id, Integer> m = new HashMap<Id, Integer>();
			for (Id id : sd.getLaneIds()){
				m.put(id, 0);
			}
			vehOnLinkLanes.put(sd.getId(), m);
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		iteration = 0;
	}
	@Override
	public void handleEvent(LaneEnterEvent e) {
		Map<Id, Integer> m = vehOnLinkLanes.get(e.getLinkId());
		if (m != null && m.containsKey(e.getLaneId())){
			int i = m.get(e.getLaneId()).intValue();
			i =  i + 1;
			m.put(e.getLaneId(), Integer.valueOf(i));
			vehOnLinkLanes.put(e.getLinkId(), m);
		}
	}

	@Override
	public void handleEvent(LaneLeaveEvent e) {
		Map<Id, Integer> m = vehOnLinkLanes.get(e.getLinkId());
		if (m != null && m.containsKey(e.getLaneId())){
			int i = m.get(e.getLaneId()).intValue();
			i =  i - 1;
			m.put(e.getLaneId(), Integer.valueOf(i));
			vehOnLinkLanes.put(e.getLinkId(), m);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i + 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
		
		m = locateCars.get(e.getLinkId());
		m.put(e.getPersonId(), new CarLocator(net.getLinks().get(e.getLinkId()), e.getTime(), this.d));
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i - 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
		
		m = locateCars.get(e.getLinkId());
		if (m.containsKey(e.getPersonId())){
			m.remove(e.getPersonId());
		}
	}
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i + 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
		
		m = locateCars.get(e.getLinkId());
		if (m.containsKey(e.getPersonId())){
			m.get(e.getPersonId()).agentEndsActivity();
		}else{
			m.put(e.getPersonId(), new CarLocator(net.getLinks().get(e.getLinkId()), e.getTime(), this.d));
			m.get(e.getPersonId()).agentEndsActivity();
		}
	}
	@Override
	public void handleEvent(AgentArrivalEvent e){
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i - 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
		
		m = locateCars.get(e.getLinkId());
		m.get(e.getPersonId()).agentStartsActivity();
	}
	
	public Map<Id, Integer> getVehOnLink(){
		return this.vehOnLink;
	}
	
	public Map<Id, Map<Id, Integer>> getVehOnLinkLanes(){
		return this.vehOnLinkLanes;
	}
	
	public double getVehInD(double time, Id id){
		double i = 0;
		for (CarLocator c : locateCars.get(id).values()){
			if(c.agentIsInD(time) == true){
				i++;
			}
		}
		return i;
	}
	
	public void setQNetwork(QNetwork net){
		this.net = net;
		for(Entry<Id, QLink> e: net.getLinks().entrySet()){
			locateCars.put(e.getKey(), new HashMap<Id, CarLocator>());
		}
	}

}
