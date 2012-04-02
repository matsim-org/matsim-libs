/* *********************************************************************** *
 * project: org.matsim.*
 * PTransitAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.andreas.P2.hook;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author aneumann
 */
public class PTransitAgent extends PersonDriverAgentImpl implements MobsimDriverPassengerAgent {

	private final static Logger log = Logger.getLogger(PTransitAgent.class);

	public static PTransitAgent createTransitAgent(Person p, Netsim simulation) {
		PTransitAgent agent = new PTransitAgent(p, simulation);
		return agent;
	}

	private PTransitAgent(final Person p, final Netsim simulation) {
		super(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), simulation);
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
//		if (line.getId().equals(route.getLineId())) {
			return containsId(stopsToCome, route.getEgressStopId());
//		} else {
//			return false;
//		}
	}

	private Leg getCurrentLeg() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		return (Leg) currentPlanElement;
	}

	private boolean containsId(List<TransitRouteStop> stopsToCome,
			Id egressStopId) {
		for (TransitRouteStop stop : stopsToCome) {
			if (egressStopId.equals(stop.getStopFacility().getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getWeight() {
		return 1.0;
	}

	@Override
	public Id getDesiredAccessStopId() {
		Leg leg = getCurrentLeg();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ (leg.getRoute() instanceof GenericRoute ? ((GenericRoute) leg.getRoute()).getRouteDescription() : ""));
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Id accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}

}
