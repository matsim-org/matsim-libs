/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideChooseModeForSubtourModule.java
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
package playground.thibautd.parknride.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.parknride.ParkAndRideConfigGroup;
import playground.thibautd.parknride.ParkAndRideConstants;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideUtils;
import playground.thibautd.parknride.scoring.ParkAndRideScoringFunctionFactory;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class ParkAndRideChooseModeForSubtourModule extends AbstractMultithreadedModule {
	private final Controler controler;
	private static final boolean CHECK_CAR_AVAIL = false;

	public ParkAndRideChooseModeForSubtourModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		TripRouter tripRouter = tripRouterFactory.get();
		ParkAndRideFacilities facilities = ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() );
		ParkAndRideIncluder includer;

		ParkAndRideConfigGroup configGroup = ParkAndRideUtils.getConfigGroup( controler.getConfig() );

				includer = new ParkAndRideIncluder(
						facilities,
						tripRouter);

		Random rand = MatsimRandom.getLocalInstance();
		ParkAndRideChooseModeForSubtour algo =
			new ParkAndRideChooseModeForSubtour(
					includer,
					new FacilityChanger(
						rand,
						tripRouter,
						((ParkAndRideScoringFunctionFactory) controler.getScoringFunctionFactory()).getPenaltyFactory(),
						facilities,
						configGroup.getLocalSearchRadius(),
						configGroup.getPriceOfDistance()),
					tripRouter,
					new ModesChecker( configGroup.getAvailableModes() ),
					configGroup.getAvailableModes(),
					configGroup.getChainBasedModes(),
					configGroup.getFacilityChangeProbability(),
					rand);

		return algo;
	}

	private static class ModesChecker implements PermissibleModesCalculator {
		private final Set<String> modes;

		public ModesChecker(final String[] modes) {
			this.modes = new TreeSet<String>( Arrays.asList( modes ) );
		}

		@Override
		public Collection<String> getPermissibleModes(final Plan plan) {
			List<String> available = new ArrayList<String>( modes );

			Person person = plan.getPerson();
			if (CHECK_CAR_AVAIL &&
					person instanceof PersonImpl &&
					"never".equals( PersonUtils.getCarAvail(person) )) {
				available.remove( TransportMode.car );
				available.remove( ParkAndRideConstants.PARK_N_RIDE_LINK_MODE );
			}

			return available;
		}
	}
}

