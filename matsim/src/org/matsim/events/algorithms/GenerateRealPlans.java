/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRealPlans.java
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

package org.matsim.events.algorithms;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.utils.misc.Time;

// "GeneratePlansFromEvents" would be more appropriate as class name...
/**
 * Generate plans (resp. persons with each one plan) from events.
 *
 * @author mrieser
 */
public class GenerateRealPlans implements ActStartEventHandler,
		ActEndEventHandler,
		AgentArrivalEventHandler,
		AgentDepartureEventHandler,
		AgentStuckEventHandler,
		LinkEnterEventHandler {

	private final Population realplans = new PopulationImpl(PopulationImpl.NO_STREAMING);
	private Population oldplans = null;
	private Network network = null;

	// routes = TreeMap<agent-id, route-nodes = ArrayList<nodes>>
	private final TreeMap<String, ArrayList<Node>> routes = new TreeMap<String, ArrayList<Node>>();

	public GenerateRealPlans() {
		super();
	}

	public GenerateRealPlans(final Population plans) {
		super();
		this.oldplans = plans;
	}

	/**
	 * Sets the network used to look up links and nodes, when only the
	 * corresponding Ids are given in the event and not the objects themselves.
	 *
	 * @param network the network used for lookups
	 */
	public void setNetworkLayer(final Network network) {
		this.network = network;
	}

	public void handleEvent(final AgentArrivalEvent event) {
		Plan plan;
		double time = event.time;
		if (event.agent != null) {
			plan = getPlanForPerson(event.agent);
		} else {
			plan = getPlanForPerson(event.agentId);
		}
		Leg leg = (Leg)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
		leg.setTravelTime(time - leg.getDepartureTime());
		leg.setArrivalTime(time);
		finishLeg(event.agentId, leg);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		Plan plan;
		String agentId;
		double time = event.time;
		if (event.agent != null) {
			plan = getPlanForPerson(event.agent);
			agentId = event.agent.getId().toString();
		} else {
			plan = getPlanForPerson(event.agentId);
			agentId = event.agentId;
		}
		try {

			if (plan.getActsLegs().size() % 2 == 0) {
				// the last thing in our plan is a leg; it seems we don't receive ActStart- and ActEnd-events
				// add the last act from the original plan if possible
				double starttime = 0;
				if (plan.getActsLegs().size() > 0) {
					Leg lastLeg = (Leg)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
					starttime = lastLeg.getArrivalTime();
				}
				double endtime = time;
				String acttype = "unknown";
				if (this.oldplans != null) {
					Person person = this.oldplans.getPerson(agentId);
					Act act = (Act)(person.getSelectedPlan().getActsLegs().get(plan.getActsLegs().size()));
					acttype = act.getType();
				}
				Act a = plan.createAct(acttype, event.link);
				a.setStartTime(starttime);
				a.setEndTime(endtime);
				a.setDuration(endtime - starttime);
			}

			Leg leg;
			if (event.leg != null) {
				leg = plan.createLeg(event.leg.getMode());
				leg.setDepartureTime(time);
			} else {
				leg = plan.createLeg(BasicLeg.Mode.car); // maybe get the leg mode from oldplans if available?
				leg.setDepartureTime(time);
			}

			leg.setDepartureTime(time);
		} catch (Exception e) {
			System.err.println("Agent # " + agentId);
			Gbl.errorMsg(e);
		}
	}

	public void handleEvent(final AgentStuckEvent event) {
		Plan plan;
		double time = event.time;
		String agentId;
		try {
			if (event.agent != null) {
				plan = getPlanForPerson(event.agent);
				agentId = event.agent.getId().toString();
			} else {
				plan = getPlanForPerson(event.agentId);
				agentId = event.agentId;
			}
			if (plan.getActsLegs().size() % 2 != 0) {
				// not all agents must get stuck on a trip: if the simulation is ended early, some agents may still be doing some activity
				// insert for those a dummy leg so we can safely create the stuck-act afterwards
				Leg leg = plan.createLeg(event.leg.getMode());
				leg.setDepartureTime(time);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(time);
				finishLeg(event.agentId, leg);
			}
			Link link = event.link;
			if (link == null) {
				Plan oldPlan = getOldPlanForPerson(agentId);
				int idx = plan.getActsLegs().size() - 2;
				link = ((Act)oldPlan.getActsLegs().get(idx)).getLink();
			}
			Act a = plan.createAct("stuck", link);
			a.setStartTime(time);
			a.setEndTime(time);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(final ActStartEvent event) {
		Plan plan;
		if (event.agent != null) {
			plan = getPlanForPerson(event.agent);
		} else {
			plan = getPlanForPerson(event.agentId);
		}
		try {
			if (event.act == null) {
				Act a = plan.createAct("unknown", event.link);
				a.setStartTime(event.time);
				a.setEndTime(event.time);
			} else {
				Act a = plan.createAct(event.act.getType(), event.act.getLink());
				a.setStartTime(event.time);
				a.setEndTime(event.time);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(final ActEndEvent event) {
		Plan plan;
		if (event.agent != null) {
			plan = getPlanForPerson(event.agent);
		} else {
			plan = getPlanForPerson(event.agentId);
		}
		if (plan.getActsLegs().size() == 0) {
			// this is the first act that ends, we didn't get any ActStartEvent for that,
			// so create this first activity now with an assumed start-time of midnight
			try {
				if (event.act == null) {
					Act a = plan.createAct("unknown", event.link);
					a.setEndTime(event.time);
				} else {
					Act a = plan.createAct(event.act.getType(), event.act.getLink());
					a.setEndTime(event.time);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Act act = (Act)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
			act.setDuration(event.time - act.getStartTime());
			act.setEndTime(event.time);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(final LinkEnterEvent event) {
		Link link = null;
		if (event.link != null) {
			link = event.link;
		} else if (this.network != null) {
			link = this.network.getLink(new IdImpl(event.linkId));
		}
		if (link != null) {
			String agentId = event.agentId;
			Node node = link.getFromNode();
			ArrayList<Node> routeNodes = this.routes.get(agentId);
			if (routeNodes == null) {
				routeNodes = new ArrayList<Node>();
			}
			routeNodes.add(node);
			this.routes.put(agentId, routeNodes);
		}
	}

	private void finishLeg(final String agentId, final Leg leg) {
		ArrayList<Node> routeNodes = this.routes.remove(agentId);
		CarRoute route = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		route.setNodes(routeNodes);
		leg.setRoute(route);
	}

	private Plan getPlanForPerson(final Person person) {
		Person realperson = this.realplans.getPerson(person.getId());
		if (realperson == null) {
			realperson = new PersonImpl(person.getId());
			realperson.setSex(person.getSex());
			realperson.setAge(person.getAge());
			realperson.setLicence(person.getLicense());
			realperson.setCarAvail(person.getCarAvail());
			realperson.setEmployed(person.getEmployed());
			realperson.createPlan(true);
			this.realplans.addPerson(realperson);
		}
		return realperson.getPlans().get(0);
	}

	private Plan getPlanForPerson(final String personId) {
		Person realperson = this.realplans.getPerson(personId);
		if (realperson == null) {
			realperson = new PersonImpl(new IdImpl(personId));
			realperson.createPlan(true);
			this.realplans.addPerson(realperson);
		}
		return realperson.getPlans().get(0);
	}

	private Plan getOldPlanForPerson(final String personId) {
		return this.oldplans.getPerson(personId).getSelectedPlan();
	}


	public Population getPlans() {
		return this.realplans;
	}

	public void finish() {
		// makes sure all plans end with an act
		// necessary when actend- and actstart-events are not available
		for (Person person : this.realplans.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			if (plan.getActsLegs().size() == 0) {
				// the person does not have any activity at all
				try {
					Plan oldPlan = getPlanForPerson(person);
					Act act = (Act)oldPlan.getActsLegs().get(0);
					Act act2 = plan.createAct(act.getType(), act.getLink());
					act2.setStartTime(0.0);
					act2.setEndTime(24.0*3600);
					act2.setDuration(24.0*3600);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (plan.getActsLegs().size() % 2 == 0) {
				// the final act seems missing
				try {
					Act act = (Act)plan.getActsLegs().get(0);
					Leg leg = (Leg)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
					double startTime = leg.getArrivalTime();
					double endTime = 24*3600;
					if (startTime == Time.UNDEFINED_TIME) {
						// maybe the agent never arrived on time?
						startTime = leg.getDepartureTime() + 15*60; // just assume some travel time, e.g. 15 minutes.
					}
					if (endTime < startTime) {
						endTime = startTime + 900; // startTime+15min
					}
					Act act2 = plan.createAct(act.getType(), act.getLink());
					act2.setStartTime(startTime);
					act2.setEndTime(endTime);
					act2.setDuration(endTime - startTime);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// we have a final act, make sure it ends at 24:00
				Act act = (Act)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
				act.setEndTime(24*3600);
				act.setDuration(act.getEndTime() - act.getStartTime());
			}
		}
	}

	public void reset(final int iteration) {
	}
}
