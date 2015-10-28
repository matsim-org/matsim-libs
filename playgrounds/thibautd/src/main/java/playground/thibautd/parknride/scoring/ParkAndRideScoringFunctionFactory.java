/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideScoringFunctionFactory.java
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
package playground.thibautd.parknride.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.facilities.ActivityFacilities;

/**
 * @author thibautd
 */
public class ParkAndRideScoringFunctionFactory implements ScoringFunctionFactory {
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final ParkingPenaltyFactory parkingPenaltyFactory;
	private final ActivityFacilities facilities;
	private final Network network;

	public ParkAndRideScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory,
			final ParkingPenaltyFactory parkingPenaltyFactory,
			final ActivityFacilities facilities,
			final Network network) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.parkingPenaltyFactory = parkingPenaltyFactory;
		this.facilities = facilities;
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		return new ParkAndRideScoringFunction(
				scoringFunctionFactory.createNewScoringFunction( person ),
				parkingPenaltyFactory.createPenalty( person.getSelectedPlan() ),
				facilities,
				network,
				person.getSelectedPlan());
	}

	/**
	 * Creates a listener which will replace the ScoringFunctionFactory in the
	 * controler just before the iterations start, taking the registered ScoringFunctionFactory
	 * as a delegate.
	 * @param penalties the factory to use to create penalties
	 * @return a controler listener, to add to the controler before running the iterations.
	 */
	public static ControlerListener createFactoryListener(
			final ParkingPenaltyFactory penalties) {
		return new Listener( penalties );
	}

	public ParkingPenaltyFactory getPenaltyFactory() {
		return parkingPenaltyFactory;
	}

	private static class Listener implements StartupListener {
		private final ParkingPenaltyFactory parkingPenaltyFactory;

		public Listener(final ParkingPenaltyFactory factory) {
			parkingPenaltyFactory = factory;
		}

		@Override
		public void notifyStartup(final StartupEvent event) {
			Controler controler = event.getControler();
            controler.setScoringFunctionFactory(
					new ParkAndRideScoringFunctionFactory(
						controler.getScoringFunctionFactory(),
						parkingPenaltyFactory,
						((MutableScenario) controler.getScenario()).getActivityFacilities(),
                            controler.getScenario().getNetwork()) );
		}
	}
}

