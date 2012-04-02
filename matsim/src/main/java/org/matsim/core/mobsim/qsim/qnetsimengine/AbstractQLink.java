/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.comparators.QVehicleEarliestLinkExitTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * Please read the docu  AbstractQLane and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class AbstractQLink extends QLinkInternalI {
	
	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();

	final Link link ;
	
	final QNetwork network;	
	
	// joint implementation for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	private final Map<Id, QVehicle> parkedVehicles = new LinkedHashMap<Id, QVehicle>(10);

	private final Map<Id, MobsimAgent> additionalAgentsOnLink = new LinkedHashMap<Id, MobsimAgent>();
	
	private final Map<Id, MobsimDriverAgent> agentsWaitingForCars = new LinkedHashMap<Id, MobsimDriverAgent>();

	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
 /*package*/ final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<QVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/*package*/ NetElementActivator netElementActivator;
	
	AbstractQLink(Link link, QNetwork network) {
		this.link = link ;
		this.network = network;
		this.netElementActivator = network.simEngine;
	}
	
	abstract boolean doSimStep(double now);

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);
	
	abstract QNode getToNode() ;

	/*package*/ final void addParkedVehicle(MobsimVehicle vehicle) {
		QVehicle qveh = (QVehicle) vehicle ; // cast ok: when it gets here, it needs to be a qvehicle to work.
		this.parkedVehicles.put(qveh.getId(), qveh);
		qveh.setCurrentLink(this.link);
	}

	/*package*/ final QVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	/*package*/ final void addDepartingVehicle(MobsimVehicle mvehicle) {
		QVehicle vehicle = (QVehicle) mvehicle ;
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
	}

	/*package*/ void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	/*package*/ MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		return this.additionalAgentsOnLink.remove(mobsimAgentId);
	}

	/*package*/ Collection<MobsimAgent> getAdditionalAgentsOnLink(){
		return Collections.unmodifiableCollection( this.additionalAgentsOnLink.values());
	}
	
	void clearVehicles() {
		this.parkedVehicles.clear();

		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : this.waitingList) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
//		this.network.simEngine.getMobsim().getAgentCounter().decLiving(this.waitingList.size());
//		this.network.simEngine.getMobsim().getAgentCounter().incLost(this.waitingList.size());
		this.waitingList.clear();

	}

	void makeVehicleAvailableToNextDriver(QVehicle veh, double now) {
		MobsimDriverAgent agentWaitingForCar = agentsWaitingForCars.remove(veh.getId());
		if (agentWaitingForCar != null) {
			this.letAgentDepartWithVehicle(agentWaitingForCar, veh, now);
		}
	}
	
	final void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) {
		vehicle.setDriver(agent);
		if ( agent.getDestinationLinkId().equals(link.getId()) && (agent.chooseNextLinkId() == null)) {
			// yyyy this should be handled at person level, not vehicle level.  kai, feb'10

			agent.endLegAndComputeNextState(now);
			this.addParkedVehicle(vehicle);
			this.network.simEngine.internalInterface.arrangeNextAgentState(agent) ;
			// yyyyyy I think this neither works this way nor when I exchange the last two lines. kai, dec'11
			
		} else {
			EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
			eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, agent.getId(), vehicle.getId()));
			this.addDepartingVehicle(vehicle);
		}
	}
	

	final boolean addTransitToBuffer(final double now, final QVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			while (true) {
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue, can do this as the waitQueue is also non-blocking anyway
						transitVehicleStopQueue.add(veh);
						return true;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}

	QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = this.parkedVehicles.get(vehicleId);
		return ret;
	}
	
	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = this.getAllNonParkedVehicles();
		vehicles.addAll(this.parkedVehicles.values());
		return vehicles;
	}

	
	
	@Override
	public final Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	/*package*/ void setNetElementActivator(NetElementActivator qSimEngineRunner) {
		this.netElementActivator = qSimEngineRunner;
	}

	/*package*/ void registerAgentWaitingForCar(MobsimDriverAgent agent) {
		Id vehicleId = agent.getPlannedVehicleId() ;
		agentsWaitingForCars.put(vehicleId, agent);
	}
	
}
