/* *********************************************************************** *
 * project: org.matsim.*
 * CAParkingLot.java
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

package playground.christoph.mobsim.ca;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class CAParkingLot {

//    properties (Access = private)
//    size;
//    agents = Agent.empty(0,0);
//end

	private final Id id;
	private final CACell cell;
	private int size;
	private final Coord coord;
	private final List<CAAgent> parkingAgents;
	private final Deque<CAAgent> departingAgents;
	
//methods
//    function this = ParkingLot(id, position_x, position_y, size)  
//        this = this@SpatialElement(id, position_x, position_y); 
//        this.size = size;
//    end 
	public CAParkingLot(Id id, CACell cell, int size, Coord coord) {
		this.id = id;
		this.cell = cell;
		this.size = size;
		this.coord = coord;
		this.parkingAgents = new ArrayList<CAAgent>();
		this.departingAgents = new LinkedList<CAAgent>();
	}

	public Id getId() {
		return this.getId();
	}
	
	public CACell getCell() {
		return this.cell;
	}
	
//    function [isFree] = isFree(this)
//        isFree = (this.size > length(this.agents));
//    end
	public boolean isFree() {
		return this.size > (this.parkingAgents.size() + this.departingAgents.size());
	}
	
//    function [isEmpty] = isEmpty(this)
//        isEmpty = isempty(this.agents);
//    end
	public boolean isEmpty() {
		return this.parkingAgents.isEmpty() && this.departingAgents.isEmpty();
	}
	
//    function [add] = increaseSizeBy(this, addedSpaces)
//        this.size = this.size + addedSpaces;
//        add = true;
//    end
	@Deprecated //??? do we need/want this?
	public boolean increaseSizeBy(int addSpaces) {
		this.size += addSpaces;
		return true;
	}
	
//    function addAgent(this, agent)
//        this.agents(end + 1) = agent;
//    end
	public void addAgent(CAAgent agent) {
		this.parkingAgents.add(agent);
	}
	
//    function [agents] = getAgents(this)
//        agents = this.agents;
//    end
	public List<CAAgent> getParkingAgents() {
		return this.parkingAgents;
	}
	
	public Deque<CAAgent> getDepartingAgents() {
		return this.departingAgents;
	}
	
//    function [size] = getSize(this)
//        size = this.size;
//    end
	public int getSize() {
		return this.size;
	}
	
	public Coord getCoord() {
		return this.coord;
	}
	
//    function [occupied] = getNrOccupiedSpaces(this)
//        occupied = length(this.agents);
//    end
	public int getNrOccupiedSpaces() {
		return this.parkingAgents.size();
	}
	
//    function [removedAgents] = handleAllAgents(this, currentTime)
//        removedAgents = Agent.empty(0,0);
//        removedAgentsIndexes = [];
//        for i=1:length(this.agents)
//            agent = this.agents(i);
//            if (agent.leaveParkingLot(currentTime)) 
//                removedAgentsIndexes = [this.getAgentIndex(agent) removedAgentsIndexes];
//                removedAgents = [agent removedAgents];
//            end
//        end
//        this.agents(removedAgentsIndexes) = [];
//    end   
//end
	public List<CAAgent> handleAllAgents(double currentTime) {
		List<CAAgent> removedAgents = new ArrayList<CAAgent>();
		
		Iterator<CAAgent> iter = this.parkingAgents.iterator();
		while (iter.hasNext()) {
			CAAgent agent = iter.next();
			if (agent.leaveParkingLot(currentTime)) {
				removedAgents.add(agent);
				this.departingAgents.add(agent);
				iter.remove();
			}
		}
		
		return removedAgents;
	}
	
//methods (Access = private)
//    function index = getAgentIndex(this, agent)
//        index = find(this.agents == agent);
//    end
//end
//	public int getAgentIndex(MobsimAgent agent) {
//		return this.parkingAgents.indexOf(agent);
//	}
}