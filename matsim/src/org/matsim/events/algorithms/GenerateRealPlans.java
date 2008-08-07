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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.utils.misc.Time;

// "GeneratePlansFromEvents" would be more appropriate as class name...
/**
 * Generate plans (resp. persons with each one plan) from events.
 *
 * @author mrieser
 */
public class GenerateRealPlans implements EventHandlerActivityStartI,
		EventHandlerActivityEndI,
		EventHandlerAgentArrivalI,
		EventHandlerAgentDepartureI,
		EventHandlerAgentStuckI,
		EventHandlerLinkEnterI {

	private final Population realplans = new Population(Population.NO_STREAMING);
	private Population oldplans = null;
	private NetworkLayer network = null;

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
	public void setNetworkLayer(final NetworkLayer network) {
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
		leg.setTravTime(time - leg.getDepTime());
		leg.setArrTime(time);
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
					starttime = lastLeg.getArrTime();
				}
				double endtime = time;
				String acttype = "unknown";
				Id linkId = new IdImpl("");
				if (this.oldplans != null) {
					Person person = this.oldplans.getPerson(agentId);
					Act act = (Act)(person.getSelectedPlan().getActsLegs().get(plan.getActsLegs().size()));
					acttype = act.getType();
					linkId = act.getLink().getId();
				}
				plan.createAct(acttype, (String)null, (String)null, linkId.toString(), Time.writeTime(starttime), Time.writeTime(endtime), Time.writeTime(endtime - starttime), "no");
			}

			Leg leg;
			if (event.leg != null) {
				leg = plan.createLeg(event.leg.getMode(), time, Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else {
				leg = plan.createLeg("car", time, Integer.MIN_VALUE, Integer.MIN_VALUE); // maybe get the leg mode from oldplans if available?
			}

			leg.setDepTime(time);
		} catch (Exception e) {
			System.err.println("Agent # " + agentId);
			Gbl.errorMsg(e);
		}
	}

	public void handleEvent(final AgentStuckEvent event) {
		Plan plan;
		double time = event.time;
		Id linkId;
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
				Leg leg = plan.createLeg(event.leg.getMode(), time, 0., time);
				finishLeg(event.agentId, leg);
			}
			if (event.link == null) {
				Plan oldPlan = getOldPlanForPerson(agentId);
				int idx = plan.getActsLegs().size() - 2;
				linkId = ((Act)oldPlan.getActsLegs().get(idx)).getLink().getId();
			} else {
				linkId = event.link.getId();
			}
			plan.createAct("stuck", (String)null, (String)null, linkId.toString(), Time.writeTime(time), Time.writeTime(time), null, "no");
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
				plan.createAct("unknown", (String)null, (String)null, event.linkId, Time.writeTime(event.time), Time.writeTime(event.time), null, "no");
			} else {
				plan.createAct(event.act.getType(), (String)null, (String)null, event.act.getLink().getId().toString(), Time.writeTime(event.time), Time.writeTime(event.time), null, "no");
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
					plan.createAct("unknown", (String)null, (String)null,
							event.linkId, "00:00",
							Time.writeTime(event.time), Time.writeTime(event.time), "no");
				} else {
					plan.createAct(event.act.getType(), (String)null, (String)null,
							event.act.getLink().getId().toString(), "00:00",
							Time.writeTime(event.time), Time.writeTime(event.time), "no");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Act act = (Act)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
			act.setDur(event.time - act.getStartTime());
			act.setEndTime(event.time);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(final LinkEnterEnter event) {
		Link link = null;
		if (event.link != null) {
			link = event.link;
		} else if (this.network != null) {
			link = this.network.getLink(event.linkId);
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
		Route route = new Route();
		route.setRoute(routeNodes);
		leg.setRoute(route);
	}

	private Plan getPlanForPerson(final Person person) {
		Person realperson = this.realplans.getPerson(person.getId());
		if (realperson == null) {
			try {
				realperson = new Person(person.getId());
				realperson.setSex(person.getSex());
				realperson.setAge(person.getAge());
				realperson.setLicence(person.getLicense());
				realperson.setCarAvail(person.getCarAvail());
				realperson.setEmployed(person.getEmployed());
				realperson.createPlan(true);
				this.realplans.addPerson(realperson);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return realperson.getPlans().get(0);
	}

	private Plan getPlanForPerson(final String personId) {
		Person realperson = this.realplans.getPerson(personId);
		if (realperson == null) {
			try {
				realperson = new Person(new IdImpl(personId));
				realperson.createPlan(true);
				this.realplans.addPerson(realperson);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
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
					plan.createAct(act.getType(), (String)null, (String)null, act.getLink().getId().toString(), "00:00", "24:00", "24:00", "no");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (plan.getActsLegs().size() % 2 == 0) {
				// the final act seems missing
				try {
					Act act = (Act)plan.getActsLegs().get(0);
					Leg leg = (Leg)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
					double startTime = leg.getArrTime();
					double endTime = 24*3600;
					if (startTime == Time.UNDEFINED_TIME) {
						// maybe the agent never arrived on time?
						startTime = leg.getDepTime() + 15*60; // just assume some traveltime, e.g. 15 minutes.
					}
					if (endTime < startTime) {
						endTime = startTime + 900; // startTime+15min
					}
					plan.createAct(act.getType(), (String)null, (String)null, act.getLink().getId().toString(), Time.writeTime(startTime), Time.writeTime(endTime), Time.writeTime(endTime - startTime), "no");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// we have a final act, make sure it ends at 24:00
				Act act = (Act)plan.getActsLegs().get(plan.getActsLegs().size() - 1);
				act.setEndTime(24*3600);
				act.setDur(act.getEndTime() - act.getStartTime());
			}
		}
	}

	public void reset(final int iteration) {
	}
}
