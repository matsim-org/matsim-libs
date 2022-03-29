/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingTollCalculator.java
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

package org.matsim.contrib.roadpricing;

import java.util.TreeMap;
import java.util.stream.Collector;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * Calculates the toll agents pay during a simulation by analyzing events. 
 * Add an instance of this class as an EventHandler.
 * 
 * Users of this class can get to the amounts which have to be paid by the 
 * agents by using the getter methods and/or by asking for a stream of 
 * AgentMoneyEvent instances.
 *
 * @author mrieser
 */
public final class RoadPricingTollCalculator implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	Logger log = Logger.getLogger( RoadPricingTollCalculator.class ) ;


	/**
	 * Much of this is no longer needed since we now throw money events immediately when links are left.  It is, however, still
	 * needed for area toll, and for the specific implementation of cordon toll here. kai, jan'21
	 */
	private static class AgentTollInfo {
		public double toll = 0.0;
		boolean insideCordonArea = true;
	}

	private final RoadPricingScheme scheme;

	/**
	 * Much of this is no longer needed since we now throw money events immediately when links are left.  It is, however, still
	 * needed for area toll, and for the specific implementation of cordon toll here. kai, jan'21
	 */
	private final TreeMap<Id<Person>, AgentTollInfo> agents = new TreeMap<>();

	private final Network network;

	private final TollBehaviourI handler;
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	RoadPricingTollCalculator(final Network network, final RoadPricingScheme scheme, EventsManager events ) {
		super();
		events.addHandler(this);
		this.network = network;
		this.scheme = scheme;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.handler = new DistanceTollBehaviour(events);
			log.info("just instantiated DistanceTollBehavior") ;
		} else if (RoadPricingScheme.TOLL_TYPE_AREA.equals(scheme.getType())) {
			this.handler = new AreaTollBehaviour(events);
			log.info("just instantiated AreaTollBehavior") ;
			log.warn("area toll does not work if you have different toll amounts on different " +
					"links.  Make sure you get what you want.  kai, mar'12");
		} else if (RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType())) {
			throw new RuntimeException( "use LINK toll behavior to implement cording pricing (just pay on links that traverse the cordon)" );
		} else if (RoadPricingScheme.TOLL_TYPE_LINK.equals(scheme.getType())) {
			this.handler = new LinkTollBehaviour(events);
			log.info("just instantiated LinkTollBehavior") ;
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		Link link = this.network.getLinks().get(event.getLinkId());
		this.handler.handleEvent(event, link);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
		
		Link link = this.network.getLinks().get(event.getLinkId());
		if (handler instanceof DistanceTollBehaviour || handler instanceof LinkTollBehaviour) {
			/* we do not handle wait2link-events for these tolls, because the agent
			 * should not pay twice for this link, and he (likely) paid already when
			 * arriving at this link.  */
			//noinspection UnnecessaryReturnStatement
			return;
		} else {
			// Just like a LinkEnterEvent
			this.handler.handleEvent(new LinkEnterEvent(event.getTime(), event.getVehicleId(), event.getLinkId()), link);
		}
	}

//	/**
//	 * Sends {@link PersonMoneyEvent}s for all agents that must pay a toll.
//	 * This method should usually be called at the end before of an iteration.
//	 *
//	 * <strong>Important note: </strong>Do not call this method twice without
//	 * calling {@link #reset(int)} in between. Otherwise modules listening to
//	 * AgentMoneyEvents will hear them twice, i.e. the toll-disutility
//	 * may be added twice to the agents' score!
//	 *
//	 * @param time the current time the generated events are associated with
//	 * @param events the {@link EventsManager} collection, the generated events are sent to for processing
//	 */
//	public void sendMoneyEvents(final double time, final EventsManager events) {
//		// public is currently needed. kai, sep'13
//
//		for (Map.Entry<Id<Person>, AgentTollInfo> entries : this.agents.entrySet()) {
//			events.processEvent(new PersonMoneyEvent(time, entries.getKey(), -entries.getValue().toll));
//		}
//	}

	@Override
	public void reset(final int iteration) {
		this.agents.clear();
		delegate.reset(iteration);
	}


//	/**
//	 * Returns the toll the specified agent has paid in the course of the
//	 * simulation so far.
//	 *
//	 * @param agentId
//	 * @return The toll paid by the specified agent, 0.0 if no toll was paid.
//	 */
//	double getAgentToll(final Id<Person> agentId) {
//		AgentTollInfo info = this.agents.get(agentId);
//		if (info == null) {
//			return 0.0;
//		}
//		return info.toll;
//	}

	/**
	 * @return The toll paid by all the agents.
	 */
	double getAllAgentsToll() {
		double tolls = 0;
		for (AgentTollInfo ai : this.agents.values()) {
			tolls += (ai == null) ? 0.0 : ai.toll;
		}
		return tolls;
	}

	/**
	 * @return The Number of all the Drawees.
	 */
	int getDraweesNr() {
		int dwCnt = 0;
		for (AgentTollInfo ai : this.agents.values()) {
			if ((ai != null) && (ai.toll > 0.0)) {
				dwCnt++;
			}
		}
		return dwCnt;
	}

	/**
	 * A simple interface to implement different toll schemes.
	 */
	private interface TollBehaviourI {
		void handleEvent( LinkEnterEvent event, Link link );
	}

	/**
	 * Handles the calculation of the distance toll. If an agent enters a link at
	 * a time the toll has to be paid, the toll amount is added to the agent. The
	 * agent does not have to pay the toll for a link if it starts on the link,
	 * as it may have paid already when arriving on the link.
	 */
	private class DistanceTollBehaviour implements TollBehaviourI {
		private final EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		public DistanceTollBehaviour(EventsManager events) {
			this.events = events;
		}
		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			Cost cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId());
			if (cost != null) {
				double newToll = link.getLength() * cost.amount;
				events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-newToll,"toll",null));

				AgentTollInfo info = RoadPricingTollCalculator.this.agents.computeIfAbsent( driverId, (key) -> new AgentTollInfo() );
				info.toll += newToll;
			}
		}
	}

	
	private class LinkTollBehaviour implements TollBehaviourI {
		private final EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		private LinkTollBehaviour(EventsManager events) {
			this.events = events;

		}
		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			Cost cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId() );
			if (cost != null) {
				events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-cost.amount,"toll",null));

				AgentTollInfo info = RoadPricingTollCalculator.this.agents.computeIfAbsent( driverId, (key) -> new AgentTollInfo() );
				info.toll += cost.amount;
			}
		}
	}

	/** Handles the calculation of the area toll. Whenever the agent is seen on
	 * one of the tolled link, the constant toll amount has to be paid once.
	 * <br>
	 * Design comments:
	 * <ul>
	 * 		<li> This implementation becomes a problem if someone tries to 
	 * 			 implement more than one area which do not share the same flat 
	 * 			 fee.  kai, mar'12
	 * </ul> 
	 */
	private class AreaTollBehaviour implements TollBehaviourI {
		private final EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		private AreaTollBehaviour(EventsManager events) {
			this.events = events;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			Cost cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId() );
			if (cost != null) {
				AgentTollInfo info = RoadPricingTollCalculator.this.agents.get(driverId);
				if (info == null) {
					/* The agent is not yet "registered" */

					/* Generate a "registration object" */
					info = new AgentTollInfo();

					/* Register it. */
					RoadPricingTollCalculator.this.agents.put(driverId, info);

					/* The toll amount comes from the current link, but should 
					 * be the same for all links. */
					events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-cost.amount,"toll",null));

					info.toll = cost.amount;
				}
			}
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

}
