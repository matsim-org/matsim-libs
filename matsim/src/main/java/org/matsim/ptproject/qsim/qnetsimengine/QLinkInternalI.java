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

package org.matsim.ptproject.qsim.qnetsimengine;

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
import org.matsim.pt.qsim.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.comparators.QVehicleEarliestLinkExitTimeComparator;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;

/**
 * 
 * Please read the docu  AbstractQLane and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class QLinkInternalI extends AbstractQLane implements NetsimLink {
	
	abstract boolean doSimStep(double now);

	abstract void addFromIntersection(final QVehicle veh);
	
	abstract QNode getToNode() ;

	abstract void addParkedVehicle(MobsimVehicle vehicle) ;

	abstract QVehicle removeParkedVehicle(Id vehicleId) ;

	abstract void registerAdditionalAgentOnLink(MobsimAgent planAgent) ;

	abstract MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) ;

	abstract Collection<MobsimAgent> getAdditionalAgentsOnLink() ;
	
	abstract void clearVehicles() ;

	abstract void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) ;

	abstract QVehicle getVehicle(Id vehicleId) ;
	
	abstract void registerAgentWaitingForCar(MobsimDriverAgent agent) ;
	
}
