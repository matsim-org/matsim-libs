/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControler.java
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

package playground.christoph.multimodal.mobsim;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.multimodal.router.CheckActivityReachability;
import playground.christoph.multimodal.router.MultiModalPlansCalcRoute;
import playground.christoph.multimodal.router.costcalculator.TravelTimeCalculatorWithBufferFactory;

public class MultiModalControler extends Controler {

	private static final Logger log = Logger.getLogger(MultiModalControler.class);
	
	protected boolean checkActivityReachability = false;
	protected boolean dropNonCarRoutes = false;
	protected boolean createMultiModeNetwork = false;
	
	public MultiModalControler(String[] args) {
		super(args);
		
		/*
		 * Use a TravelTimeCalculator that buffers the TravelTimes form the
		 * previous Iteration.
		 */
		setTravelTimeCalculatorFactory(new TravelTimeCalculatorWithBufferFactory());
	}

	/*
	 * Always use a MultiModalMobsimFactory - it will return
	 * a (Parallel)QSim using a MultiModalQNetwork.
	 */
	@Override
	public MobsimFactory getMobsimFactory() {
		return new MultiModalMobsimFactory(this.getTravelTimeCalculator());
	}
		
	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		return new MultiModalPlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory());
	}
	
	@Override
	protected void loadData() {
		
		log.info("replacing RouteFactories for non car modes...");
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.bike, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.ride, new LinkNetworkRouteFactory());
		log.info("done.");
		
		super.loadData();

		if (createMultiModeNetwork) {
			log.info("creating multi modal network...");
			createMultiModalNetwork();
			log.info("done.");			
		}
		
		if (checkActivityReachability) {
			log.info("moving activities that cannot be reached by the transport modes of the from- and/or to-legs...");
			new CheckActivityReachability(this.scenarioData).checkAndUpdateActivityFacilities();
			log.info("done.");
		}
		
		if (dropNonCarRoutes) {
			log.info("dropping existing walk and bike routes...");
			dropNonCarRoutes();
			log.info("done.");			
		}
	}
	
	private void dropNonCarRoutes() {
		/*
		 * Drop Walk Routes - they have to be recreated.
		 */
		for (Person person : this.population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						if (leg.getMode().equals(TransportMode.walk)) {
							leg.setRoute(null);
						} else if (leg.getMode().equals(TransportMode.bike)) {
							leg.setRoute(null); 
						} else if (leg.getMode().equals(TransportMode.pt)) {
							leg.setRoute(null);
						} else if (leg.getMode().equals(TransportMode.ride)) {
							leg.setRoute(null);
						}
					}
				}
			}
		}
	}
	
	private void createMultiModalNetwork() {
		for (Link link : this.network.getLinks().values()) {
			if (Math.round(link.getFreespeed() * 3.6) <= 100) {
				Set<String> allowedModes = link.getAllowedModes();
				allowedModes.add(TransportMode.walk);
				allowedModes.add(TransportMode.bike);
				allowedModes.add(TransportMode.pt);
				link.setAllowedModes(allowedModes);
			}
		}
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new MultiModalControler(args);
			controler.setOverwriteFiles(true);		
			controler.run();
		}
		System.exit(0);
	}
}
