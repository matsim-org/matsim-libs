/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.ev.routing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.util.StraightLineKnnFinder;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

/**
 * This network Routing module adds stages for re-charging into the Route.
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 *
 * @author jfbischoff
 */

public final class EvNetworkRoutingModule implements RoutingModule {

	private final String mode;

	private final Network network;
	private final RoutingModule delegate;
	private final ElectricFleetSpecification electricFleet;
	private final ChargingInfrastructureSpecification chargingInfrastructureSpecification;
	private final Random random = MatsimRandom.getLocalInstance();
	private final TravelTime travelTime;
	private final DriveEnergyConsumption.Factory driveConsumptionFactory;
	private final AuxEnergyConsumption.Factory auxConsumptionFactory;
	private final String stageActivityType;
	private final String vehicleSuffix;

	private final class EvCharingStageActivityType implements StageActivityTypes {
		@Override
		public boolean isStageActivity(String activityType) {
			if (EvNetworkRoutingModule.this.stageActivityType.equals(activityType)) {
				return true;
			} else if (activityType.endsWith("interaction")) {
				return true;
			}
			{
				return false;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof EvCharingStageActivityType)) {
				return false;
			}
			EvCharingStageActivityType other = (EvCharingStageActivityType)obj;
			return other.isStageActivity(EvNetworkRoutingModule.this.stageActivityType);
		}

		@Override
		public int hashCode() {
			return EvNetworkRoutingModule.this.stageActivityType.hashCode();
		}
	}

	public EvNetworkRoutingModule(final String mode, final Network network, RoutingModule delegate,
			ElectricFleetSpecification electricFleet,
			ChargingInfrastructureSpecification chargingInfrastructureSpecification, TravelTime travelTime,
			DriveEnergyConsumption.Factory driveConsumptionFactory,
			AuxEnergyConsumption.Factory auxConsumptionFactory) {
		this.travelTime = travelTime;
		Gbl.assertNotNull(network);
		this.delegate = delegate;
		this.network = network;
		this.mode = mode;
		this.electricFleet = electricFleet;
		this.chargingInfrastructureSpecification = chargingInfrastructureSpecification;
		this.driveConsumptionFactory = driveConsumptionFactory;
		this.auxConsumptionFactory = auxConsumptionFactory;
		stageActivityType = mode + VehicleChargingHandler.CHARGING_IDENTIFIER;
		this.vehicleSuffix = mode.equals(TransportMode.car) ? "" : "_" + mode;
	}

	@Override
	public List<? extends PlanElement> calcRoute(final Facility fromFacility, final Facility toFacility,
			final double departureTime, final Person person) {

		List<? extends PlanElement> basicRoute = delegate.calcRoute(fromFacility, toFacility, departureTime, person);
		Id<ElectricVehicle> evId = Id.create(person.getId() + vehicleSuffix, ElectricVehicle.class);
		if (!electricFleet.getVehicleSpecifications().containsKey(evId)) {
			return basicRoute;
		} else {
			Leg basicLeg = (Leg)basicRoute.get(0);
			ElectricVehicleSpecification ev = electricFleet.getVehicleSpecifications().get(evId);

			Map<Link, Double> estimatedEnergyConsumption = estimateConsumption(ev, basicLeg);
			double estimatedOverallConsumption = estimatedEnergyConsumption.values()
					.stream()
					.mapToDouble(Number::doubleValue)
					.sum();
			double capacity = ev.getBatteryCapacity() * (0.8 + random.nextDouble() * 0.18);
			double numberOfStops = Math.floor(estimatedOverallConsumption / capacity);
			if (numberOfStops < 1) {
				return basicRoute;
			} else {
				List<Link> stopLocations = new ArrayList<>();
				double currentConsumption = 0;
				for (Map.Entry<Link, Double> e : estimatedEnergyConsumption.entrySet()) {
					currentConsumption += e.getValue();
					if (currentConsumption > capacity) {
						stopLocations.add(e.getKey());
						currentConsumption = 0;
					}
				}
				List<PlanElement> stagedRoute = new ArrayList<>();
				Facility lastFrom = fromFacility;
				double lastArrivaltime = departureTime;
				for (Link stopLocation : stopLocations) {

					StraightLineKnnFinder<Link, ChargerSpecification> straightLineKnnFinder = new StraightLineKnnFinder<>(
							2, l -> l, s -> network.getLinks().get(s.getLinkId()));
					List<ChargerSpecification> nearestChargers = straightLineKnnFinder.findNearest(stopLocation,
							chargingInfrastructureSpecification.getChargerSpecifications()
									.values()
									.stream()
									.filter(charger -> ev.getChargerTypes().contains(charger.getChargerType())));
					ChargerSpecification selectedCharger = nearestChargers.get(random.nextInt(1));
					Link selectedChargerLink = network.getLinks().get(selectedCharger.getLinkId());
					Facility nexttoFacility = new LinkWrapperFacility(selectedChargerLink);
					if (nexttoFacility.getLinkId().equals(lastFrom.getLinkId())) {
						continue;
					}
					List<? extends PlanElement> routeSegment = delegate.calcRoute(lastFrom, nexttoFacility,
							lastArrivaltime, person);
					Leg lastLeg = (Leg)routeSegment.get(0);
					lastArrivaltime = lastLeg.getDepartureTime() + lastLeg.getTravelTime();
					stagedRoute.add(lastLeg);
					Activity chargeAct = PopulationUtils.createActivityFromCoordAndLinkId(stageActivityType,
							selectedChargerLink.getCoord(), selectedChargerLink.getId());
					double estimatedChargingTime = (ev.getBatteryCapacity() * 1.25) / selectedCharger.getMaxPower();
					chargeAct.setMaximumDuration(estimatedChargingTime);
					lastArrivaltime += chargeAct.getMaximumDuration();
					stagedRoute.add(chargeAct);
					lastFrom = nexttoFacility;
				}
				stagedRoute.addAll(delegate.calcRoute(lastFrom, toFacility, lastArrivaltime, person));

				return stagedRoute;

			}

		}
	}

	private Map<Link, Double> estimateConsumption(ElectricVehicleSpecification ev, Leg basicLeg) {
		Map<Link, Double> consumptions = new LinkedHashMap<>();
		NetworkRoute route = (NetworkRoute)basicLeg.getRoute();
		List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());
		ElectricVehicle pseudoVehicle = ElectricVehicleImpl.create(ev, driveConsumptionFactory, auxConsumptionFactory,
				v -> charger -> {
					throw new UnsupportedOperationException();
				});
		DriveEnergyConsumption driveEnergyConsumption = pseudoVehicle.getDriveEnergyConsumption();
		AuxEnergyConsumption auxEnergyConsumption = pseudoVehicle.getAuxEnergyConsumption();
		double lastSoc = pseudoVehicle.getBattery().getSoc();
		for (Link l : links) {
			double travelT = travelTime.getLinkTravelTime(l, basicLeg.getDepartureTime(), null, null);

			double consumption = driveEnergyConsumption.calcEnergyConsumption(l, travelT, Time.getUndefinedTime())
					+ auxEnergyConsumption.calcEnergyConsumption(basicLeg.getDepartureTime(), travelT, l.getId());
			if (consumption > 0) {
				pseudoVehicle.getBattery().discharge(consumption);
			} else {
				pseudoVehicle.getBattery().charge(-consumption);
			}
			double currentSoc = pseudoVehicle.getBattery().getSoc();
			// to accomodate for ERS, where energy charge is directly implemented in the consumption model
			double consumptionDiff = (lastSoc - currentSoc);
			lastSoc = currentSoc;
			consumptions.put(l, consumptionDiff);
		}
		return consumptions;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new EvCharingStageActivityType();
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode=" + this.mode + "]";
	}

}
