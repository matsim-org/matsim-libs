/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
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

package playground.christoph.mobsim.ca2;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import playground.christoph.mobsim.ca2.events.VXYEvent;

public class CALink {

	private static final Logger log = Logger.getLogger(CALink.class);
	
	public static boolean createVXYEvents = true;
	
	private final Link link;
	private final CANode toNode;
	private final double spatialResolution;
	private final double timeStep;
	private final Random random;
	
	private final double minSpeed;
	private final double maxSpeed;
	private final int maxCellsPerTimeStep;
	
	// cannot be final since it is replaced in the parallel implementation
	private CASimEngine simEngine;
	
	private Cell[] cells;

	/*
	 * Is set to "true" if the MultiModalQLinkExtension has active Agents.
	 */
	protected AtomicBoolean isActive = new AtomicBoolean(false);

	private final Deque<MobsimAgent> waitingAfterActivityAgents = new LinkedList<MobsimAgent>();
	
	private final Map<Id, AgentContext> agentContexts = new HashMap<Id, AgentContext>();
	
	/*
	 * Index of the first cells from which a vehicle can leave the link within a single time step.
	 */
	private int firstPossibleOutCellIndex;
	/*
	 * If e.g. vehicles from 5.2 cells could leave the link within a time step, every 5th time step
	 * an additional vehicle is allowed to leave.
	 */
	private double firstPossibleOutCellIndexFraction;
	
	/*
	 * Number of vehicles that can leave the link within a time step.
	 */
	private int outFlowCapacity;
	/*
	 * Fraction of vehicles that can leave the link within a time step (see comment for possibleOutflowFraction). 
	 */
	private double outFlowCapacityFraction;
	
	private double randomValue;
	private double randomValueTime;
	
	// m/s - TODO: find a formula or a reasonable value for this!
	private double acceleration = 2.0;
	
	// TODO: find reasonable values for this!
	private double randomizationProbability = 0.10;	// 10% of all agents are affected
	private double randomizationSlowDown = 0.95;	// slow down by 5%
	
	public CALink(Link link, CASimEngine simEngine, CANode multiModalQNodeExtension, double spatialResolution,
			double timeStep) {
		this.link = link;
		this.simEngine = simEngine;
		this.toNode = multiModalQNodeExtension;
		this.spatialResolution = spatialResolution;
		this.timeStep = timeStep;

		this.minSpeed = 0.0;
		this.maxSpeed = link.getFreespeed();
		
		double maxDistancePerTimeStep = this.maxSpeed * this.timeStep;
		maxCellsPerTimeStep = (int) Math.floor(maxDistancePerTimeStep / this.spatialResolution);
		
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

			this.cells[i] = new Cell(i, new Coord(x, y), cellLength);
		}

		/*
		 * Calculate number of possible out cells based on link's free speed.
		 */		
		double maxDistancePerTimeStep = this.link.getFreespeed() * this.timeStep;
				
		// at least one cell has to allow vehicles to leave within one time step
		double numPossibleOutCellsSpeed = maxDistancePerTimeStep / this.spatialResolution;
		
		if (numPossibleOutCellsSpeed > numberOfCells) {
			this.firstPossibleOutCellIndex = 0;
			this.firstPossibleOutCellIndexFraction = 0.0;
		} else {
			this.firstPossibleOutCellIndex = numberOfCells - (int) numPossibleOutCellsSpeed;
			this.firstPossibleOutCellIndexFraction = numPossibleOutCellsSpeed - (int) numPossibleOutCellsSpeed;
		}

		/*
		 * Calculate number of possible outflow vehicles based on link's capacity
		 */
		double outFlowCap = this.timeStep * this.link.getCapacity() / 3600.0;	// TODO: so far it is assumed that the capacity is per hour
		this.outFlowCapacity = (int) outFlowCap;
		this.outFlowCapacityFraction = outFlowCap - this.outFlowCapacity;
	}
	
	/*package*/ Cell[] getCells() {
		return this.cells;
	}
	
	/*package*/ void setCASimEngine(CASimEngine simEngine) {
		this.simEngine = simEngine;
	}

	/*package*/ boolean hasWaitingToLeaveAgents(double now) {
		for (int i = this.cells.length - 1; i >= this.getFirstOutFlowCell(now); i--) {
			if (this.cells[i].hasAgent()) return true;
		}
		return false;
//		return this.waitingToLeaveAgents.size() > 0;
	}

	/*package*/ void remveAgentContext(Id agentId) {
		this.agentContexts.remove(agentId);
	}
	
	private int getFirstOutFlowCell(double now) {
		
		if (now != this.randomValueTime) this.randomValue = this.random.nextDouble();
		
		if (this.randomValue <= this.firstPossibleOutCellIndexFraction) return this.firstPossibleOutCellIndex - 1;
		else return this.firstPossibleOutCellIndex;
	}
	
	public int getOutFlowCapacity(double now) {
		
		if (now != this.randomValueTime) this.randomValue = this.random.nextDouble();
		
		if (this.randomValue <= this.outFlowCapacityFraction) return this.outFlowCapacity + 1;
		else return this.outFlowCapacity;
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
		AgentContext agentContext = new AgentContext();
		agentContext.currentSpeed = agentsSpeed;
		this.agentContexts.put(mobsimAgent.getId(), agentContext);
			
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
	}

	public void addDepartingAgent(MobsimAgent mobsimAgent, double now) {
		this.waitingAfterActivityAgents.add(mobsimAgent);
		this.activateLink();

		this.simEngine.getMobsim().getEventsManager().processEvent(
				new Wait2LinkEvent(now, mobsimAgent.getId(), link.getId(), null));
	}

	protected boolean moveLink(double now) {
		
		boolean keepLinkActive = moveAgents(now);
		this.isActive.set(keepLinkActive);

		moveWaitingAfterActivityAgents(now);

		// If agents are ready to leave the link, ensure that the to Node is active and handles them.
		if (this.hasWaitingToLeaveAgents(now)) toNode.activateNode();
		
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
		
//		/*
//		 * TODO: 
//		 * - Is there a way to iterate only over occupied cells?
//		 * - Think about performance (iterate over array is faster than iterating over a list)
//		 * - What about almost empty links?
//		 * - What about overtaking (if using a list)
//		 */
		List<Integer> cellsToReset = new ArrayList<Integer>();
		for (int i = cells.length - 1; i >= 0; i--) {
			Cell cell = cells[i];
			MobsimAgent agent = cell.getAgent();
			if (agent != null) {

				AgentContext agentContext = this.agentContexts.get(agent.getId());
				int nextCellIndex = this.adaptSpeed(agentContext, i);
				
				// if the agent was moved forward
				if (nextCellIndex > i) {					
					this.carMotion(agent, nextCellIndex);
					cellsToReset.add(i);
				}
				
				if (createVXYEvents) {
					Cell newCell = this.cells[nextCellIndex];
					VXYEvent event = new VXYEvent(agent.getId(), this.agentContexts.get(agent.getId()).currentSpeed, newCell.getCoord().getX(), newCell.getCoord().getY(), now);
					this.simEngine.getMobsim().getEventsManager().processEvent(event);
				}
				
				/*
				 * Check if MobsimAgent has reached destination:
				 * - Has the agent reached the end of the link?
				 * - Does the agent end its current leg on this link?
				 */
				if (nextCellIndex >= this.getFirstOutFlowCell(now)) {
					MobsimDriverAgent driver = (MobsimDriverAgent) agent;
					if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
						cellsToReset.add(nextCellIndex);	// remove agent from its current cell
						driver.endLegAndComputeNextState(now);
						this.simEngine.internalInterface.arrangeNextAgentState(driver);
						this.agentContexts.remove(agent.getId());
					}
				}	
			}
		}
		for (int i : cellsToReset) this.cells[i].reset();

		return !this.agentContexts.isEmpty();
	}

	private int adaptSpeed(AgentContext agentContext, int cellIndex) {

		// find next occupied cell within search range (defined by link's vmax)
		int farestNotOccupiedCellIndex = cellIndex;
		for (int i = cellIndex + 1; i <= Math.min(cells.length - 1, cellIndex + maxCellsPerTimeStep); i++) {
			Cell cell = this.cells[i];
			if (!cell.hasAgent()) farestNotOccupiedCellIndex++;
			else break;
		}
		
		// try to accelerate
		int possibleTravelledCells = this.accelerate(agentContext, cellIndex, farestNotOccupiedCellIndex);
		boolean accelerated = (possibleTravelledCells >= 0);
		
		// if the agent did not accelerate, it might have to slow down its speed
		boolean slowedDown = false;
		if (!accelerated) {
			slowedDown = true; 
			possibleTravelledCells = this.slowDown(agentContext, cellIndex, farestNotOccupiedCellIndex);
		}
		
		// randomize
//		this.randomization(agentContext);
		
		// return next cell for step 4
//		int maxTravelledCells = (int) Math.floor((agentContext.currentSpeed * this.timeStep) / this.spatialResolution);
		int nextCellIndex = Math.min(this.cells.length - 1, cellIndex + possibleTravelledCells);
		
		if (nextCellIndex > farestNotOccupiedCellIndex) {
			log.error("to far!!");
		}
		return nextCellIndex;
	}
	
	// step 1 of Nagel & Schreckenberg
	private int accelerate(AgentContext agentContext, int cellIndex, int farestNotOccupiedCellIndex) {
		
		/*
		 * Workaround: 
		 * At the end of a link, no more free cells are available. Here, we assume that the agent can 
		 * accelerate in any case. To do so, we increase the farestNotOccupiedCellIndex. In the future,
		 * the agent should either check the intersection or the next link.
		 */
		if (this.cells.length - 1 == farestNotOccupiedCellIndex) farestNotOccupiedCellIndex += 1000;
		
		double currentSpeed = agentContext.currentSpeed;
		
		// try accelerating
		double newSpeed = Math.min(this.maxSpeed, currentSpeed + this.acceleration * timeStep);
		
		// add randomization
		newSpeed = this.randomization(newSpeed);
		
		double distance = agentContext.positionInCell + newSpeed * this.timeStep;
//		double distance = newSpeed * this.timeStep;
		int distanceInCells = (int) Math.floor(distance / this.spatialResolution);
		if (cellIndex + distanceInCells <= farestNotOccupiedCellIndex) {
			agentContext.currentSpeed = newSpeed;
			agentContext.positionInCell = distance - distanceInCells * this.spatialResolution;
			return distanceInCells;
		} else return -1;
	}
	
	// step 2 of Nagel & Schreckenberg
	private int slowDown(AgentContext agentContext, int cellIndex, int farestNotOccupiedCellIndex) {
		
		double currentSpeed = agentContext.currentSpeed;
		double distance = (farestNotOccupiedCellIndex - cellIndex) * this.spatialResolution;
		double newSpeed = distance / this.timeStep;

		// add randomization
		newSpeed = this.randomization(newSpeed);
		
		agentContext.currentSpeed = newSpeed;
		agentContext.positionInCell = 0.0;	// try moving as far as possible in the cell?? e.g. 0.98 * length?
		if (newSpeed > currentSpeed) log.error("Agent should slow down but new speed is higher than current speed?!");
		
		return farestNotOccupiedCellIndex - cellIndex;
	}
	
	// step 3 of Nagel & Schreckenberg
	private void randomization(AgentContext agentContext) {
		if (this.random.nextDouble() < this.randomizationProbability) {
			agentContext.currentSpeed *= this.randomizationSlowDown;
		}
	}

	private double randomization(double speed) {
		if (this.random.nextDouble() < this.randomizationProbability) {
			return speed * this.randomizationSlowDown;
		} else return speed;
	}
	
	private void carMotion(MobsimAgent agent, int newCellIndex) {
		this.cells[newCellIndex].setAgent(agent);
	}
	
	private Cell update(MobsimAgent agent, Cell currentCell, double now) {
		
		// find cell which is reached with v * Delta(t) or head of gap
		Cell nextCell = this.getNextCell(agent, currentCell, now); // find cell of agent
		currentCell.reset();
		nextCell.setAgent(agent);
		
		if (createVXYEvents) {
			VXYEvent event = new VXYEvent(agent.getId(), this.agentContexts.get(agent.getId()).currentSpeed, nextCell.getCoord().getX(), nextCell.getCoord().getY(), now);
			this.simEngine.getMobsim().getEventsManager().processEvent(event);
		}
		
		return nextCell;
	}
	
	private Cell getNextCell(MobsimAgent agent, Cell currentCell, double currentTime) {
		
		int currentIndex = currentCell.getId();
		AgentContext agentContext = this.agentContexts.get(agent.getId());
		double currentSpeed = agentContext.currentSpeed;
		
		// min(id, id(link length))
		int maxPossibleNextIndex = Math.min(currentIndex + (int) Math.floor(currentSpeed * timeStep / this.spatialResolution), this.cells.length);

		// start at next cell not current cell
		int nextCellIndex = this.moveOrParkInGap(currentIndex + 1, maxPossibleNextIndex, agent, currentSpeed, timeStep, currentTime);
		return this.cells[nextCellIndex];
	}
	
	private int moveOrParkInGap(int startIndex, int endIndex, MobsimAgent agent, double currentSpeed, double timeStep, double now) {
		int index = endIndex;
		
		/*
		 * From Nagel & Schreckenberg "A cellular automaton model for freeway traffic"
		 * 
		 * A system update consists of 4 steps:
		 * 1) Acceleration:
		 * 		if the velocity of a vehicle is lower than vmax and if the distance to the next
		 * 		car ahead is larger than v + 1, the speed is advanced by one [v -> v + 1].
		 * 2) Slowing down (due to other cars):
		 * 		if a vehicle at site i sees the next vehicle at site i + j (with j <= v), it
		 * 		reduces its speed to j - 1 [v -> j - 1].
		 * 3) Randomization:
		 * 		with probability p, the velocity of each vehicle (if greater than zero) is decreased
		 * 		by one [v -> v - 1].
		 * 4) Car motion:
		 * 		each vehicle is advanced v sites.
		 * 
		 * Note: v is not m/s but cells/time step!
		 */
		for (int i = startIndex; i <= endIndex; i++) {
			// TODO: check - can we use the same random value twice?
			double rand = random.nextDouble();
			
			AgentContext agentContext = this.agentContexts.get(agent.getId());
			
			Cell cell = this.cells[i];
			// cell is reachable
			if (!cell.hasAgent()) {
				index = i;
				// next cell is also free
				if (i + 1 < this.cells.length && !this.cells[i + 1].hasAgent()) {
					double newSpeed = Math.max(this.minSpeed, Math.min(currentSpeed + this.spatialResolution / timeStep + rand, this.maxSpeed - rand));
					//step 1 and 2 of NaSch
					agentContext.currentSpeed = newSpeed;
				}
			} else {
				// go to last free cell of gap
				index = i - 1;
				
				// + 1 as we start at agent's current position + 1! -> (startIndex - 1 - startIndex + 1) if agent should not move
				double newSpeed = Math.max(this.minSpeed, Math.min((index - startIndex + 1) / timeStep *  this.spatialResolution - rand, this.maxSpeed - rand)); 

				// step 2 and 3 of NaSch
				agentContext.currentSpeed = newSpeed;
				
				break;
			}
		}
		return index;
	}
	
	/*
	 * Add all Agents that have ended an Activity to the waitingToLeaveLink Queue.
	 * If waiting Agents exist, the toNode of this Link is activated.
	 */
	private void moveWaitingAfterActivityAgents(double now) {
		
		int cellToCheck = this.cells.length - 1;
		while (!waitingAfterActivityAgents.isEmpty()) {
			
			// search for the next free cell in the range of cell that could leave the link within a single time step
			Cell freeCell = null;
			for (int i = cellToCheck; i >= this.getFirstOutFlowCell(now); i--) {
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
				AgentContext agentContext = new AgentContext();
				agentContext.currentSpeed = 0.0;
				this.agentContexts.put(agent.getId(), agentContext);
				
				/*
				 * The agent has a velocity of 0.0, i.e. it will have to accelerate before it can leave the link.
				 * To do so, its current link has to be active.
				 */
				this.activateLink();
			} else break;
		}
	}
	
	/*
	 * Returns a AgentMoveOverNodeContext object if the agent could leave the node,
	 * i.e. its speed is high enough to travel far enough. However, at this point in
	 * time it is not clear, whether the agent's next link has free capacity.
	 */
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
			AgentContext agentContext = this.agentContexts.get(mobsimAgent.getId());
			
			// check whether the agent travels fast enough to leave the link in this time step
			double travelDistance = agentContext.currentSpeed * this.timeStep;
			if (travelDistance > distanceOnLeftLink) {
				this.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, mobsimAgent.getId(), link.getId(), null));
				AgentMoveOverNodeContext context = new AgentMoveOverNodeContext();
				context.mobsimAgent = mobsimAgent;
				context.currentCell = cell;
				context.currentSpeed = agentContext.currentSpeed;
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
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			this.simEngine.getMobsim().getAgentCounter().incLost();
			this.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		
		for (MobsimAgent mobsimAgent : this.waitingAfterActivityAgents) {
			this.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			this.simEngine.getMobsim().getAgentCounter().incLost();
			this.simEngine.getMobsim().getAgentCounter().decLiving();
		}
	}

	public String toString() {
		return "[id=" + this.link.getId() + "]" +
		"[length=" + this.link.getLength() + "]";
	}
	
	private static class AgentContext {
		double currentSpeed;
		double positionInCell = 0.0;
	}
}
