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
package playground.droeder.realTimeNavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sim2d_v3.events.TickEvent;
import playground.gregor.sim2d_v3.events.TickEventHandler;
import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventsHandler;

/**
 * @author droeder
 *
 */
public class RealTimeNavigation implements XYVxVyEventsHandler, TickEventHandler{
	
	private Map<String, List<Coord>> agentPosition;
	private Map<Id, Map<String, Coord>> agentPosition2;
	private Map<Id, Map<String, List<Coord>>> agentPrefferedSpeed;
	private double currentTime = Double.NEGATIVE_INFINITY;
	
	public RealTimeNavigation(){
		this.agentPosition = new HashMap<String, List<Coord>>();
		this.agentPosition2 = new HashMap<Id, Map<String,Coord>>();
		this.agentPrefferedSpeed = new HashMap<Id, Map<String,List<Coord>>>();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(TickEvent e) {
		System.out.println("tick "  + e.getTime());
		
	}
	
	@SuppressWarnings("serial")
	@Override
	public void handleEvent(XYVxVyEvent e) {
		if(e.getTime() == currentTime){
			
		}else{
			currentTime = e.getTime();
			if(this.agentPosition.size() == 0){
				
			}
			
		}
		System.out.println("xy agent: " + e.getPersonId() + " " + e.getTime());
		if(!this.agentPosition.containsKey(e.getPersonId().toString())){
			this.agentPosition.put(e.getPersonId().toString(), new ArrayList<Coord>());
			this.agentPosition2.put(e.getPersonId(), new HashMap<String, Coord>());
			this.agentPrefferedSpeed.put(e.getPersonId(), new HashMap<String, List<Coord>>());
		}
		final Coord p;
		final Coord v;
		p = new CoordImpl(e.getX(), e.getY());
		v = new CoordImpl(p.getX() + e.getVX(), p.getY() + e.getVY());
		this.agentPosition.get(e.getPersonId().toString()).add(p);
		this.agentPosition2.get(e.getPersonId()).put(String.valueOf(e.getTime()), p);
		this.agentPrefferedSpeed.get(e.getPersonId()).put(String.valueOf(e.getTime()), new ArrayList<Coord>(){{
			add(p);
			add(v);
		}});
	}

	/**
	 * @return the agentPosition
	 */
	public Map<String, List<Coord>> getAgentPosition() {
		return agentPosition;
	}

	/**
	 * @return the agentPosition2
	 */
	public Map<Id, Map<String, Coord>> getAgentPosition2() {
		return agentPosition2;
	}

	
	/**
	 * @return the agentPrefferedSpeed
	 */
	public Map<Id, Map<String, List<Coord>>> getAgentPrefferedSpeed() {
		return agentPrefferedSpeed;
	}

	
}
