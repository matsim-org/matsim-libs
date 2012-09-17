/* *********************************************************************** *
 * project: org.matsim.*
 * MultiLegRoutingControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.router.TripRouterFactoryImpl;

/**
 * @author thibautd
 */
public class MultiLegRoutingControler extends Controler {
	private TripRouterFactory tripRouterFactory;

	public MultiLegRoutingControler(final Config config) {
		super(config);
	}

	public MultiLegRoutingControler(final Scenario scenario) {
		super(scenario);
	}

	@Override
	protected void loadControlerListeners() {
		addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				setTripRouterFactory( new TripRouterFactoryImpl( event.getControler() ) );
			}
		});

		super.loadControlerListeners();
	}

	//TODO: pass arguments to factory
	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			final TravelDisutility travelCosts,
			final TravelTime travelTimes) {
		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		return new PlanRouter(
				tripRouterFactory.createTripRouter(),
				getScenario().getActivityFacilities());
	}


	/**
	 * Returns the (only) trip router factory instance for this controler.
	 * The instance is created at the first call of this method.
	 * <br>
	 * The fact of having one only instance simplifies the custom configuration
	 * (you just have to tune this instance to your needs) but has the drawback
	 * that a change in the controler (for example, a change of the travelDisutilityFactory)
	 * will <b>not</b> be reflected in the router.
	 *
	 * @return the {@link TripRouterFactory} instance
	 */
	public TripRouterFactory getTripRouterFactory() {
		return tripRouterFactory;
	}

	public void setTripRouterFactory(final TripRouterFactory tripRouterFactory) {
		this.tripRouterFactory = tripRouterFactory;
	}
}

