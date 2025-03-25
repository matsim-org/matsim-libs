/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.mobsim;

import org.junit.jupiter.api.Disabled;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierConstants;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.vehicles.Vehicle;

@Disabled
public class ScoringFunctionFactoryForTests implements CarrierScoringFunctionFactory{

	 static class DriverLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

			private double score = 0.0;
			private final Network network;
			private final Carrier carrier;

		 public DriverLegScoring(Carrier carrier, Network network) {
				super();
				this.network = network;
				this.carrier = carrier;
			}


			@Override
			public void finish() {}

			@Override
			public double getScore() {
				return score;
			}

		 @Override
		 public void handleLeg(Leg leg) {
			 if (leg.getRoute() instanceof NetworkRoute nRoute) {
				 Id<Vehicle> vehicleId = nRoute.getVehicleId();
				 CarrierVehicle vehicle = CarriersUtils.getCarrierVehicle(carrier, vehicleId);
				 Gbl.assertNotNull(vehicle);
				 double distance = 0.0;
				 if (leg.getRoute() instanceof NetworkRoute) {
					 distance += network.getLinks().get(leg.getRoute().getStartLinkId()).getLength();
					 for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
						 distance += network.getLinks().get(linkId).getLength();
					 }
					 distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
				 }
				 score += (-1) * distance * getDistanceParameter(vehicle);
			 }
		 }

			private double getDistanceParameter(CarrierVehicle vehicle) {
				return vehicle.getType().getCostInformation().getCostsPerMeter();
			}

		}

	 static class DriverActScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		 boolean firstEnd = true;

		 double startTime;

		 double startTimeOfEnd;

		 final double amountPerHour = 20.0;

		 @Override
		 public void handleFirstActivity(Activity act) {
			if(firstEnd){
				startTime = act.getEndTime().seconds();
				firstEnd = false;
			}
		 }

		 @Override
		 public void handleActivity(Activity act) {
			if(act.getType().equals(CarrierConstants.END)){
				startTimeOfEnd = act.getStartTime().seconds();
			}
		 }

		 @Override
		 public void handleLastActivity(Activity act) {
			if(act.getType().equals(CarrierConstants.END)){
				startTimeOfEnd = act.getStartTime().seconds();
			}
		 }

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return Math.round((-1)*(startTimeOfEnd-startTime)/3600.0*amountPerHour);
		}

	 }

	static class NumberOfToursAward implements SumScoringFunction.BasicScoring {

		private final Carrier carrier;

		public NumberOfToursAward(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			if(carrier.getSelectedPlan().getScheduledTours().size() > 1){
				return 10000.0;
			}
			return 0;
		}

	}

	 private final Network network;

	public ScoringFunctionFactoryForTests(Network network) {
		super();
		this.network = network;
	}

	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		DriverLegScoring driverLegScoring = new DriverLegScoring(carrier, network);
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(new NumberOfToursAward(carrier));
		sf.addScoringFunction(new DriverActScoring());
		return sf;
	}

}
