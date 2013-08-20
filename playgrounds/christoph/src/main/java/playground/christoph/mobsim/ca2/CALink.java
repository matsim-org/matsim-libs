/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalQLinkExtension.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca2;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.utils.geometry.CoordImpl;

public class CALink {

	private final Link link;
	private final CANode toNode;
	private final double spatialResolution;
	private final double timeStep;
	private final Random random;
	
	private final double minSpeed;
	private final double maxSpeed;
	
	// cannot be final since it is replaced in the parallel implementation
	private CASimEngine simEngine;
	
	private Cell[] cells;

	/*
	 * Is set to "true" if the MultiModalQLinkExtension has active Agents.
	 */
	protected AtomicBoolean isActive = new AtomicBoolean(false);

	private final Deque<MobsimAgent> waitingAfterActivityAgents = new LinkedList<MobsimAgent>();
	
	private final Map<Id, Double> agentSpeeds = new HashMap<Id, Double>();
	
	/*
	 * Index of the first cells from which a vehicle can leave the link within a single time step.
	 */
	private int firstPossibleOutCellIndex;
	
	public CALink(Link link, CASimEngine simEngine, CANode multiModalQNodeExtension, double spatialResolution,
			double timeStep) {
		this.link = link;
		this.simEngine = simEngine;
		this.toNode = multiModalQNodeExtension;
		this.spatialResolution = spatialResolution;
		this.timeStep = timeStep;
		
		this.minSpeed = 0.0;
		this.maxSpeed = link.getFreespeed();
		
		this.random = MatsimRandom.getLocalInstance();
		
		createCells();
	}

	private void createCells() {

		double linkLength = this.link.getLength();
		int numberOfCells = (int) Math.ceil(linkLength / this.spatialResolution);
		double  cellLength = linkLength / numberOfCells;
		
		Coord fromCoord = this.link.getFromNode().getCoord();
		Coord toCoord = this.link.getToNode().getCoord();
		double dx = (toCoord.getX() - fromCoord.getX()) / numberOfCells;
		double dy = (toCoord.getY() - fromCoord.getY()) / numberOfCells;
		
		// p... shift lanes??
		this.cells = new Cell[numberOfCells];
		for (int i = 0; i < numberOfCells; i++) {
			// add half of the cell length. position is mid point of cell!
			double x = fromCoord.getX() + (i - 0.5) * dx;
			double y = fromCoord.getY() + (i - 0.5) * dy;
			
			this.cells[i] = new Cell(i, new CoordImpl(x, y), cellLength);
		}

		double maxDistancePerTimeStep = this.link.getFreespeed() * timeStep;
				
		// at least one cell has to allow vehicles to leave within one time step
		int numPossibleOutCells = Math.max(1, (int) Math.floor(maxDistancePerTimeStep / this.spatialResolution));
		
		if (numPossibleOutCells > numberOfCells) this.firstPossibleOutCellIndex = 0;
		else this.firstPossibleOutCellIndex = numberOfCells - numPossibleOutCells;
	}
	
	/*package*/ Cell[] getCells() {
		return this.cells;
	}
	
	/*package*/ void setCASimEngine(CASimEngine simEngine) {
		this.simEngine = simEngine;
	}

	/*package*/ boolean hasWaitingToLeaveAgents() {
		for (int i = this.cells.length - 1; i >= firstPossibleOutCellIndex; i--) {
			if (this.cells[i].hasAgent()) return true;
		}
		return false;
//		return this.waitingToLeaveAgents.size() > 0;
	}

	/*
	 * Accepts additional agent(s) if at least the very first cell is empty.
	 * Entering agents might then be moved to a later cell.
	 */
	public boolean isAcceptingFromUpstream() {
		return !this.cells[0].hasAgent();
	}
	
	/**
	 * Adds a mobsimAgent to the link (i.e. the "queue"), called by
	 * {@link MultiModalQNode#moveAgentOverNode(MobsimAgent, double)}.
	 *
	 * @param personAgent
	 *          the personAgent
	 */
	public void addAgentFromIntersection(AgentMoveOverNodeContext context, double now) {
		this.activateLink();

		this.addAgent(context, now);

		this.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(now, context.mobsimAgent.getId(), link.getId(), null));
	}

	private void addAgent(AgentMoveOverNodeContext context, double now) {

		MobsimAgent mobsimAgent = context.mobsimAgent;
		double agentsSpeed = Math.min(context.currentSpeed, this.maxSpeed);
		double remainingTravelTime = context.remainingTravelTime;
		this.agentSpeeds.put(mobsimAgent.getId(), agentsSpeed);
		
		double maxTravelDistance = remainingTravelTime * agentsSpeed;
		int maxReachableCells = Math.max(1, (int) Math.floor(maxTravelDistance / this.spatialResolution));	// at least one cell has to be reachable
		
		Cell farestFreeCell = null;
		for (int i = 0; i < maxReachableCells; i++) {
			Cell cell = this.cells[i]; 
			if (!cell.hasAgent()) farestFreeCell = cell;
			else break;
		}
		
		if (farestFreeCell == null) throw new RuntimeException("Could not move agent " + mobsimAgent.getId() + 
				" over node to link " + this.link.getId());
		else farestFreeCell.setAgent(mobsimAgent);
//		Map<String, TravelTime> multiModalTravelTime = simEngine.getMultiModalTravelTimes();
//		Person person = null;
//		if (mobsimAgent instanceof HasPerson) {
//			person = ((HasPerson) mobsimAgent).getPerson(); 
//		}
//		
//		double travelTime = multiModalTravelTime.get(mobsimAgent.getMode()).getLinkTravelTime(link, now, person, null);
//		double departureTime = now + travelTime;
//
//		departureTime = Math.round(departureTime);
//
//		agents.add(new Tuple<Double, MobsimAgent>(departureTime, mobsimAgent));
	}

	public void addDepartingAgent(MobsimAgent mobsimAgent, double now) {
		this.waitingAfterActivityAgents.add(mobsimAgent);
		this.activateLink();

		this.simEngine.getMobsim().getEventsManager().processEvent(
				new AgentWait2LinkEvent(now, mobsimAgent.getId(), link.getId(), null));
	}

	protected boolean moveLink(double now) {
		
		boolean keepLinkActive = moveAgents(now);
		this.isActive.set(keepLinkActive);

		moveWaitingAfterActivityAgents();

		// If agents are ready to leave the link, ensure that the to Node is active and handles them.
		if (this.hasWaitingToLeaveAgents()) toNode.activateNode();
		
		return keepLinkActive;
	}

	private void activateLink() {
		/*
		 * If isActive is false, then it is set to true ant the
		 * link is activated. Using an AtomicBoolean is thread-safe.
		 * Otherwise, it could be activated multiple times concurrently.
		 */
		if (this.isActive.compareAndSet(false, true)) {			
			simEngine.activateLink(this);
		}
	}

	/*
	 * Returns true, if the Link has to be still active.
	 */
	private boolean moveAgents(double now) {
		
		/*
		 * TODO: 
		 * - Is there a way to iterate only over occupied cells?
		 * - Think about performance (iterate over array is faster than iterating over a list)
		 * - What about almost empty links?
		 * - What about overtaking (if using a list)
		 */
		for (int i = cells.length - 1; i >= 0; i--) {
			Cell cell = cells[i];
			MobsimAgent agent = cell.getAgent();
			if (agent != null) {
				Cell agentsNewCell = this.update(agent, cell, now);

				/*
				 * Check if MobsimAgent has reached destination:
				 * - Has the agent reached the end of the link?
				 * - Does the agent end its current leg on this link?
				 */
				if (agentsNewCell.getId() >= this.firstPossibleOutCellIndex) {
					MobsimDriverAgent driver = (MobsimDriverAgent) agent;
					if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
						agentsNewCell.reset();	// remove agent from its current cell
						driver.endLegAndComputeNextState(now);
						this.simEngine.internalInterface.arrangeNextAgentState(driver);
						this.agentSpeeds.remove(agent.getId());
					}
				}
				
			}
		}	
		
		return !this.agentSpeeds.isEmpty();
//		Tuple<Double, MobsimAgent> tuple = null;
//
//		while ((tuple = agents.peek()) != null) {
//			/*
//			 * If the MobsimAgent cannot depart now:
//			 * At least still one Agent is still walking/cycling/... on the Link, therefore
//			 * it cannot be deactivated. We return true (Link has to be kept active).
//			 */
//			if (tuple.getFirst() > now) {
//				return true;
//			}
//
//			/*
//			 *  Agent starts next Activity at the same link or leaves the Link.
//			 *  Therefore remove him from the Queue.
//			 */
//			agents.poll();
//
//			// Check if MobsimAgent has reached destination:
//			MobsimDriverAgent driver = (MobsimDriverAgent) tuple.getSecond();
//			if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
//				driver.endLegAndComputeNextState(now);
//				this.simEngine.internalInterface.arrangeNextAgentState(driver);
//			}
//			/*
//			 * The PersonAgent can leave, therefore we move him to the waitingToLeave Queue.
//			 */
//			else {
//				this.waitingToLeaveAgents.add(driver);
//			}
//		}
//		
//		return agents.size() > 0;
	}

	private Cell update(MobsimAgent agent, Cell currentCell, double now) {
		
		// find cell which is reached with v * Delta(t) or head of gap            
		Cell nextCell = this.getNextCell(agent, currentCell, now); // find cell of agent
		currentCell.reset();
		nextCell.setAgent(agent);
		return nextCell;
//		agent.setCell(nextCell);
//      if (isempty(agent.getParkingLot()) || agent.leaveParkingLot(currentTime)) % agent is on the road
//          nextCell.setAgent(agent);
//      end
//      agent.setCell(nextCell);
	}
	
	private Cell getNextCell(MobsimAgent agent, Cell currentCell, double currentTime) {
		
		int currentIndex = currentCell.getId();
		double currentSpeed = this.agentSpeeds.get(agent.getId());
		
		// min(id, id(link length))
		int maxPossibleNextIndex = Math.min(currentIndex + (int) Math.floor(currentSpeed * timeStep / this.spatialResolution), this.cells.length);

		// start at next cell not current cell
		int nextCellIndex = this.moveOrParkInGap(currentIndex + 1, maxPossibleNextIndex, agent, currentSpeed, timeStep, currentTime);
		return this.cells[nextCellIndex];
	}
	
	private int moveOrParkInGap(int startIndex, int endIndex, MobsimAgent agent, double currentSpeed, double timeStep, double now) {
		int index = endIndex;
		
		for (int i = startIndex; i <= endIndex; i++) {
			// TODO: check - can we use the same random value twice?
			double rand = random.nextDouble();
			
			Cell cell = this.cells[i];
			// cell is reachable
			if (!cell.hasAgent()) {
				index = i;
				// next cell is also free
				if (i + 1 < this.cells.length && !this.cells[i + 1].hasAgent()) {
					double newSpeed = Math.max(this.minSpeed, Math.min(currentSpeed + this.spatialResolution / timeStep + rand, this.maxSpeed - rand));
					//step 1 and 2 of NaSch 
					this.agentSpeeds.put(agent.getId(), newSpeed);
				}
			} else {
				// go to last free cell of gap
				index = i - 1;
				
				// + 1 as we start at agent's current position + 1! -> (startIndex - 1 - startIndex + 1) if agent should not move
				double newSpeed = Math.max(this.minSpeed, Math.min((index - startIndex + 1) / timeStep *  this.spatialResolution - rand, this.maxSpeed - rand)); 

				// step 2 and 3 of NaSch
				this.agentSpeeds.put(agent.getId(), newSpeed);
				
				break;
			}
		}
		return index;
	}
	
	/*
	 * Add all Agents that have ended an Activity to the waitingToLeaveLink Queue.
	 * If waiting Agents exist, the toNode of this Link is activated.
	 */
	private void moveWaitingAfterActivityAgents() {
		
		int cellToCheck = this.cells.length - 1;
		while (!waitingAfterActivityAgents.isEmpty()) {
			
			// search for the next free cell in the range of cell that could leave the link within a single time step
			Cell freeCell = null;
			for (int i = cellToCheck; i >= firstPossibleOutCellIndex; i--) {
				Cell cell = this.cells[i];
				if (cell.hasAgent()) cellToCheck--;
				else {
					freeCell = cell;
					break;
				}
			}
			
			// if a free cell was found, get agent from waiting list and move it to the cell
			if (freeCell != null) {
				MobsimAgent agent = this.waitingAfterActivityAgents.poll();
				freeCell.setAgent(agent);
				this.agentSpeeds.put(agent.getId(), 10.0);
			} else break;
		}
		
//		waitingToLeaveAgents.addAll(waitingAfterActivityAgents);
//		waitingAfterActivityAgents.clear();
	}
	
	public AgentMoveOverNodeContext getNextWaitingAgent(double now) {

		// look for a mobsim agent in the cell that can leave the link within a time step
		Cell cell = null;
		MobsimAgent mobsimAgent = null;
		int cellToCheck = this.cells.length - 1;
		double distanceOnLeftLink = 0.0;
		for (int i = cellToCheck; i >= firstPossibleOutCellIndex; i--) {
			cell = this.cells[i];
			mobsimAgent = cell.getAgent();
			distanceOnLeftLink += this.spatialResolution;
			if (mobsimAgent != null) break;
		}

		// check whether a potentially leaving agent was found
		if (mobsimAgent != null) {
			// check whether the agent travels fast enough to leave the link in this time step
			double travelDistance = this.agentSpeeds.get(mobsimAgent.getId()) / this.timeStep;
			if (travelDistance > distanceOnLeftLink) {
				cell.reset();
				this.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, mobsimAgent.getId(), link.getId(), null));
				AgentMoveOverNodeContext context = new AgentMoveOverNodeContext();
				context.mobsimAgent = mobsimAgent;
				context.currentSpeed = this.agentSpeeds.remove(mobsimAgent.getId());
				context.remainingTravelTime = this.timeStep - distanceOnLeftLink / context.currentSpeed;
				return context;				
			} else return null;
		} else return null;
	}

	public void clearVehicles() {
		double now = this.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (Cell cell : this.cells) {
			MobsimAgent mobsimAgent = cell.getAgent();
			if (mobsimAgent == null) continue;
			this.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			this.simEngine.getMobsim().getAgentCounter().incLost();
			this.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		
		for (MobsimAgent mobsimAgent : this.waitingAfterActivityAgents) {
			this.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			this.simEngine.getMobsim().getAgentCounter().incLost();
			this.simEngine.getMobsim().getAgentCounter().decLiving();
		}
	}
}
