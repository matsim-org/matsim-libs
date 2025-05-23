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

import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierConstants;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.vehicles.Vehicle;

@Disabled
public class TimeScoringFunctionFactoryForTests implements CarrierScoringFunctionFactory{

	static class DriverLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

		private double score = 0.0;
		private final Network network;
		private final Carrier carrier;
		private final Set<CarrierVehicle> employedVehicles;

		public DriverLegScoring(Carrier carrier, Network network) {
			super();
			this.network = network;
			this.carrier = carrier;
			employedVehicles = new HashSet<>();
		}


		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleLeg(Leg leg) {
			if(leg.getRoute() instanceof NetworkRoute nRoute){
				Id<Vehicle> vehicleId = nRoute.getVehicleId();
				CarrierVehicle vehicle = CarriersUtils.getCarrierVehicle(carrier, vehicleId);
				assert vehicle != null : "cannot find vehicle with id=" + vehicleId;
				if(!employedVehicles.contains(vehicle)){
					employedVehicles.add(vehicle);
					score += (-1)*getFixEmploymentCost(vehicle);
				}
				double distance = 0.0;
				double toll = 0.0;
				if(leg.getRoute() instanceof NetworkRoute){
					distance += network.getLinks().get(leg.getRoute().getStartLinkId()).getLength();
					for(Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()){
						distance += network.getLinks().get(linkId).getLength();
						toll += getToll(linkId, vehicle, null);
					}
					distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
					toll += getToll(leg.getRoute().getEndLinkId(), vehicle, null);
				}
				score += (-1)*(leg.getTravelTime().seconds())*getTimeParameter(vehicle,null);
				score += (-1)*distance*getDistanceParameter(vehicle,null);
				score += (-1)*toll;
			}

		}

		@SuppressWarnings("SameReturnValue") // Keep the method as code example.
		private double getFixEmploymentCost(CarrierVehicle vehicle) {
			return 0.0;
		}

		@SuppressWarnings({"SameReturnValue", "SameParameterValue"}) // Keep the method as code example.
		private double getToll(Id<Link> linkId, CarrierVehicle vehicle, Person driver) {
			return 0;
		}

		@SuppressWarnings({"SameReturnValue", "SameParameterValue"}) // Keep the method as code example.
		private double getDistanceParameter(CarrierVehicle vehicle, Person driver) {
			return 0.0;
		}

		@SuppressWarnings({"SameReturnValue", "SameParameterValue"}) // Keep the method as code example.
		private double getTimeParameter(CarrierVehicle vehicle, Person driver) {
			return 1.0;
		}

	}

	static class DriverActScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		boolean firstEnd = true;
		double startTime;
		double startTimeOfEnd;
		double timeAtActivities = 0.0;
		double amountPerHour = 20.0;
		double startCurrentAct;
		double time_at_activities = 0.0;

		@Override
		public void handleFirstActivity(Activity act) {
			endActivity(act.getEndTime().seconds(), act);
		}

		@Override
		public void handleActivity(Activity act) {
			startActivity(act.getStartTime().seconds(), act);
			endActivity(act.getEndTime().seconds(), act);
		}

		@Override
		public void handleLastActivity(Activity act) {
			startActivity(act.getStartTime().seconds(), act);
		}


		private void startActivity(double time, Activity act) {
			if(!act.getType().equals(CarrierConstants.END)){
				System.out.println("act_start="+ Time.writeTime(time)+" act="+act);
				startCurrentAct = time;
			}
		}

		private void endActivity(double time, Activity act) {
			if(!act.getType().equals("start")){
				System.out.println("act_end="+Time.writeTime(time)+" act="+act);
				time_at_activities += time - startCurrentAct;
				System.out.println("time_at_activities="+time_at_activities);
			}
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return Math.round((-1)*(time_at_activities));
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

	@Inject private Network network;

	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		DriverLegScoring driverLegScoring = new DriverLegScoring(carrier, network);
		sf.addScoringFunction(new DriverActScoring());
		return sf;
	}

}
