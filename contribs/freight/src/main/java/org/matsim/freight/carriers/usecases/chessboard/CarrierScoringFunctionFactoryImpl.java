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

package org.matsim.freight.carriers.usecases.chessboard;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.FreightActivity;
import org.matsim.freight.carriers.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * Defines example carrier scoring function (factory).
 *
 * <p>Just saw that there are some Deprecations. Needs to be adapted.
 *
 * @author stefan
 *
 */
public final class CarrierScoringFunctionFactoryImpl implements CarrierScoringFunctionFactory{

	/**
	 *
	 * Example activity scoring that penalizes missed time-windows with 1.0 per second.
	 *
	 * @author stefan
	 *
	 */
	public static class SimpleDriversActivityScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		@SuppressWarnings("unused")
		private static final  Logger log = LogManager.getLogger( SimpleDriversActivityScoring.class );

		private double score;
		private final double timeParameter = 0.008;
		private final double missedTimeWindowPenalty = 0.01;

		public SimpleDriversActivityScoring() {
			super();
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleFirstActivity(Activity act) {
			handleActivity(act);
		}

		@Override
		public void handleActivity(Activity act) {
			if(act instanceof FreightActivity) {
				double actStartTime = act.getStartTime().seconds();

				TimeWindow tw = ((FreightActivity) act).getTimeWindow();
				if(actStartTime > tw.getEnd()){
					double penalty_score = (-1)*(actStartTime - tw.getEnd())*missedTimeWindowPenalty;
					if (!(penalty_score <= 0.0)) throw new AssertionError("penalty score must be negative");
					score += penalty_score;

				}
				double actTimeCosts = (act.getEndTime().seconds() -actStartTime)*timeParameter;
				if (!(actTimeCosts >= 0.0)) throw new AssertionError("actTimeCosts must be positive");
				score += actTimeCosts*(-1);
			}
		}

		@Override
		public void handleLastActivity(Activity act) {
			handleActivity(act);
		}

	}

	public static class SimpleVehicleEmploymentScoring implements SumScoringFunction.BasicScoring {

		private final Carrier carrier;

		public SimpleVehicleEmploymentScoring( Carrier carrier ) {
			super();
			this.carrier = carrier;
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			double score = 0.;
			CarrierPlan selectedPlan = carrier.getSelectedPlan();
			if(selectedPlan == null) return 0.;
			for(ScheduledTour tour : selectedPlan.getScheduledTours()){
				if(!tour.getTour().getTourElements().isEmpty()){
					score += (-1)*tour.getVehicle().getType().getCostInformation().getFixedCosts();
				}
			}
			return score;
		}

	}

	/**
	 * Example leg scoring.
	 *
	 * @author stefan
	 *
	 */
	public static class SimpleDriversLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

		@SuppressWarnings("unused")
		private static final  Logger log = LogManager.getLogger( SimpleDriversLegScoring.class );

		private double score = 0.0;
		private final Network network;
		private final Carrier carrier;
		private final Set<CarrierVehicle> employedVehicles;

		public SimpleDriversLegScoring( Carrier carrier, Network network ) {
			super();
			this.network = network;
			this.carrier = carrier;
			employedVehicles = new HashSet<>();
		}

		@Override
		public void finish() { }

		@Override
		public double getScore() {
			return score;
		}

		private double getTimeParameter(CarrierVehicle vehicle) {
			return vehicle.getType().getCostInformation().getCostsPerSecond();
		}

		private double getDistanceParameter(CarrierVehicle vehicle) {
			return vehicle.getType().getCostInformation().getCostsPerMeter();
		}

		@Override
		public void handleLeg(Leg leg) {
			if(leg.getRoute() instanceof NetworkRoute nRoute){
				Id<Vehicle> vehicleId = nRoute.getVehicleId();
				CarrierVehicle vehicle = CarriersUtils.getCarrierVehicle(carrier, vehicleId);
				Gbl.assertNotNull(vehicle);
				employedVehicles.add(vehicle);
				double distance = 0.0;
				if(leg.getRoute() instanceof NetworkRoute){
					Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
					distance += startLink.getLength();
					for(Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()){
						distance += network.getLinks().get(linkId).getLength();

					}
					distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();

				}

				double distanceCosts = distance*getDistanceParameter(vehicle);
				if (!(distanceCosts >= 0.0)) throw new AssertionError("distanceCosts must be positive");
				score += (-1) * distanceCosts;
				double timeCosts = leg.getTravelTime().seconds() *getTimeParameter(vehicle);
				if (!(timeCosts >= 0.0)) throw new AssertionError("distanceCosts must be positive");
				score += (-1) * timeCosts;

			}
		}

	}


	public static class SimpleTollScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ArbitraryEventScoring {

		@SuppressWarnings("unused")
		private static final  Logger log = LogManager.getLogger( SimpleTollScoring.class );

		private double score = 0.;
		private final Carrier carrier;
		private final Network network;
		private final VehicleTypeDependentRoadPricingCalculator roadPricing;

		public SimpleTollScoring( Carrier carrier, Network network, VehicleTypeDependentRoadPricingCalculator roadPricing ) {
			this.carrier = carrier;
			this.roadPricing = roadPricing;
			this.network = network;
		}

		@Override
		public void handleEvent(Event event) {
			if(event instanceof LinkEnterEvent){
				CarrierVehicle carrierVehicle = CarriersUtils.getCarrierVehicle(carrier, ((LinkEnterEvent) event).getVehicleId());
				if(carrierVehicle == null) throw new IllegalStateException("carrier vehicle missing");
				double toll = roadPricing.getTollAmount(carrierVehicle.getType().getId(),network.getLinks().get(((LinkEnterEvent) event).getLinkId() ),event.getTime() );
				if(toll > 0.) System.out.println("bing: vehicle " + carrierVehicle.getId() + " paid toll " + toll );
				score += (-1) * toll;
			}
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			return score;
		}
	}

	private final Network network;

	@Inject CarrierScoringFunctionFactoryImpl(Network network) {
		super();
		this.network = network;
	}


	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		SimpleDriversLegScoring driverLegScoring = new SimpleDriversLegScoring(carrier, network);
		SimpleVehicleEmploymentScoring vehicleEmployment = new SimpleVehicleEmploymentScoring(carrier);
//		DriversActivityScoring actScoring = new DriversActivityScoring();
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(vehicleEmployment);
//		sf.addScoringFunction(actScoring);
		return sf;
	}



}
