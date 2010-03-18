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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QLinkLanesImpl;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * @author droeder
 *
 */
public class CarsOnLinkLaneHandler implements LaneEnterEventHandler, LaneLeaveEventHandler, 
			LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentWait2LinkEventHandler{
	
	private static final Logger log = Logger
			.getLogger(CarsOnLinkLaneHandler.class);
	
	private Map<Id, Integer> vehOnLink = new HashMap<Id, Integer>();
	private Map<Id, Map<Id, Integer>> vehOnLinkLanes = new HashMap<Id, Map<Id, Integer>>(); 
	
	private Map<Id, Map<Id, CarLocator>> locateCars =  new HashMap<Id, Map<Id, CarLocator>>();
	private Map<Id, CarLocator> m;
	private Map<Id, Double> dForLinks;
	
	private QNetwork qNet;
	private double d;
	private Map<Id, SignalGroupDefinition> groups;

	/*  dMax is the maximum length of d, if d is longer then linkLength d is set to linkLength.
	 */
	public CarsOnLinkLaneHandler(Map<Id, SignalGroupDefinition> groups, double dMax, Network net){
		this.d = dMax;
		this.groups = groups;
		this.reset(0);
		this.checkD(net);
	}
	
	private void checkD(Network net){
		this.dForLinks = new HashMap<Id, Double>();
		for(Link l: net.getLinks().values()){
			if(l.getLength()<d){
				this.dForLinks.put(l.getId(), l.getLength());
			}else{
				this.dForLinks.put(l.getId(), d);
			}
		}
	}
	
	@Override
	public void reset(int iteration) {

		for (SignalGroupDefinition sd : this.groups.values()){
			Map<Id, Integer> m = new HashMap<Id, Integer>();
			for (Id id : sd.getLaneIds()){
				m.put(id, 0);
			}
			vehOnLinkLanes.put(sd.getLinkRefId(), m);
		}
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
		m = locateCars.get(e.getLinkId());
		m.put(e.getPersonId(), new CarLocator(qNet.getLinks().get(e.getLinkId()), e.getTime(), this.dForLinks.get(e.getLinkId())));
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		m = locateCars.get(e.getLinkId());
		if (m.containsKey(e.getPersonId())){
			m.remove(e.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(AgentWait2LinkEvent e) {
		m = locateCars.get(e.getLinkId());
		m.put(e.getPersonId(), new CarLocator(qNet.getLinks().get(e.getLinkId()), e.getTime(), this.dForLinks.get(e.getLinkId())));
		m.get(e.getPersonId()).setEarliestD(e.getTime());
		
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent e){
		m = locateCars.get(e.getLinkId());
		if (m.containsKey(e.getPersonId())){
			m.remove(e.getPersonId());
		}
	}
	
	public Integer getVehOnLinkLanes(Id id){
		Integer i = 0;
		for (Entry<Id, Integer> e : vehOnLinkLanes.get(id).entrySet()){
			i += e.getValue();
		}
		return i;
	}
	
	public double getVehInD(double time, Id linkId){
		double i = 0;
		for (CarLocator c : locateCars.get(linkId).values()){
			if(c.agentIsInD(time) == true){
				i = i+1;
			}
		}
		return i;
	}
	
	public double getVehOnLink(Id linkId){
		double i = 0;
		for (CarLocator c : locateCars.get(linkId).values()){
			i++;
		}
		
		return i;
	}
	
	public void setQNetwork(QNetwork net){
		this.qNet = net;
		for(Entry<Id, QLink> e: net.getLinks().entrySet()){
			locateCars.put(e.getKey(), new HashMap<Id, CarLocator>());
		}
	}

}
