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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
final class RoadPricingTollCalculator implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	Logger log = Logger.getLogger( RoadPricingTollCalculator.class ) ;

	static class AgentTollInfo {
		public double toll = 0.0;
		boolean insideCordonArea = true;
	}

	final RoadPricingScheme scheme;
	final TreeMap<Id<Person>, AgentTollInfo> agents = new TreeMap<>();
	private final Network network;

	private final TollBehaviourI handler;
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	RoadPricingTollCalculator( final Network network, final Scenario scenario, EventsManager events ) {
		super();
		events.addHandler(this);
		this.network = network;
		this.scheme = RoadPricingUtils.getScheme( scenario ) ;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.handler = new DistanceTollBehaviour(events);
			log.info("just instantiated DistanceTollBehavior") ;
		} else if (RoadPricingScheme.TOLL_TYPE_AREA.equals(scheme.getType())) {
			this.handler = new AreaTollBehaviour(events);
			log.info("just instantiated AreaTollBehavior") ;
			log.warn("area toll does not work if you have different toll amounts on different " +
					"links.  Make sure you get what you want.  kai, mar'12");
		} else if (RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType())) {
			this.handler = new CordonTollBehaviour(events);
			log.info("just instantiated CordonTollBehavior") ;
			log.warn("cordon toll only charges at transition from untolled to tolled. " +
					"Make sure this is what you want. kai, mar'12") ;
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

	/**
	 * Sends {@link PersonMoneyEvent}s for all agents that must pay a toll.
	 * This method should usually be called at the end before of an iteration.
	 *
	 * <strong>Important note: </strong>Do not call this method twice without
	 * calling {@link #reset(int)} in between. Otherwise modules listening to
	 * AgentMoneyEvents will hear them twice, i.e. the toll-disutility
	 * may be added twice to the agents' score!
	 *
	 * @param time the current time the generated events are associated with
	 * @param events the {@link EventsManager} collection, the generated events are sent to for processing
	 */
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


	/**
	 * Returns the toll the specified agent has paid in the course of the
	 * simulation so far.
	 *
	 * @param agentId
	 * @return The toll paid by the specified agent, 0.0 if no toll was paid.
	 */
	double getAgentToll(final Id<Person> agentId) {
		AgentTollInfo info = this.agents.get(agentId);
		if (info == null) {
			return 0.0;
		}
		return info.toll;
	}

	/**
	 * @return The toll paid by all the agents.
	 */
	public double getAllAgentsToll() {
		// public is currently needed. kai, sep'13

		double tolls = 0;
		for (AgentTollInfo ai : this.agents.values()) {
			tolls += (ai == null) ? 0.0 : ai.toll;
		}
		return tolls;
	}

	/**
	 * @return The Number of all the Drawees.
	 */
	public int getDraweesNr() {
		// public is currently needed. kai, sep'13

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
		public void handleEvent(LinkEnterEvent event, Link link );
	}

	/**
	 * Handles the calculation of the distance toll. If an agent enters a link at
	 * a time the toll has to be paid, the toll amount is added to the agent. The
	 * agent does not have to pay the toll for a link if it starts on the link,
	 * as it may have paid already when arriving on the link.
	 */
	class DistanceTollBehaviour implements TollBehaviourI {
		private EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		public DistanceTollBehaviour(EventsManager events) {
			this.events = events;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			CostInfo cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(),
					event.getTime(), driverId, event.getVehicleId() );
			if (cost != null) {
				double newToll = link.getLength() * cost.amount;
				AgentTollInfo info = RoadPricingTollCalculator.this.agents.get(driverId);
				if (info == null) {
					/* The agent is not yet "registered". */
					
					/* Generate a "registration object. */
					info = new AgentTollInfo();
					
					/* Register it. */
					RoadPricingTollCalculator.this.agents.put(driverId, info);
				}
				info.toll += newToll;
				events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-newToll));

			}
		}
	}

	
	class LinkTollBehaviour implements TollBehaviourI {
		private EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		public LinkTollBehaviour(EventsManager events) {
			this.events = events;

		}

		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {

			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			
			CostInfo cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId() );
			if (cost != null) {

				AgentTollInfo info = RoadPricingTollCalculator.this.agents.get(driverId);
				if (info == null) {
					/* The agent is not yet "registered" */

					/* Generate a "registration object" */
					info = new AgentTollInfo();

					/* Register it. */
					RoadPricingTollCalculator.this.agents.put(driverId, info);
				}
				info.toll += cost.amount;
				events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-cost.amount));
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
	class AreaTollBehaviour implements TollBehaviourI {
		private EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		public AreaTollBehaviour(EventsManager events) {
			this.events = events;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			CostInfo cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId() );
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
					info.toll = cost.amount;
					events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-cost.amount));
				}
			}
		}
	}

	/**
	 * Handles the calculation of the cordon toll. An agent has only to pay if he
	 * crosses the cordon from the outside to the inside.
	 */
	class CordonTollBehaviour implements TollBehaviourI {
		private EventsManager events;
		/**
		 * @param events The EventsManager to send money events
		 */
		public CordonTollBehaviour(EventsManager events) {
			this.events = events;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event, final Link link) {
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());
			CostInfo cost = RoadPricingTollCalculator.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), driverId, event.getVehicleId() );
			if (cost != null) {
				// this is a link inside the toll area.
				// [[I guess this assumes that all links inside the cordon are listed in the toll scheme, similar to an area
				// toll.  Conventionally, one would not do it in this way, but one would just name those links where
				// the cordon toll is charged.  kai, mar'12]]
				AgentTollInfo info = RoadPricingTollCalculator.this.agents.get(driverId);
				if (info == null) {
					// (the agent is not yet "registered")
					// [[yyyy this would refer to any toll, so if we have two cordons, it does not work.  kai, mar'12]]

					// generate a "registration object":
					info = new AgentTollInfo();

					// register it:
					RoadPricingTollCalculator.this.agents.put(driverId, info);

					// we start in the area, do not toll:
					info.toll = 0.0;

					// info.insideCordonArea is implicitly initialized with `true'. kai, mar'12
				} else if (!info.insideCordonArea) {
					// (info is != null, and insideCordonArea is false)
					// agent was outside before, now inside the toll area --> agent has to pay
					info.insideCordonArea = true;
					info.toll += cost.amount;
					events.processEvent(new PersonMoneyEvent(event.getTime(),driverId,-cost.amount));
				}
				// else: agent was already in toll area, does not have to pay again (this implementation is a bit unusual!)
			} else {
				// this is a link outside the toll area; just need to memorize that the agent is outside the toll area.
				AgentTollInfo info = RoadPricingTollCalculator.this.agents.get(driverId);
				if (info == null) {
					// (the agent is not yet "registered")

					// generate a "registration object":
					info = new AgentTollInfo();

					// register it:
					RoadPricingTollCalculator.this.agents.put(driverId, info);
				}
				// memorize that agent is outside toll area:
				info.insideCordonArea = false;
			}
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

}
