/* *********************************************************************** *
 * project: org.matsim.*
 * FlexibleCellLink.java
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

package playground.christoph.mobsim.flexiblecells;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import playground.christoph.mobsim.flexiblecells.events.VXYEvent;
import playground.christoph.mobsim.flexiblecells.velocitymodels.GippsModel;
import playground.christoph.mobsim.flexiblecells.velocitymodels.VelocityModel;

public class FlexibleCellLink {

	private static final Logger log = Logger.getLogger(FlexibleCellLink.class);
	
	public static boolean createVXYEvents = true;
	
	private final Link link;
	private FlexibleCell head;
	private FlexibleCell tail;
	
	private FlexibleCellSimEngine simEngine;
	private final FlexibleCellNode toNode;
	
	private final Deque<MobsimAgent> waitingAfterActivityAgents = new LinkedList<MobsimAgent>();
	private final double length;
	private final double fromX;
	private final double fromY;
	private final double normDx;
	private final double normDy;
	private final double timeStep;
	
	private final double vehicleLength;	// so far hard-coded
	private final double minSpaceCellLength;	// min space between two vehicles
	private final double maxOutflowDistance;
		
	private final VelocityModel velocityModel;
	
	/*
	 * If the next node is an intersection, the agent has to slow down. Otherwise its speed
	 * is not adapted when coming closer to the node. In that case  the agent has to peek
	 * to the next link.
	 * 
	 * Note:
	 * This will make parallelization a bit harder since links "connected" this way will have
	 * to be simulated on one thread. However, at intersections splitting the network
	 * should still be possible. 
	 * Alternative:
	 * Introduce something like a cachedFirstVehicle that points to each links first vehicle
	 * and is updated outside the parallel code.
	 */
//	private final boolean nextNodeIsIntersection;
//	private FlexibleCellLink nextLink;
	
	// so far this are only assumptions!
	private double deceleration = 3.0;
	
	/*
	 * Is set to "true" if the MultiModalQLinkExtension has active Agents.
	 */
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	
	public FlexibleCellLink(Link link, FlexibleCellSimEngine simEngine, FlexibleCellNode toNode, double vehicleLength, double minSpaceCellLength, double timeStep) {
		this.link = link;
		this.simEngine = simEngine;
		this.toNode = toNode;
		this.length = link.getLength();
		this.vehicleLength = vehicleLength;
		this.minSpaceCellLength = minSpaceCellLength;
		this.timeStep = timeStep;

//		this.velocityModel = new LeutzbachModel(this.timeStep);
		this.velocityModel = new GippsModel();	
		
		this.maxOutflowDistance = Math.min(this.link.getLength(), this.link.getFreespeed() * this.timeStep);
		
		double dx = this.link.getToNode().getCoord().getX() - this.link.getFromNode().getCoord().getX();
		double dy = this.link.getToNode().getCoord().getY() - this.link.getFromNode().getCoord().getY();
		this.normDx = dx / this.length;
		this.normDy = dy / this.length;
		this.fromX = this.link.getFromNode().getCoord().getX();
		this.fromY = this.link.getFromNode().getCoord().getY();
		
		
		this.reset();
	}
	
	public double getLength() {
		return this.link.getLength();
	}
	
	public VehicleCell getFirstVehicle() {
		return (VehicleCell) this.head.getNextCell();
	}
	
	/*package*/ Link getLink() {
		return this.link;
	}
	
	/*package*/ void setNextLink(FlexibleCellLink nextLink) {
		this.velocityModel.setNextLink(nextLink);
	}
	
	/*package*/ void setCASimEngine(FlexibleCellSimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	/*package*/ boolean hasWaitingToLeaveAgents(double now) {
		
		if (this.hasVehicles()) {
			/*
			 * If there is no space cell at the end of the link, the vehicle is ready to leave
			 */
			FlexibleCell flexibleCell = this.tail;
			if (flexibleCell instanceof VehicleCell) return true;
			else {
				VehicleCell vehicleCell = (VehicleCell) this.tail.getPreviousCell();
				if (vehicleCell.getHeadPosition() > this.length - this.maxOutflowDistance) return true;
				else return false;
			}
		} else return false;
	}
	
	/*
	 * Check whether there is enough space for at least one additional agent.
	 */
	public boolean isAcceptingFromUpstream(double vehicleLength) {
		
		/*
		 * If at least one vehicle in on the link, get the first space.
		 * Its head position has to be reduced by the min space cell length since
		 * an additional vehicle cannot move closer to the vehicle which is already
		 * present on the link.
		 */
		double freeSpace = this.head.getHeadPosition();
		if (this.head != this.tail) freeSpace -= (this.minSpaceCellLength + this.vehicleLength);
		
		/*
		 * If an additional vehicle is added to the link, automatically also a
		 * space cell is added, which has a minimal length. Doing this simplifies
		 * handling in other places and should not affect the traffic flows to much.
		 */
		if (freeSpace + this.minSpaceCellLength >= vehicleLength) return true;
		else return false;
	}
	
	public void addAgentFromIntersection(VehicleCell vehicleCell, double now, double remainingLinkTravelDistance) {
		
		this.activateLink();

		this.addVehicle(vehicleCell, now, remainingLinkTravelDistance);

		this.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(now, vehicleCell.getMobsimAgent().getId(), link.getId(), null));
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
	 * So far: assume length of 7.5m
	 * later: get a MobsimVehicle that know its length
	 */
	/*
	 * Tries to add the vehicle to the link at the given head position.
	 * Vehicles speed is not checked here (e.g. whether it will collide with the next vehicle)!
	 */
	public boolean addVehicle(VehicleCell vehicleCell, double now, double remainingLinkTravelDistance) {
		
		// check whether the vehicle has space at all
		double freeSpace = this.head.getHeadPosition();
		if (freeSpace < this.vehicleLength + this.minSpaceCellLength) {
			throw new RuntimeException("There is not enough free space on link " + this.link.getId() +
					" for agent " + vehicleCell.getMobsimAgent().getId() + ". This should not happen. Aborting!");
		} else {
			/*
			 * Add vehicle cell and subsequent space cell to deque.
			 * Leave no space for further vehicles on the link. Does this make sense?
			 */
			
			// check how far the vehicle could travel if enough free space is available
			double maxTravelDistance = vehicleCell.getSpeed() * this.timeStep;
			maxTravelDistance -= remainingLinkTravelDistance;
			
			double headPosition;
			
			// ensure that the vehicle does not drive to far
			headPosition = Math.min(maxTravelDistance, this.head.getHeadPosition() - this.minSpaceCellLength); 
			
			// ensure that the vehicles fully enters the link - this might be a bit of teleportation!!
			headPosition = Math.max(this.vehicleLength + this.minSpaceCellLength, headPosition);
			
			vehicleCell.setHeadPosition(headPosition);
			
			vehicleCell.setNextCell(this.head);
			this.head.setPreviousCell(vehicleCell);
			this.head = vehicleCell;
			
			SpaceCell spaceCell = new SpaceCell(headPosition - this.minSpaceCellLength, this.minSpaceCellLength);
			spaceCell.setNextCell(this.head);
			this.head.setPreviousCell(spaceCell);
			this.head = spaceCell;
			
			return true;
		}
	}
	
	/*
	 * Returns a VehicleCell object if the agent could leave the node,
	 * i.e. its speed is high enough to travel far enough. However, at this point in
	 * time it is not clear, whether the agent's next link has free capacity.
	 * 
	 * It is not checked whether the vehicle is allowed to leave! This check is done in the
	 * hasWaitingToLeaveAgents(...) method!!
	 */
	/*package*/ VehicleCell getNextWaitingVehicle(double now) {
		
		if (this.tail instanceof VehicleCell) return (VehicleCell) this.tail;
		else return ((SpaceCell) this.tail).getPreviousCell();
	}
	
	public VehicleCell removeTailVehicle() {
		
		while (this.hasVehicles()) {
			
			/*
			 * remove current tail and setup new one 
			 */
			FlexibleCell flexibleCell = this.tail;
			flexibleCell.getPreviousCell().resetNextCell();
			this.tail = flexibleCell.getPreviousCell();
			flexibleCell.resetPreviousCell();
			
			if (flexibleCell instanceof SpaceCell) {
				VehicleCell vehicleCell = (VehicleCell) this.tail;
				vehicleCell.setHeadPosition(this.length);
				vehicleCell.resetNextCell();
			} else if (flexibleCell instanceof VehicleCell) {
				SpaceCell spaceCell = (SpaceCell) this.tail;
				spaceCell.setHeadPosition(this.length);
				spaceCell.resetNextCell();
				return (VehicleCell) flexibleCell;
			} else {
				throw new RuntimeException("Found unknown implementation of a FlexibleCell: " +
						flexibleCell.getClass().toString() + ". Aborting!");
			}
		} return null;
	}
	
	public void removeActivityStartVehicle(VehicleCell vehicleCell) {
		
		SpaceCell nextSpaceCell = vehicleCell.getNextCell();
		SpaceCell previousSpaceCell = vehicleCell.getPreviousCell();
		
		VehicleCell previousVehicleCell = previousSpaceCell.getPreviousCell();
		// if it was the first vehicle on the link 
		if (previousVehicleCell == null) {
			this.head = nextSpaceCell;
			this.head.resetPreviousCell();
		} else {
			previousVehicleCell.setNextCell(nextSpaceCell);
			nextSpaceCell.setPreviousCell(previousVehicleCell);
		}
		
		vehicleCell.resetNextCell();
		vehicleCell.resetPreviousCell();
		previousSpaceCell.resetNextCell();
		previousSpaceCell.resetPreviousCell();
	}
	
	public void addDepartingAgent(MobsimAgent mobsimAgent, double now) {
		this.waitingAfterActivityAgents.add(mobsimAgent);
		this.activateLink();

		this.simEngine.getMobsim().getEventsManager().processEvent(
				new Wait2LinkEvent(now, mobsimAgent.getId(), link.getId(), null));
	}
	
	protected boolean moveLink(double now) {
		
		boolean keepLinkActive = moveVehicles(now);
		this.isActive.set(keepLinkActive);

		moveWaitingAfterActivityAgents(now);

		// If agents are ready to leave the link, ensure that the to Node is active and handles them.
		if (this.hasWaitingToLeaveAgents(now)) toNode.activateNode();
		
		return keepLinkActive;
	}
	
	/*
	 * returns true if the link has to stay active
	 */
	private boolean moveVehicles(double now) {
	
		if (this.hasVehicles()) {
			SpaceCell spaceCell;
			VehicleCell vehicleCell;

			// move vehicle cells forward (but do not change space cells)
			spaceCell = (SpaceCell) this.head;
			vehicleCell = spaceCell.getNextCell();
			while (vehicleCell != null) {
				moveVehicle(vehicleCell, now);
				spaceCell = vehicleCell.getNextCell();
				if (spaceCell == null) vehicleCell = null;
				else vehicleCell = spaceCell.getNextCell();
			}
			
			// now move space cells (but do not change vehicle cells)
			spaceCell = (SpaceCell) this.head;
			vehicleCell = null;
			while (spaceCell != null) {
				moveSpace(spaceCell);
				vehicleCell = spaceCell.getNextCell();
				if (vehicleCell == null) spaceCell = null;
				else spaceCell = vehicleCell.getNextCell();
			}
			
			// finally let vehicles that have reached their destination arrive
			if (this.tail instanceof VehicleCell) vehicleCell = (VehicleCell) this.tail;
			else vehicleCell = ((SpaceCell) this.tail).getPreviousCell();
			while (vehicleCell != null && vehicleCell.getHeadPosition() >= this.length - maxOutflowDistance) {

				/*
				 * Cache possible next vehicle cell - the references in the vehicleCell 
				 * are removed in the checkAndHandleVehicleArrival(...) method. 
				 */ 
				VehicleCell possibleNextVehicleCell = vehicleCell.getPreviousCell().getPreviousCell(); 
				
				this.checkAndHandleVehicleArrival(vehicleCell, now);
				
				/*
				 * This should never produce a null pointer exception since there should
				 * always be a space cell at the first position of the link. 
				 */
				vehicleCell = possibleNextVehicleCell;
			}
		}
		return this.hasVehicles();
	}
	
	private boolean hasVehicles() {
		return this.head != this.tail;
	}
	
	private void moveVehicle(VehicleCell vehicleCell, double now) {
		
		// adapt speed
		double speed = calcVehicleSpeed(vehicleCell);
		vehicleCell.setSpeed(speed);
		vehicleCell.setHeadPosition(vehicleCell.getHeadPosition() + speed * this.timeStep);
		
		if (createVXYEvents) {
			MobsimAgent mobsimAgent = vehicleCell.getMobsimAgent();
			VXYEvent event = new VXYEvent(mobsimAgent.getId(), vehicleCell.getSpeed(), 
					this.fromX + this.normDx * vehicleCell.getHeadPosition(), 
					this.fromY + this.normDy * vehicleCell.getHeadPosition(), now);
			this.simEngine.getMobsim().getEventsManager().processEvent(event);
		}	
	}
	
	private double calcVehicleSpeed(VehicleCell vehicleCell) {
	
		// use velocity model to calculate agent's new speed
		double speed = this.velocityModel.calcSpeed(vehicleCell, link.getFreespeed());
		
		if (speed < 0.0) {
			log.warn("Found speed < 0.0 for agent " + vehicleCell.getMobsimAgent().getId() +
					": " + speed);
		}
		
		return speed;
	}
		
	private void moveSpace(SpaceCell spaceCell) {		
		VehicleCell vehicleCell = spaceCell.getNextCell();
		if (vehicleCell != null) {
			spaceCell.setHeadPosition(vehicleCell.getHeadPosition() - vehicleCell.getMinLength());
		}
	}
	
	/*
	 * Check if MobsimAgent has reached destination:
	 * - Does the agent end its current leg on this link?
	 */
	private void checkAndHandleVehicleArrival(VehicleCell vehicleCell, double now) {
		
		MobsimDriverAgent driver = (MobsimDriverAgent) vehicleCell.getMobsimAgent();
		if ((link.getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
			
			this.removeActivityStartVehicle(vehicleCell);
			
			driver.endLegAndComputeNextState(now);
			this.simEngine.internalInterface.arrangeNextAgentState(driver);
		}
	}
	
	/*
	 * Add all Agents that have ended an Activity to the waitingToLeaveLink Queue.
	 * If waiting Agents exist, the toNode of this Link is activated.
	 */
	private void moveWaitingAfterActivityAgents(double now) {
		
		if(!waitingAfterActivityAgents.isEmpty()) {
			
			double distanceFromLinkEnd = 0.0;
			FlexibleCell flexibleCell = this.tail;
			
			while (true) {

				SpaceCell spaceCell = null;
				if (flexibleCell instanceof VehicleCell) {
					spaceCell = (SpaceCell) flexibleCell.getPreviousCell();
				} else spaceCell = (SpaceCell) flexibleCell;
				
				// abort criteria #1
				distanceFromLinkEnd = (this.length - spaceCell.getHeadPosition()) + this.minSpaceCellLength;
				if (distanceFromLinkEnd > this.maxOutflowDistance) return;
				
				/* 
				 * Check whether there is a previous vehicle. If one is found, check whether a vehicle
				 * can be added in front of it (can is stop before crashing into the entered vehicle, etc.).
				 */
				boolean canInsertVehicle = true;
				double previousVehicleDistance = Double.MAX_VALUE;
				double previousVehicleSpeed = Double.NEGATIVE_INFINITY;
				FlexibleCell previousCell = flexibleCell.getPreviousCell();
				if (previousCell != null) {
					VehicleCell vehicleCell = (VehicleCell) previousCell;
					previousVehicleDistance = vehicleCell.getHeadPosition();
					previousVehicleSpeed = vehicleCell.getSpeed();
					double nextHeadPosition = flexibleCell.getHeadPosition();
					
					/*
					 * If possible, insert a new vehicle cell and a new space cell afterwards
					 * and adapt the cells head position.
					 */
					canInsertVehicle = this.checkVehicleInsert(previousVehicleSpeed, previousVehicleDistance, nextHeadPosition);
				}
				if (canInsertVehicle) {
					
					// create and add vehicle cell
					MobsimAgent mobsimAgent = this.waitingAfterActivityAgents.poll();
					
					double vehicleHeadPosition = spaceCell.getHeadPosition() - this.minSpaceCellLength;
					double speed = 0.0;
					VehicleCell newVehicleCell = new VehicleCell(mobsimAgent, vehicleHeadPosition, this.vehicleLength, speed);
					
					// create and add space cell
					SpaceCell newSpaceCell = new SpaceCell(spaceCell.getHeadPosition(), this.minSpaceCellLength);
					newSpaceCell.setNextCell(spaceCell.getNextCell());
					newSpaceCell.setPreviousCell(newVehicleCell);
					newVehicleCell.setNextCell(newSpaceCell);
					newVehicleCell.setPreviousCell(spaceCell);
					
					// update next vehicle cell, if not null
					VehicleCell nextvVehicleCell = spaceCell.getNextCell();
					if (nextvVehicleCell != null) nextvVehicleCell.setPreviousCell(newSpaceCell);
					
					// update existing space cell					
					spaceCell.setNextCell(newVehicleCell);
					spaceCell.setHeadPosition(newVehicleCell.getHeadPosition() - this.vehicleLength);
										
					// if the vehicle is inserted at the link's end, update reference
					if (spaceCell == this.tail) this.tail = newSpaceCell;
					
					/*
					 * The agent has a velocity of 0.0, i.e. it will have to accelerate before it can leave the link.
					 * To do so, its current link has to be active.
					 */
					this.activateLink();
					
					// abort criteria #2
					if (waitingAfterActivityAgents.isEmpty()) return;
					
					// update reference to flexibleCell for next iteration
					flexibleCell = newSpaceCell;
				} else {
					// try previous space cell
					if (spaceCell.getPreviousCell() != null && spaceCell.getPreviousCell().getPreviousCell() != null) {
						flexibleCell = spaceCell.getPreviousCell().getPreviousCell();							
					}
				}					
			}
		}
	}
	
	/*
	 * Check whether a vehicle can enter the link before another vehicle
	 * traveling based on a given speed and distance.
	 * Should probably be moved to the velocity model... 
	 */
	private boolean checkVehicleInsert(double speed, double distance, double nextHeadPosition) {
		
		// TODO: improve logic
		double brakingDistance = (speed * speed) / (2 * this.deceleration);
		if (brakingDistance > distance) return false;
				
		// is there enough space to the next vehicle?
		if (distance < this.vehicleLength + this.minSpaceCellLength) return false;
		
		// is there enough space on the link? (i.e. are we to near to the links head) 
		if (this.length - nextHeadPosition < this.vehicleLength + this.minSpaceCellLength) return false;
		
		return true;
	}
	
	public void clearVehicles() {
		double now = this.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		FlexibleCell cell = this.head;
		while (cell != null) {
			if (cell instanceof VehicleCell) {
				VehicleCell vehicleCell = (VehicleCell) cell;
				MobsimAgent mobsimAgent = vehicleCell.getMobsimAgent();
				this.simEngine.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
				this.simEngine.getMobsim().getAgentCounter().incLost();
				this.simEngine.getMobsim().getAgentCounter().decLiving();
			}
			cell = cell.getNextCell();
		}
		
		for (MobsimAgent mobsimAgent : this.waitingAfterActivityAgents) {
			this.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, mobsimAgent.getId(), link.getId(), mobsimAgent.getMode()));
			this.simEngine.getMobsim().getAgentCounter().incLost();
			this.simEngine.getMobsim().getAgentCounter().decLiving();
		}
	}
	
	public void reset() {
		if (this.head != null) this.head.setNextCell(null);
		if (this.tail != null) this.tail.setPreviousCell(null);
		this.head = new SpaceCell(this.length, this.minSpaceCellLength);
		this.tail = this.head;
	}

	@Override
	public String toString() {
		return "[id=" + this.link.getId() + "]" +
				"[length=" + this.link.getLength() + "]";
	}
	
}
