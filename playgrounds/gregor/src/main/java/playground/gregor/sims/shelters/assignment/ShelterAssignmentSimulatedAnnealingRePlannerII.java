/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterAssignmentSimulatedAnnealingRePlannerII.java
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
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.evacuation.base.Building;

/**
 * @author laemmel
 * 
 */
public class ShelterAssignmentSimulatedAnnealingRePlannerII extends ShelterAssignmentSimulatedAnnealingRePlanner implements IterationStartsListener {

	private static final Logger log = Logger.getLogger(ShelterAssignmentSimulatedAnnealingRePlannerII.class);

	boolean melted = false;

	private final int epochLength;
	private int epochNum = 0;
	private final int minEpochs = 2;
	private double fEpochsSum = 0;

	private double fCurrent = 0;

	private double deltaFDecrSum;

	private double fEpoch;

	private int iteration = 0;

	private final List<Double> fMutations = new ArrayList<Double>();

	/**
	 * @param sc
	 * @param tc
	 * @param tt
	 * @param shelterLinkMapping
	 */
	public ShelterAssignmentSimulatedAnnealingRePlannerII(ScenarioImpl sc, TravelCost tc, TravelTime tt, HashMap<Id, Building> shelterLinkMapping) {
		super(sc, tc, tt, shelterLinkMapping);
		this.epochLength = (int) (0.05 * this.agents.size());// TODO does this
		// make sense
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @seeplayground.gregor.sims.shelters.assignment.
	 * ShelterAssignmentSimulatedAnnealingRePlanner
	 * #notifyIterationStarts(org.matsim
	 * .core.controler.events.IterationStartsEvent)
	 */
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.melted) {
			run();
		} else if (event.getIteration() >= 10) {
			melt();
		}
		validateShelterUtilization();
	}

	/**
	 * 
	 */
	private void melt() {

		double chi = 0;
		while (!this.melted) {
			this.deltaFDecrSum = 0;
			this.accepted = 0;
			this.proposed = 0;
			for (int i = 0; i < this.epochLength; i++) {
				mutate();
			}

			double m1 = this.accepted;
			double m2 = this.proposed - this.accepted;
			double deltaFDecr = this.deltaFDecrSum / m2;

			chi = (m1 + m2 * Math.exp(-deltaFDecr / this.c_k)) / this.proposed;
			if (chi < this.chi_0) {
				double tmp = m2 / (m2 * this.chi_0 - m1 * (1 - this.chi_0));
				this.c_k = deltaFDecr / Math.log(tmp);
				log.info("System not yet melted! new  c_0 = " + this.c_k);
			} else {
				this.melted = true;
			}

		}

	}

	/**
	 * 
	 */
	private void run() {
		this.epochNum++;
		this.fEpoch = 0;
		this.fCurrent = 0;
		this.accepted = 0;
		this.proposed = 0;

		for (Person p : this.agents) {
			double time = Math.min(((Leg) p.getSelectedPlan().getPlanElements().get(1)).getTravelTime() / 600, 30 * 6);
			this.fCurrent += time;
		}

		log.info("DEBUG: fCurrent: " + this.fCurrent);
		for (int i = 0; i < this.epochLength; i++) {
			mutate();
		}
		this.fEpoch /= this.epochLength;
		log.info("DEBUG: fEpoch: " + this.fEpoch + "  fCurrent: " + this.fCurrent + "  proposed: " + this.proposed + "  accepted: " + this.accepted);

		this.fEpochsSum += this.fEpoch;
		if (this.epochNum >= this.minEpochs) {
			boolean eq = testForEquilibrium();
			if (eq) {
				reduceTemperature();
			}
		}
		log.info("proposed transitions = " + this.proposed + "   accepted transistions = " + this.accepted + "  ratio =" + (double) this.accepted / this.proposed + "  control parameter = " + this.c_k);

	}

	/**
	 * 
	 */
	private void reduceTemperature() {
		this.iteration++;
		double L = this.fMutations.size();
		double f_est = this.fEpochsSum / this.epochNum;

		double sigSqrSum = 0;
		for (Double f_i : this.fMutations) {
			sigSqrSum += Math.pow(f_i - f_est, 2);
		}
		double sigma = Math.sqrt(sigSqrSum / L);

		this.c_k = this.c_k / (1 + this.c_k * Math.log(1 + this.delta) / (3 * sigma));

		log.info("temperature reduced! New c_k = " + this.c_k + "  sigma = " + sigma);
		// reset everything
		this.fMutations.clear();
		this.epochNum = 0;
		this.fEpochsSum = 0;
	}

	/**
	 * @return equilibrium reached
	 */
	private boolean testForEquilibrium() {
		double absDiff = Math.abs(this.fEpoch - this.fEpochsSum / this.epochNum);
		double tmp = absDiff / (this.fEpochsSum / this.epochNum);
		log.info("eq test tmp = " + tmp);
		if (tmp <= this.epsilon_s) {
			return true;
		}
		return false;
	}

	private void mutate() {
		Person p = null;
		do {
			int idx = MatsimRandom.getRandom().nextInt(this.agents.size());
			p = this.agents.get(idx);
		} while (!generateAndTest((PersonImpl) p));
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

		Node testDestinationNode = this.sc.getNetwork().getLinks().get(e.getKey()).getFromNode();

		// test for NULL shift
		if (testDestinationLink.getId() == ((Activity) plan.getPlanElements().get(2)).getLinkId()) {
			return false;
		}

		Path path = this.router.calcLeastCostPath(origin, testDestinationNode, actOrigin.getEndTime());

		double f_after = path.travelTime / 600;

		// invalid mutation
		if (f_after >= 60 * 6) {
			return false;
		}

		// make sure that we do not get infinities
		f_current = Math.min(f_current, 30 * 6);
		// f_after = Math.min(f_after, 30 * 6);

		double prob = 0;
		boolean shiftAgent = false;
		double deltaF = f_after - f_current;
		if (f_after <= f_current) {
			prob = 1;
		} else {

			prob = Math.exp(-deltaF / this.c_k);
			if (!this.melted) {
				this.deltaFDecrSum += deltaF;
			}
		}

		if (prob > MatsimRandom.getRandom().nextDouble()) {
			shiftAgent = true;

		}

		this.proposed++;
		if (shiftAgent) {
			this.fCurrent += deltaF;
			this.fEpoch += this.fCurrent;
			this.accepted++;

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

			p.addPlan(newPlan);
			((PersonImpl) p).setSelectedPlan(newPlan);
			((PersonImpl) p).removeUnselectedPlans();

			e.getValue().count++;

			this.fMutations.add(this.fCurrent);
			return true;
		} else {
			this.fMutations.add(this.fCurrent);
			return false;
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

		double f1_after = Math.min(path1.travelTime / 600, 30 * 6);
		double f2_after = Math.min(path2.travelTime / 600, 30 * 6);

		// invalid mutation
		if (f1_after >= 60 * 6 || f2_after >= 60 * 6) {
			return false;
		}

		double prob = 0; // probability to switch

		double deltaF = 0;

		if (this.swNash) {
			// in order to switch according to Nash constraints both agents have
			// to accept
			deltaF = (f1_after + f2_after) - (f1_current + f2_current);
			if (f1_after < f1_current && f2_after < f2_current) {
				prob = 1;

			} else {
				double prob1 = Math.exp((f1_current - f1_after) / this.c_k);
				double prob2 = Math.exp((f2_current - f2_after) / this.c_k);
				prob = prob1 * prob2;
				if (!this.melted) {
					this.deltaFDecrSum += Math.max(deltaF, 0);
				}
			}
		} else {
			// in order to switch according to SO constraints the agents have to
			// accept on average
			double contrib_current = f1_current + f2_current; // current
			// contribution
			// to system
			// costs
			double contrib_after = f1_after + f2_after; // current contribution
			// to system costs
			deltaF = (contrib_after - contrib_current);
			if (contrib_current > contrib_after) {
				prob = 1;

			} else {
				prob = Math.exp((contrib_current - contrib_after) / this.c_k);
				if (!this.melted) {
					this.deltaFDecrSum += deltaF;
				}
			}
		}
		boolean switchAgents = false;
		if (prob > MatsimRandom.getRandom().nextDouble()) {
			switchAgents = true;
		}

		this.proposed++;
		if (switchAgents) {
			this.fCurrent += deltaF;
			this.fEpoch += this.fCurrent;
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

			this.fMutations.add(this.fCurrent);
			return true;
		} else {

			this.fMutations.add(this.fCurrent);
			return false;
		}
	}
}
