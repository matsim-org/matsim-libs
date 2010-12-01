/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterAssigmentSimulatedAnnealingRePlanner.java
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
package playground.gregor.sims.shelters.assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.config.EvacuationConfigGroup;

/**
 * @author laemmel
 * 
 */
public class ShelterAssignmentSimulatedAnnealingRePlanner implements IterationStartsListener {

	private static final Logger log = Logger.getLogger(ShelterAssignmentSimulatedAnnealingRePlanner.class);

	// private TravelTime tt;
	// private TravelCost tc;
	/* package */ScenarioImpl sc;
	/* package */ArrayList<Person> agents;
	/* package */NetworkFactoryImpl routeFactory;
	/* package */final boolean swNash;
	/* package */List<Entry<Id, Shelter>> shelterList = new ArrayList<Entry<Id, Shelter>>();

	// simulate annealing related stuff begins here
	/* package */double L_k = 0; // number of transitions in current iteration
	/* package */double c_k = 0; // current temperature

	// 
	/* package */HashMap<Id, Shelter> shelterLinkMapping;

	/* package */Dijkstra router;

	/* package */PopulationFactory popFac;

	// private final double c_decrement;

	// private final double L_decrement;

	int proposed = 0;
	int accepted = 0;

	/* package */double delta = 0.1;
	/* package */double chi_0 = 0.95;
	/* package */double epsilon_s = 0.02;

	private boolean melted = false;
	private double m1;
	private double m2;
	private double deltaFDecrSum;
	private double fAvgSum = 0;
	private double fBefore = 0;
	private final List<Double> fTransitions = new ArrayList<Double>();

	public ShelterAssignmentSimulatedAnnealingRePlanner(ScenarioImpl sc, TravelCost tc, TravelTime tt, HashMap<Id, Building> shelterLinkMapping) {
		this.router = new Dijkstra(sc.getNetwork(), tc, tt);
		this.sc = sc;
		// this.tc = tc;
		// this.tt = tt;
		this.agents = new ArrayList<Person>(sc.getPopulation().getPersons().values());
		this.routeFactory = sc.getNetwork().getFactory();
		this.swNash = !((EvacuationConfigGroup) sc.getConfig().getModule("evacuation")).isSocialCostOptimization();
		// this.swNash = false;
		this.shelterLinkMapping = new HashMap<Id, Shelter>();
		this.popFac = sc.getPopulation().getFactory();
		for (Link link : sc.getNetwork().getLinks().values()) {
			if ((link.getId().toString().contains("sl") && link.getId().toString().contains("b"))) {
				Building b = shelterLinkMapping.get(link.getId());
				int cap = b.getShelterSpace();
				Shelter t = new Shelter(cap, 0);
				this.shelterLinkMapping.put(link.getId(), t);
			} else if (link.getId().toString().equals("el1")) {
				int cap = this.agents.size();
				Shelter t = new Shelter(cap, 0);
				this.shelterLinkMapping.put(link.getId(), t);
			}
		}

		this.c_k = 0.1;

		validateShelterUtilization();

	}

	/**
	 * counts the number of agents in each shelter
	 * 
	 * @throws RuntimeException
	 *             if more agents in shelter as shelter has space
	 */
	/* package */void validateShelterUtilization() {
		for (Shelter s : this.shelterLinkMapping.values()) {
			s.count = 0;
		}
		for (Person pers : this.agents) {
			Activity act = (Activity) pers.getSelectedPlan().getPlanElements().get(2);
			Id id = act.getLinkId();
			Shelter t = this.shelterLinkMapping.get(id);
			t.count++;
			if (t.count > t.cap) {
				throw new RuntimeException("More agents in shelter than allowed! Shelter on link:" + id + "  capacity:" + t.cap + "  count:" + t.count);

			}
		}
		this.shelterList.clear();
		for (Entry<Id, Shelter> e : this.shelterLinkMapping.entrySet()) {
			if (e.getValue().cap > e.getValue().count) {
				this.shelterList.add(e);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.matsim.core.controler.listener.IterationStartsListener#
	 * notifyIterationStarts
	 * (org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.melted) {

			double sqrSum = 0;
			double fAvg = this.fAvgSum / this.L_k;
			for (int i = 0; i < this.L_k; i++) {
				Double d = this.fTransitions.get(i);
				sqrSum += Math.pow((d - fAvg), 2);
			}
			double sigma_k = Math.sqrt(sqrSum / this.L_k);
			double denom = 1 + (this.c_k * Math.log(1 + this.delta)) / (3. * sigma_k);

			double old = this.c_k;
			this.c_k = this.c_k / denom;
			this.L_k = (int) (0.1 * this.agents.size());// TODO does this make
			// sense

			log.info(" old control parameter = " + old + "  sigma_k =" + sigma_k + "  new control parameter = " + this.c_k);
			run();
			validateShelterUtilization();
			log.info("proposed transitions = " + this.proposed + "   accepted transistions = " + this.accepted + "  ratio =" + (double) this.accepted / this.proposed + "  control parameter = " + this.c_k);
		} else if (event.getIteration() >= 1) {
			this.m1 = 0;
			this.m2 = 0;
			this.deltaFDecrSum = 0;

			this.L_k = (int) (0.1 * this.agents.size());// TODO does this make
			// sense
			run();
			double deltaFDecr = this.deltaFDecrSum / this.m2;
			validateShelterUtilization();
			log.info("proposed transitions = " + this.proposed + "   accepted transistions = " + this.accepted + "  ratio =" + (double) this.accepted / this.proposed + "  control parameter = " + this.c_k);
			double strength = Math.exp(-deltaFDecr / this.c_k);
			double chi = (this.m1 + this.m2 * strength) / (this.m1 + this.m2);
			if (chi >= this.chi_0) {
				this.melted = true;
				log.info("System is melted! Initial control parameter c_0:" + this.c_k);
			} else {
				this.c_k = deltaFDecr / Math.log(this.m2 / (this.m2 * this.chi_0 - this.m1 * (1 - this.chi_0)));
				log.info("System is not yet melted! New control parameter c:" + this.c_k + " current chi:" + chi);
			}

		}
	}

	/**
	 * 
	 */
	private void run() {
		log.info("starting shelter assignment re-planning");
		this.fBefore = 0;
		this.fAvgSum = 0;
		this.fTransitions.clear();
		for (Person p : this.agents) {
			this.fBefore += Math.min(((Leg) p.getSelectedPlan().getPlanElements().get(1)).getTravelTime(), 30 * 6);
		}

		this.proposed = (int) this.L_k;
		this.accepted = 0;
		for (int i = 0; i < this.L_k; i++) {
			Person p = null;
			do {
				int idx = MatsimRandom.getRandom().nextInt(this.agents.size());
				p = this.agents.get(idx);
			} while (!generateAndTest((PersonImpl) p));
		}

	}

	/**
	 * 
	 * @param p
	 * @return success
	 */
	private boolean generateAndTest(PersonImpl p) {
		if (MatsimRandom.getRandom().nextBoolean()) {
			return generateAndTestShift(p);
		} else {
			return generateAndTestSwitch(p);
		}
	}

	/**
	 * 
	 * @param p1
	 * @return
	 */
	private boolean generateAndTestSwitch(PersonImpl p1) {

		Plan plan1 = p1.getSelectedPlan();
		Activity actOrigin1 = (Activity) plan1.getPlanElements().get(0);
		Node origin1 = this.sc.getNetwork().getLinks().get(actOrigin1.getLinkId()).getToNode();
		Activity actDestination1 = (Activity) plan1.getPlanElements().get(2);
		Link linkDestination1 = this.sc.getNetwork().getLinks().get(actDestination1.getLinkId());
		Node nodeDestination1 = linkDestination1.getFromNode();
		Leg leg1 = (Leg) plan1.getPlanElements().get(1);
		double f1_current = Math.min(leg1.getTravelTime() / 600, 30 * 6);

		Person p2 = null;
		boolean found = false;
		while (!found) {
			p2 = this.agents.get(MatsimRandom.getRandom().nextInt(this.agents.size()));
			if (((Activity) p2.getSelectedPlan().getPlanElements().get(2)).getLinkId() != actDestination1.getLinkId()) {
				found = true;
			}
		}

		Plan plan2 = p2.getSelectedPlan();
		Activity actOrigin2 = (Activity) plan2.getPlanElements().get(0);
		Node origin2 = this.sc.getNetwork().getLinks().get(actOrigin2.getLinkId()).getToNode();
		Activity actDestination2 = (Activity) plan2.getPlanElements().get(2);
		Link linkDestination2 = this.sc.getNetwork().getLinks().get(actDestination2.getLinkId());
		Node nodeDestination2 = linkDestination2.getFromNode();
		Leg leg2 = (Leg) plan2.getPlanElements().get(1);
		double f2_current = Math.min(leg2.getTravelTime() / 600, 30 * 6);

		Path path1 = this.router.calcLeastCostPath(origin1, nodeDestination2, actOrigin1.getEndTime());
		Path path2 = this.router.calcLeastCostPath(origin2, nodeDestination1, actOrigin2.getEndTime());
		double t = this.sc.getConfig().charyparNagelScoring().getTraveling_utils_hr();

		double f1_after = path1.travelTime / 600;
		double f2_after = path2.travelTime / 600;

		if (f1_after >= 30 * 6 || f2_after >= 30 * 6) {
			return false;
		}

		if (Double.isInfinite(f1_current) || Double.isInfinite(f2_current)) {
			throw new RuntimeException("got inifity");
		}

		double prob = 0; // probability to switch

		double deltaF = 0;

		if (this.swNash) {
			// in order to switch according to Nash constraints both agents have
			// to accept
			if (f1_after < f1_current && f2_after < f2_current) {
				prob = 1;
				this.m1++;

			} else {
				double prob1 = Math.exp((f1_current - f1_after) / this.c_k);
				double prob2 = Math.exp((f2_current - f2_after) / this.c_k);
				prob = prob1 * prob2;
				this.deltaFDecrSum += (f1_after + f2_after) - (f1_current + f2_current);
				this.m2++;
			}
			deltaF = (f1_after + f2_after) - (f1_current + f2_current);
		} else {
			// in order to switch according to SO constraints the agents have to
			// accept on average
			double contrib_current = f1_current + f2_current; // current
			// contribution
			// to system
			// costs
			double contrib_after = f1_after + f2_after; // current contribution
			// to system costs
			if (contrib_current > contrib_after) {
				prob = 1;
				this.m1++;
			} else {
				prob = Math.exp((contrib_current - contrib_after) / this.c_k);

				this.deltaFDecrSum += contrib_after - contrib_current;
				this.m2++;

			}
			deltaF = (contrib_after - contrib_current);
		}
		boolean switchAgents = false;
		if (prob > MatsimRandom.getRandom().nextDouble()) {
			switchAgents = true;
			this.fBefore += deltaF;

		}
		if (switchAgents) {
			this.accepted++;
			// person 1
			Plan newPlan1 = this.popFac.createPlan();
			newPlan1.addActivity(actOrigin1);

			Leg leg1new = this.popFac.createLeg("car");
			leg1new.setDepartureTime(actOrigin1.getEndTime());
			NetworkRoute route1 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, actOrigin1.getLinkId(), actDestination2.getLinkId());
			route1.setLinkIds(actOrigin1.getLinkId(), NetworkUtils.getLinkIds(path1.links), actDestination2.getLinkId());
			route1.setDistance(RouteUtils.calcDistance(route1, this.sc.getNetwork()));
			leg1new.setRoute(route1);
			leg1new.setTravelTime(path1.travelTime);
			newPlan1.addLeg(leg1new);

			Activity newActivityDestination1 = this.popFac.createActivityFromLinkId(actOrigin1.getType(), actDestination2.getLinkId());
			newPlan1.addActivity(newActivityDestination1);

			// newPlan1.setScore(path1.travelCost/-600);

			p1.addPlan(newPlan1);
			(p1).setSelectedPlan(newPlan1);
			(p1).removeUnselectedPlans();

			// person 2
			Plan newPlan2 = this.popFac.createPlan();
			newPlan2.addActivity(actOrigin2);

			Leg leg2new = this.popFac.createLeg("car");
			leg2new.setDepartureTime(actOrigin2.getEndTime());
			NetworkRoute route2 = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, actOrigin2.getLinkId(), actDestination1.getLinkId());
			route2.setLinkIds(actOrigin2.getLinkId(), NetworkUtils.getLinkIds(path2.links), actDestination1.getLinkId());
			route2.setDistance(RouteUtils.calcDistance(route2, this.sc.getNetwork()));
			leg2new.setRoute(route2);
			leg2new.setTravelTime(path2.travelTime);
			newPlan2.addLeg(leg2new);

			Activity newActivityDestination2 = this.popFac.createActivityFromLinkId(actOrigin2.getType(), actDestination1.getLinkId());
			newPlan2.addActivity(newActivityDestination2);

			// newPlan2.setScore(path2.travelCost/-600);

			p2.addPlan(newPlan2);
			((PersonImpl) p2).setSelectedPlan(newPlan2);
			((PersonImpl) p2).removeUnselectedPlans();

		}
		if (this.deltaFDecrSum < 0) {
			log.error("not possible!");
		}

		this.fAvgSum += this.fBefore;
		this.fTransitions.add(this.fBefore);
		return true;
	}

	/**
	 * 
	 * @param p
	 * @return success
	 */
	private boolean generateAndTestShift(Person p) {
		Entry<Id, Shelter> e = null;
		boolean found = false;
		while (!found) {
			e = this.shelterList.get(MatsimRandom.getRandom().nextInt(this.shelterList.size()));
			if (e.getValue().cap > e.getValue().count) {
				found = true;
			}
		}

		Plan plan = p.getSelectedPlan();
		Leg leg = (Leg) plan.getPlanElements().get(1);
		double f_current = leg.getTravelTime() / 600;

		Activity actOrigin = (Activity) plan.getPlanElements().get(0);
		Node origin = this.sc.getNetwork().getLinks().get(actOrigin.getLinkId()).getToNode();

		Link testDestinationLink = this.sc.getNetwork().getLinks().get(e.getKey());

		// test for NULL shift
		if (testDestinationLink.getId() == ((Activity) plan.getPlanElements().get(2)).getLinkId()) {
			return false;
		}

		Node testDestinationNode = this.sc.getNetwork().getLinks().get(e.getKey()).getFromNode();

		Path path = this.router.calcLeastCostPath(origin, testDestinationNode, actOrigin.getEndTime());
		double t = this.sc.getConfig().charyparNagelScoring().getTraveling_utils_hr();

		double f_after = path.travelTime / 600;

		// make sure that we do not get infinities
		f_current = Math.min(f_current, 30 * 6);
		f_after = Math.min(f_after, 30 * 6);

		double prob = 0;

		// System.out.println("f_after:" + f_after + " f_current:" + f_current +
		// " prob:" + Math.exp((f_after - f_current)/this.c_k));
		boolean shiftAgent = false;
		if (f_after <= f_current) {
			prob = 1;
			this.m1++;
		} else {
			prob = Math.exp((f_current - f_after) / this.c_k);
			this.m2++;
			this.deltaFDecrSum += f_after - f_current;

		}

		if (prob > MatsimRandom.getRandom().nextDouble()) {
			shiftAgent = true;

		}

		if (shiftAgent) {
			this.accepted++;
			this.fBefore += (f_after - f_current);
			Activity oldDestination = (Activity) plan.getPlanElements().get(2);
			this.shelterLinkMapping.get(oldDestination.getLinkId()).count--;
			//
			Plan newPlan = this.popFac.createPlan();
			newPlan.addActivity(actOrigin);

			Leg legNew = this.popFac.createLeg("car");
			leg.setDepartureTime(actOrigin.getEndTime());
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, actOrigin.getLinkId(), testDestinationLink.getId());
			route.setLinkIds(actOrigin.getLinkId(), NetworkUtils.getLinkIds(path.links), testDestinationLink.getId());
			route.setDistance(RouteUtils.calcDistance(route, this.sc.getNetwork()));
			legNew.setRoute(route);
			legNew.setTravelTime(path.travelTime);
			newPlan.addLeg(legNew);

			Activity newActivityDestination = this.popFac.createActivityFromLinkId(actOrigin.getType(), testDestinationLink.getId());
			newPlan.addActivity(newActivityDestination);

			// newPlan.setScore(path.travelCost/-600);

			p.addPlan(newPlan);
			((PersonImpl) p).setSelectedPlan(newPlan);
			((PersonImpl) p).removeUnselectedPlans();
			//

			e.getValue().count++;
		}
		this.fAvgSum += this.fBefore;
		this.fTransitions.add(this.fBefore);
		return true;
	}

	/* package */static class Shelter {

		int cap = 0;
		int count = 0;

		/**
		 * @param cap2
		 *            capacity
		 * @param i
		 *            utilization
		 */
		public Shelter(int cap, int i) {
			this.cap = cap;
			this.count = i;
		}

	}
}
