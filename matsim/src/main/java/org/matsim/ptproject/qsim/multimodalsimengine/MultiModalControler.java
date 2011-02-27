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

package org.matsim.ptproject.qsim.multimodalsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.TravelTimeCalculatorWithBufferFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.tools.EnsureActivityReachability;
import org.matsim.ptproject.qsim.multimodalsimengine.tools.MultiModalNetworkCreator;
import org.matsim.ptproject.qsim.multimodalsimengine.tools.NonCarRouteDropper;

public class MultiModalControler extends Controler {

	private static final Logger log = Logger.getLogger(MultiModalControler.class);

	protected boolean checkActivityReachability = false;

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
		return new MultiModalMobsimFactory(super.getMobsimFactory(), this.getTravelTimeCalculator());
	}

	@Override
	protected void loadData() {

		MultiModalConfigGroup configGroup = scenarioData.getConfig().multiModal();

		log.info("replacing RouteFactories for non car modes...");
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.bike, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new LinkNetworkRouteFactory());
		super.scenarioData.getNetwork().getFactory().setRouteFactory(TransportMode.ride, new LinkNetworkRouteFactory());
		log.info("done.");

		super.loadData();

		if (configGroup.isCreateMultiModalNetwork()) {
			log.info("creating multi modal network...");
			new MultiModalNetworkCreator(configGroup).run(scenarioData.getNetwork());
			log.info("done.");
		}

		if (checkActivityReachability) {
			log.info("moving activities that cannot be reached by the transport modes of the from- and/or to-legs...");
			new EnsureActivityReachability(this.scenarioData).run(scenarioData.getPopulation());
			log.info("done.");
		}

		if (configGroup.isDropNonCarRoutes()) {
			log.info("dropping existing walk and bike routes...");
			new NonCarRouteDropper(configGroup).run(scenarioData.getPopulation());
			log.info("done.");
		}
	}

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new MultiModalControler(args);
			controler.run();
		}
		System.exit(0);
	}
}
