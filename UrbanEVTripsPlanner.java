/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.urbanEV;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import one.util.streamex.StreamEx;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.contrib.util.StraightLineKnnFinder;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.utils.EditPlans;

import javax.inject.Provider;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.matsim.urbanEV.MATSimVehicleWrappingEVSpecificationProvider.getWrappedElectricVehicleId;

class UrbanEVTripsPlanner implements MobsimInitializedListener {

	@Inject
	private Provider<TripRouter> tripRouterProvider;

	@Inject
	Scenario scenario;

	@Inject
	Vehicles vehicles;

	@Inject
	private SingleModeNetworksCache singleModeNetworksCache;

	@Inject
	private ElectricFleetSpecification electricFleetSpecification;

	@Inject
	private ChargingInfrastructureSpecification chargingInfrastructureSpecification;

	@Inject
	private DriveEnergyConsumption.Factory driveConsumptionFactory;

	@Inject
	private AuxEnergyConsumption.Factory auxConsumptionFactory;

	@Inject
	private ChargingPower.Factory chargingPowerFactory;

	@Inject
	private ChargingLogic.Factory chargingLogicFactory;

	@Inject
	private Map<String, TravelTime> travelTimes;

	@Inject
	ActivityWhileChargingFinder activityWhileChargingFinder;

	@Inject
	Config config;

	private QSim qsim;

	private static final Logger log = Logger.getLogger(UrbanEVTripsPlanner.class);


	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
 		if(! (e.getQueueSimulation() instanceof QSim)){
			throw new IllegalStateException(UrbanEVTripsPlanner.class.toString() + " only works with a mobsim of type " + QSim.class);
		}
		Set<Plan> selectedEVPlans = collectEVUserPlans();
		this.qsim = (QSim) e.getQueueSimulation();
		processPlans(selectedEVPlans);
	}

	/**
	 * collect agents that use an EV at least once in their selected plan
	 * @return a map of personId to selected plan
	 */
	//deliberately chose to iterate over 'normal' plans instead of modifiable plans, in order to juggle as least with modifiable plans as possible (and not to break anything..)
	//tschlenther sep' 2020
	private Set<Plan> collectEVUserPlans() {
		return scenario.getPopulation().getPersons().values().parallelStream()
				.map(person -> person.getSelectedPlan())
				.filter(plan -> ! getEVLegs(plan).isEmpty())
				.collect(toSet());
	}

	/**
	 * retrieve all legs from a plan for which the registered vehicle is an EV. sort them in order of their departure time.
	 * @param plan
	 * @return
	 */
	private List<Leg> getEVLegs(Plan plan){
		List<Leg> list = TripStructureUtils.getLegs(plan).stream()
				.filter(leg -> isEV(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())))
				.collect(toList());
		list.sort(Comparator.comparingDouble(leg -> leg.getDepartureTime().seconds()));
		return list;
	}

	private boolean isEV(Id<Vehicle> vehicleId) {
		return this.electricFleetSpecification.getVehicleSpecifications().containsKey(getWrappedElectricVehicleId(vehicleId));
	}

	private void processPlans(Set<Plan> selectedEVPlans) {
		for (Plan plan : selectedEVPlans) {
			List<Leg> evLegs = getEVLegs(plan);
			Preconditions.checkState(!evLegs.isEmpty(), "no ev legs found");
			//map vehicles to the list of legs they are used for
			Map<Vehicle, List<Leg>> vehicleToLegs = StreamEx.of(evLegs)
					.mapToEntry(leg -> vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())), leg -> leg)
					.grouping(toList());

			if(vehicleToLegs.size() > 1) throw new RuntimeException("person " + plan.getPerson().getId() + " uses more than one EV. Currently we can not handle that.."); //TODO actually should work - test
			replan(plan, vehicleToLegs.entrySet().stream().findFirst().orElseThrow());
		}
	}

	private void replan(Plan plan, Map.Entry<Vehicle, List<Leg>> vehicle2Legs) {
		//create pseudo EVSpecification
		//create pseudo EV
		Vehicle vehicle = vehicle2Legs.getKey();
		ElectricVehicleSpecification electricVehicleSpecification = electricFleetSpecification.getVehicleSpecifications()
				.get(getWrappedElectricVehicleId(vehicle.getId()));

		ElectricVehicle pseudoVehicle = ElectricVehicleImpl.create(electricVehicleSpecification, driveConsumptionFactory, auxConsumptionFactory, chargingPowerFactory);

		double capacityThreshold = electricVehicleSpecification.getBatteryCapacity() * (0.2); //TODO randomize?

		//estimate consumptionPerLegPerLink
		//i do not use lambda syntax and streams here as we need to be sure that the order of legs is right
		double secondLastSOC = pseudoVehicle.getBattery().getSoc();
		double lastSOC = pseudoVehicle.getBattery().getSoc();

		for (Leg leg : vehicle2Legs.getValue()) {
			emulateVehicleDischarging(pseudoVehicle,leg);
			if (pseudoVehicle.getBattery().getSoc() <= capacityThreshold){
				//plan charging trip
				secondLastSOC = replanPrecedentAndCurrentEVLegsAndGetPrecedentSOC(plan, electricVehicleSpecification, pseudoVehicle, secondLastSOC, leg);

				lastSOC = pseudoVehicle.getBattery().getSoc();
				if(lastSOC <= capacityThreshold) throw new RuntimeException("tried to find a suitable charging station for agent " + plan.getPerson() + " but apparently did not work");
				continue;
			}
			secondLastSOC = lastSOC;
			lastSOC = pseudoVehicle.getBattery().getSoc();
		}
	}

	/**
	 *
	 * @param plan
	 * @param electricVehicleSpecification
	 * @param pseudoVehicle
	 * @param secondLastSOC
	 * @param leg
	 * @return
	 */
	private double replanPrecedentAndCurrentEVLegsAndGetPrecedentSOC(Plan plan, ElectricVehicleSpecification electricVehicleSpecification, ElectricVehicle pseudoVehicle, double secondLastSOC, Leg leg) {
		Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(leg.getMode());

		String routingMode = TripStructureUtils.getRoutingMode(leg);
		int legIndex = plan.getPlanElements().indexOf(leg);
		Preconditions.checkState(legIndex > -1, "could not locate leg in plan");

//		Activity actWhileCharging = EditPlans.findRealActBefore(mobsimagent, legIndex);

		//from here we deal with the modifiable plan (only!?)
		MobsimAgent mobsimagent = qsim.getAgents().get(plan.getPerson().getId());
		Plan modifiablePlan = WithinDayAgentUtils.getModifiablePlan(mobsimagent);

		//find suitable non-stage activity before SOC threshold passover
		Activity actWhileCharging = activityWhileChargingFinder.findActivityWhileChargingBeforeLeg(modifiablePlan, (Leg) modifiablePlan.getPlanElements().get(legIndex));

		Preconditions.checkNotNull(actWhileCharging, "could not insert plugin activity in plan of agent " + plan.getPerson().getId() +
		".\n One reason could be that the agent has no suitable activity prior to the leg for which the " +
				" energy threshold is expected to be exceeded. \n" +
				" Another reason  might be that it's vehicle is running beyond energy threshold during the first leg of the day." +
		"That could possibly be avoided by using EVNetworkRoutingModule..."); //TODO let the sim just run and let the ev run empty!?

		//TODO what if actWhileCharging does not hold a link id?
		ChargerSpecification selectedCharger = selectChargerNearToLink(actWhileCharging.getLinkId(), electricVehicleSpecification, modeNetwork);
		Link chargingLink = modeNetwork.getLinks().get(selectedCharger.getLinkId());


		Activity pluginTripOrigin = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(actWhileCharging));
		Leg plugoutLeg = activityWhileChargingFinder.getNextLegOfRoutingModeAfterActivity(ImmutableList.copyOf(modifiablePlan.getPlanElements()), actWhileCharging, routingMode);
		Activity plugoutTripOrigin = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
		Activity plugoutTripDestination = EditPlans.findRealActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));

		{	//some consistency checks.. //TODO consider to put in a JUnit test..
			Preconditions.checkNotNull(pluginTripOrigin, "pluginTripOrigin is null. should never happen..");
			Preconditions.checkState(!pluginTripOrigin.equals(actWhileCharging), "pluginTripOrigin is equal to actWhileCharging. should never happen..");

			PlanElement legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) + 1);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg after pluginTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(actWhileCharging) - 1);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg before actWhileCharging has the wrong routing mode. should not happen..");

			Preconditions.checkState(!plugoutTripDestination.equals(actWhileCharging), "plugoutTripDestination is equal to actWhileCharging. should never happen..");

			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) < modifiablePlan.getPlanElements().indexOf(actWhileCharging));
			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(actWhileCharging) < modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin));
			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) < modifiablePlan.getPlanElements().indexOf(plugoutTripDestination));

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) + 1);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg after plugoutTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripDestination) - 1);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg before plugoutTripDestination has the wrong routing mode. should not happen..");
		}

		//TODO fix bug! this is wrong in cases where we decided not to charge on the precedent link but earlier...
		//set SOC back to the second last value as we reroute the last leg and the current leg
		pseudoVehicle.getBattery().setSoc(secondLastSOC);

		//facilities
		Facility fromFacility = FacilitiesUtils.toFacility(pluginTripOrigin, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(actWhileCharging, scenario.getActivityFacilities());

		TripRouter tripRouter = tripRouterProvider.get();

		Leg legToCharger = planPluginTripAndGetMainLeg(modifiablePlan, routingMode, pluginTripOrigin, actWhileCharging, chargingLink, tripRouter, fromFacility, chargerFacility, toFacility);
		double chargingBegin =  legToCharger.getDepartureTime().seconds() + legToCharger.getTravelTime().seconds();
		emulateVehicleDischarging(pseudoVehicle, legToCharger);
		secondLastSOC = pseudoVehicle.getBattery().getSoc();

		Leg legFromCharger = planPlugoutTripAndGetMainLeg(modifiablePlan, routingMode, actWhileCharging, plugoutTripDestination, chargingLink, tripRouter, chargerFacility, toFacility, PlanRouter.calcEndOfActivity(actWhileCharging, plan, config));
		double chargingDuration = (legFromCharger == null) ? 0 : legFromCharger.getDepartureTime().seconds() - chargingBegin;

		//charge pseudo vehicle

		//TODO: if the provider for ChargingInfrastructure.class would be bound, we could inject it and use it here and did not have to copy chargers...
		//same is actually valid for the electric fleet / electric vehicle. but there we DO want a copy...
		Charger chargerCopy = ChargerImpl.create(selectedCharger, chargingLink, chargingLogicFactory);
		pseudoVehicle.getBattery().changeSoc(pseudoVehicle.getChargingPower().calcChargingPower(chargerCopy) * chargingDuration);
		emulateVehicleDischarging(pseudoVehicle, legFromCharger);
		return secondLastSOC;
	}

	private Leg planPlugoutTripAndGetMainLeg(Plan plan, String routingMode, Activity origin, Activity destination, Link chargingLink, TripRouter tripRouter, Facility chargerFacility, Facility toFacility, double now) {
		List<? extends PlanElement> routedSegment;
		if(destination != null){//actually destination can not be null based on how we determine the actWhileCharging = origin at the moment...

			List<PlanElement> trip  = new ArrayList<>();
			Facility fromFacility = toFacility;
			toFacility = FacilitiesUtils.toFacility(destination, scenario.getActivityFacilities());

			//add leg to charger
			routedSegment = tripRouter.calcRoute(TransportMode.walk,fromFacility, chargerFacility,
					now, plan.getPerson());
			Leg accessLeg = (Leg)routedSegment.get(0);
			now = TripRouter.calcEndOfPlanElement(now, accessLeg, config);
			TripStructureUtils.setRoutingMode(accessLeg, routingMode);
			trip.add(accessLeg);

			//add plugout act
			Activity plugOutAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(),
					chargingLink.getId(), routingMode + UrbanVehicleChargingHandler.PLUGOUT_IDENTIFIER);
			trip.add(plugOutAct);
			now = TripRouter.calcEndOfPlanElement(now, plugOutAct, config);

			//add leg to destination
			routedSegment = tripRouter.calcRoute(routingMode, chargerFacility, toFacility, now, plan.getPerson());
			Leg mainLeg = (Leg) routedSegment.get(0);
			trip.add(mainLeg);
			now = TripRouter.calcEndOfPlanElement(now, mainLeg, config);

			//insert trip
			TripRouter.insertTrip(plan, origin, trip, destination ) ;

			//reset activity end time
			destination.setEndTime(PopulationUtils.decideOnActivityEndTime(destination, now, config ).seconds());
			return mainLeg;
		}
		return null;
	}

	private Leg planPluginTripAndGetMainLeg(Plan plan, String routingMode, Activity actBeforeCharging, Activity actWhileCharging, Link chargingLink, TripRouter tripRouter, Facility fromFacility, Facility chargerFacility, Facility toFacility) {
		List<PlanElement> trip = new ArrayList<>();
		//add leg to charger
		List<? extends PlanElement> routedSegment = tripRouter.calcRoute(routingMode,fromFacility, chargerFacility,
				PlanRouter.calcEndOfActivity(actBeforeCharging, plan, config), plan.getPerson());
		Leg mainLeg = (Leg)routedSegment.get(0);
		double now = mainLeg.getDepartureTime().seconds() + mainLeg.getRoute().getTravelTime().seconds();
		trip.add(mainLeg);

		//add plugin act
		Activity pluginAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(),
				chargingLink.getId(), routingMode + UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER);
		trip.add(pluginAct);
		now = TripRouter.calcEndOfPlanElement(now, pluginAct, config);

		//add walk leg to destination
		routedSegment = tripRouter.calcRoute(TransportMode.walk,chargerFacility, toFacility, now, plan.getPerson());
		Leg egress = (Leg) routedSegment.get(0);
		TripStructureUtils.setRoutingMode(egress, routingMode);
		trip.add(egress);
		now = TripRouter.calcEndOfPlanElement(now, egress, config);

		//insert trip
		TripRouter.insertTrip(plan, actBeforeCharging, trip, actWhileCharging ) ;

		//reset activity end time
		actWhileCharging.setEndTime(PopulationUtils.decideOnActivityEndTime(actWhileCharging, now, config ).seconds());

		return mainLeg;
	}


	//TODO possibly put behind interface
	private ChargerSpecification selectChargerNearToLink(Id<Link> linkId, ElectricVehicleSpecification vehicleSpecification, Network network){
		StraightLineKnnFinder<Link, ChargerSpecification> straightLineKnnFinder = new StraightLineKnnFinder<>(
				1, l -> l.getFromNode().getCoord(), s -> network.getLinks().get(s.getLinkId()).getToNode().getCoord()); //TODO get closest X chargers and choose randomly?
		List<ChargerSpecification> nearestChargers = straightLineKnnFinder.findNearest(network.getLinks().get(linkId),
				chargingInfrastructureSpecification.getChargerSpecifications()
						.values()
						.stream()
						.filter(charger -> vehicleSpecification.getChargerTypes().contains(charger.getChargerType())));
		if (nearestChargers.isEmpty()){
			throw new RuntimeException("no charger could be found for vehicle type " + vehicleSpecification.getVehicleType());
		}
		return nearestChargers.get(0);
	}

	/**
	 * this method has the side effect that the soc of the ev is altered by estimated energy consumption of the leg
	 * @param ev
	 * @param leg
	 */
	private void emulateVehicleDischarging(ElectricVehicle ev, Leg leg) {
		//retrieve mode specific network
		Network network = this.singleModeNetworksCache.getSingleModeNetworksCache().get(leg.getMode());
		//retrieve routin mode specific travel time
		String routingMode = TripStructureUtils.getRoutingMode(leg);
		TravelTime travelTime = this.travelTimes.get(routingMode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode " + routingMode + ".");
		}

//		Map<Link, Double> consumptions = new LinkedHashMap<>();
		NetworkRoute route = (NetworkRoute)leg.getRoute();
		List<Link> links = NetworkUtils.getLinks(network, route.getLinkIds());

		DriveEnergyConsumption driveEnergyConsumption = ev.getDriveEnergyConsumption();
		AuxEnergyConsumption auxEnergyConsumption = ev.getAuxEnergyConsumption();
		double linkEnterTime = leg.getDepartureTime().seconds();
		for (Link l : links) {
			double travelT = travelTime.getLinkTravelTime(l, leg.getDepartureTime().seconds(), null, null);

			double driveConsumption = driveEnergyConsumption.calcEnergyConsumption(l, travelT, linkEnterTime);
			double auxConsumption = auxEnergyConsumption.calcEnergyConsumption(leg.getDepartureTime().seconds(), travelT, l.getId());
//			double consumption = driveEnergyConsumption.calcEnergyConsumption(l, travelT, linkEnterTime)
//					+ auxEnergyConsumption.calcEnergyConsumption(leg.getDepartureTime().seconds(), travelT, l.getId());
			double consumption = driveConsumption + auxConsumption;
			ev.getBattery().changeSoc(-consumption);
			linkEnterTime += travelT;
		}
	}

}


