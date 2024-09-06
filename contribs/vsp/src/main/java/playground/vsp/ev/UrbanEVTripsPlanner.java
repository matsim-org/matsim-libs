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

package playground.vsp.ev;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.sun.istack.Nullable;
import jakarta.inject.Provider;
import one.util.streamex.StreamEx;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.util.StraightLineKnnFinder;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.withinday.utils.EditPlans;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

class UrbanEVTripsPlanner implements MobsimInitializedListener {

	@Inject
	private Provider<TripRouter> tripRouterProvider;

	@Inject
	Scenario scenario;

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
	OutputDirectoryHierarchy controlerIO;
	@Inject
	IterationCounter iterationCounter;

	@Inject
	private TimeInterpretation timeInterpretation;

	@Inject
	Config config;

	private QSim qsim;

	private static final Logger log = LogManager.getLogger(UrbanEVTripsPlanner.class);
	private static List<AgentsOutOfEnergyData> agentsOutOfEnergyDataSets = new ArrayList<>();

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		if (!(e.getQueueSimulation() instanceof QSim)) {
			throw new IllegalStateException(UrbanEVTripsPlanner.class + " only works with a mobsim of type " + QSim.class);
		}
		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup)config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);
		//collect all selected plans that contain ev legs and map them to the set of ev used
		Map<Plan, Set<Id<Vehicle>>> selectedEVPlans = StreamEx.of(scenario.getPopulation().getPersons().values())
				.mapToEntry(p -> p.getSelectedPlan(), p -> getUsedEVToPreplan(p.getSelectedPlan()))
				.filterValues(evSet -> !evSet.isEmpty())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		this.qsim = (QSim)e.getQueueSimulation();
		processPlans(selectedEVPlans);
		CSVPrinter csvPrinter;
		logAgentsOutOfEnergy();
	}

	/**
	 * retrieve all used EV in the given plan
	 *
	 * @param plan
	 * @return
	 */
	private Set<Id<Vehicle>> getUsedEVToPreplan(Plan plan) {
		return TripStructureUtils.getLegs(plan)
				.stream()
				.map(leg -> VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode()))
				.filter(vehicleId -> isEVAndToBeChargedWhileActivities(vehicleId))
				.collect(toSet());
	}

	private boolean isEVAndToBeChargedWhileActivities(Id<Vehicle> vehicleId) {
		ElectricVehicleSpecification ev = this.electricFleetSpecification.getVehicleSpecifications().getOrDefault(Id.create(vehicleId, Vehicle.class), null);
		return (ev != null && UrbanEVUtils.isChargingDuringActivities(ev));
	}

	private void processPlans(Map<Plan, Set<Id<Vehicle>>> selectedEVPlans) {

		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup)config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);

		for (Plan plan : selectedEVPlans.keySet()) {

			//from here we deal with the modifiable plan (only!?)

			MobsimAgent mobsimagent = qsim.getAgents().get(plan.getPerson().getId());
			Plan modifiablePlan = WithinDayAgentUtils.getModifiablePlan(mobsimagent);
			TripRouter tripRouter = tripRouterProvider.get();
			Set<String> modesWithVehicles = new HashSet<>(scenario.getConfig().qsim().getMainModes());
			modesWithVehicles.addAll(scenario.getConfig().routing().getNetworkModes());

			for (Id<Vehicle> ev : selectedEVPlans.get(plan)) {
				//only replan cnt times per vehicle and person. otherwise, there might be a leg which is just too long and we end up in an infinity loop...
				int cnt = configGroup.getMaximumChargingProceduresPerAgent();
				/*
				 * i had all of this implemented without so many if-statements and without do-while-loop. However, i felt like when replanning takes place, we need to start
				 * consumption estimation all over. The path to avoid this would be by complicated date/method structure, which would also be bad (especially to maintain...)
				 * ts, nov' 27, 2020
				 */
				ElectricVehicleSpecification evSpec = electricFleetSpecification.getVehicleSpecifications().get(Id.create(ev, Vehicle.class));
				Leg legWithCriticalSOC;
				ElectricVehicle pseudoVehicle = ElectricFleetUtils.create(evSpec, driveConsumptionFactory, auxConsumptionFactory,
						chargingPowerFactory );
				//TODO: erase hardcoding of car mode!
				List<Leg> evCarLegs = TripStructureUtils.getLegs(modifiablePlan)
						.stream()
						.filter(leg -> leg.getMode().equals(TransportMode.car))
						.collect(toList());
				boolean pluginBeforeStart = configGroup.getPluginBeforeStartingThePlan();

				if (pluginBeforeStart && hasHomeCharger(mobsimagent, modifiablePlan, evCarLegs,
						pseudoVehicle)) { //TODO potentially check for activity duration and/or SoC
					//TODO this does not work !! the planning stuff right here works. However, in the qsim, the initial plugin trip is not reflected and thus the physical ev will not be charged! This then leads to (large) inconsistency of the planning and the qsim!
					//Thus, set pluginBeforeStart to false for the time being!!
					//tschlenther, july '22

					Leg firstEvLeg = evCarLegs.get(0);
					//					Activity originalActWhileCharging = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(firstEvLeg));
					//					Activity lastAct = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(firstEvLeg));
					Activity actWhileCharging = (Activity)modifiablePlan.getPlanElements().get(0);
					Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(firstEvLeg.getMode());
					Link chargingLink = modeNetwork.getLinks().get(actWhileCharging.getLinkId());
					String routingMode = TripStructureUtils.getRoutingMode(firstEvLeg);

					OptionalTime originalEndTime = actWhileCharging.getEndTime();
					OptionalTime originalDuration = actWhileCharging.getMaximumDuration();

					//pluginTrip
					PopulationFactory factory = scenario.getPopulation().getFactory();
					Activity newFirstAct = factory.createActivityFromLinkId(actWhileCharging.getType(), actWhileCharging.getLinkId());
					newFirstAct.setEndTime(0);
					modifiablePlan.getPlanElements().add(0, newFirstAct);
					planPluginTrip(modifiablePlan, routingMode, evSpec, newFirstAct, actWhileCharging, chargingLink, tripRouter);
					//reset original end time or duration
					if (originalDuration.isDefined())
						actWhileCharging.setMaximumDuration(originalDuration.seconds());
					if (originalEndTime.isDefined())
						actWhileCharging.setEndTime(originalEndTime.seconds());

					//	planPluginTripFromHomeToCharger(modifiablePlan, routingMode, evSpec, actWhileCharging, chargingLink, tripRouter);

					Leg plugoutLeg = firstEvLeg;
					Activity plugoutTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
					Activity plugoutTripDestination = findRealOrChargingActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
					planPlugoutTrip(modifiablePlan, routingMode, evSpec, plugoutTripOrigin, plugoutTripDestination, chargingLink, tripRouter,
							timeInterpretation.decideOnActivityEndTimeAlongPlan(plugoutTripOrigin, modifiablePlan).seconds());

					throw new RuntimeException(
							"currently, plugging in at qsim start does not work. needs debugging. was trying tat for agent " + mobsimagent.getId());
				}

				do {
					double newCharge = evSpec.getInitialCharge();
					pseudoVehicle.getBattery().setCharge(newCharge);
					legWithCriticalSOC = getCriticalOrLastEvLeg(modifiablePlan, pseudoVehicle, ev);

					if (legWithCriticalSOC != null) {
						String mode = legWithCriticalSOC.getMode();
						List<Leg> evLegs = TripStructureUtils.getLegs(modifiablePlan)
								.stream()
								.filter(leg -> leg.getMode().equals(mode))
								.collect(toList());

						if (evLegs.get(0).equals(legWithCriticalSOC)) {
							//critical leg is the first one of the plan
							log.warn("SoC of Agent"
									+ mobsimagent.getId()
									+ "is running beyond capacity threshold during the first leg of the day.\n"
									+ "One solution might be increasing the number of maximum charges per agent per day (as this could be the result from running empty at the end of the previous iteration).");
							AgentsOutOfEnergyData agentsOutOfEnergyData = new AgentsOutOfEnergyData(mobsimagent.getId(),
									"is running beyond capacity threshold during the first leg of the day.");
							UrbanEVTripsPlanner.agentsOutOfEnergyDataSets.add(agentsOutOfEnergyData);

							//TODO: maybe find nearest charger for next act and let vehicle run empty on first leg but then be charged!?
							break;
						} else if (evLegs.get(evLegs.size() - 1).equals(legWithCriticalSOC) && isHomeChargingTrip(mobsimagent, modifiablePlan, evLegs,
								pseudoVehicle) && pseudoVehicle.getBattery().getCharge() > 0) {
							//trip leads to location of the first (ev-leg-preceding-) activity in the plan and there is a charger and so we can charge at home do not search for opportunity charge before
							//if SoC == 0, we should search for an earlier charge

							Activity actBeforeCharging = EditPlans.findRealActBefore(mobsimagent,
									modifiablePlan.getPlanElements().indexOf(legWithCriticalSOC));
							Activity lastAct = EditPlans.findRealActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(legWithCriticalSOC));
							Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(legWithCriticalSOC.getMode());
							Link chargingLink = modeNetwork.getLinks()
									.get(lastAct.getLinkId()); //TODO possibly let the agent walk to a nearby charger!
							String routingMode = TripStructureUtils.getRoutingMode(legWithCriticalSOC);

							planPluginTrip(modifiablePlan, routingMode, evSpec, actBeforeCharging, lastAct, chargingLink, tripRouter);
							log.info(mobsimagent.getId() + " is charging at home.");
							AgentsOutOfEnergyData agentsOutOfEnergyData = new AgentsOutOfEnergyData(mobsimagent.getId(), "is charging at home.");
							UrbanEVTripsPlanner.agentsOutOfEnergyDataSets.add(agentsOutOfEnergyData);
							break;

						} else if (evLegs.get(evLegs.size() - 1).equals(legWithCriticalSOC)
								&& pseudoVehicle.getBattery().getSoc() > configGroup.getCriticalSOC()) {
							//critical leg is the last of the day but energy is above the threshold
							//TODO: plan for the next day! that means, check at least if the first leg can be done!
							cnt = 0;

						} else {
							//critical leg is somewhere in the middle of the plan
							if (replanPrecedentAndCurrentEVLegs(mobsimagent, modifiablePlan, evSpec, legWithCriticalSOC)) {
								log.info(mobsimagent.getId() + " is charging on the route.");
								AgentsOutOfEnergyData agentsOutOfEnergyData = new AgentsOutOfEnergyData(mobsimagent.getId(),
										"is charging on the route.");
								UrbanEVTripsPlanner.agentsOutOfEnergyDataSets.add(agentsOutOfEnergyData);
								cnt--;
							} else {
								log.warn("could not insert plugin activity in plan of agent "
										+ mobsimagent.getId()
										+ ".\n One reason could be that the agent has no suitable activity prior to the leg for which the "
										+ " energy threshold is expected to be exceeded. \n"
										+ "Another reason could be that there is no charger available in maxDistance to a prior activity (that would have been logged above).");
								AgentsOutOfEnergyData agentsOutOfEnergyData = new AgentsOutOfEnergyData(mobsimagent.getId(),
										"can't find a suitable activity (or charger) prior the critical leg!");
								UrbanEVTripsPlanner.agentsOutOfEnergyDataSets.add(agentsOutOfEnergyData);
								break; //could not insert, so no need to retry
							}
						}
					} else {
						throw new IllegalStateException("critical leg is null. should not happen");
					}

				} while (legWithCriticalSOC != null && cnt > 0);
			}
		}
	}

	/**
	 * returns leg for which the critical soc is exceeded or the last of all ev legs.
	 *
	 * @param modifiablePlan
	 * @param pseudoVehicle
	 * @param originalVehicleId
	 * @return
	 */
	private Leg getCriticalOrLastEvLeg(Plan modifiablePlan, ElectricVehicle pseudoVehicle, Id<Vehicle> originalVehicleId) {
		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup)config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);

		Double chargingBegin = null;

		Set<String> modesWithVehicles = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		modesWithVehicles.addAll(scenario.getConfig().routing().getNetworkModes());

		Leg lastLegWithVehicle = null;

		for (PlanElement planElement : modifiablePlan.getPlanElements()) {
			if (planElement instanceof Leg) {

				Leg leg = (Leg)planElement;
				if (modesWithVehicles.contains(leg.getMode()) && VehicleUtils.getVehicleId(modifiablePlan.getPerson(), leg.getMode())
						.equals(originalVehicleId)) {
					lastLegWithVehicle = leg;
					emulateVehicleDischarging(pseudoVehicle, leg);
					if (pseudoVehicle.getBattery().getSoc() <= configGroup.getCriticalSOC()) { // TODO randomize?
						return leg;
					}
				}
			} else if (planElement instanceof Activity) {
				if (((Activity)planElement).getType().contains( UrbanEVModule.PLUGIN_INTERACTION )) {
					Leg legToCharger = (Leg)modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(planElement) - 1);
					chargingBegin = legToCharger.getDepartureTime().seconds() + legToCharger.getTravelTime().seconds();

				} else if (((Activity)planElement).getType().contains( UrbanEVModule.PLUGOUT_INTERACTION )) {

					Leg legFromCharger = (Leg)modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(planElement) + 1);
					if (chargingBegin == null)
						throw new IllegalStateException();
					double chargingDuration = legFromCharger.getDepartureTime().seconds() - chargingBegin;

					ChargerSpecification chargerSpecification = chargingInfrastructureSpecification.getChargerSpecifications()
							.values()
							.stream()
							.filter(charger -> charger.getLinkId().equals(((Activity)planElement).getLinkId()))
							.filter(charger -> pseudoVehicle.getChargerTypes().contains(charger.getChargerType()))
							.findAny()
							.orElseThrow();

					double energy = pseudoVehicle.getChargingPower().calcChargingPower(chargerSpecification) * chargingDuration;
					double newCharge = Math.min(pseudoVehicle.getBattery().getCharge() + energy, pseudoVehicle.getBattery().getCapacity());
					pseudoVehicle.getBattery().setCharge(newCharge);
				}
			} else
				throw new IllegalArgumentException();
		}

		if (lastLegWithVehicle == null) {
			throw new RuntimeException(
					"found no leg with vehicle " + originalVehicleId + ". Should not happen. Is the corresponding mode a network mode?");
		} else {
			return lastLegWithVehicle;
		}
	}

	/**
	 * @param mobsimagent
	 * @param modifiablePlan
	 * @param electricVehicleSpecification
	 * @param leg
	 */
	private boolean replanPrecedentAndCurrentEVLegs(MobsimAgent mobsimagent, Plan modifiablePlan,
			ElectricVehicleSpecification electricVehicleSpecification, Leg leg) {
		Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(leg.getMode());

		String routingMode = TripStructureUtils.getRoutingMode(leg);
		List<Leg> evLegs = TripStructureUtils.getLegs(modifiablePlan).stream().filter(evleg -> evleg.getMode().equals(routingMode)).collect(toList());
		int legIndex = modifiablePlan.getPlanElements().indexOf(leg);
		Preconditions.checkState(legIndex > -1, "could not locate leg in plan");
		Activity actWhileCharging;
		ChargerSpecification selectedCharger;
		int legIndexCounter = legIndex;
		//find suitable non-stage activity before SOC threshold passover
		do {
			actWhileCharging = activityWhileChargingFinder.findActivityWhileChargingBeforeLeg(mobsimagent, modifiablePlan,
					(Leg)modifiablePlan.getPlanElements().get(legIndexCounter));
			if (actWhileCharging == null) {
				return false;
			}
			selectedCharger = selectChargerNearToLink(actWhileCharging.getLinkId(), electricVehicleSpecification, modeNetwork);

			if (selectedCharger == null) {
				log.warn("no charger within max distance could be found for link "
						+ actWhileCharging.getLinkId()
						+ ". will try to charge earlier in the plan of agent "
						+ mobsimagent.getId());
				leg = evLegs.get(evLegs.indexOf(leg) - 1);
				legIndexCounter = modifiablePlan.getPlanElements().indexOf(leg);
			}
		} while (actWhileCharging != null && selectedCharger == null);

		//TODO what if actWhileCharging does not hold a link id?

		Link chargingLink = modeNetwork.getLinks().get(selectedCharger.getLinkId());

		//		Activity pluginTripOrigin = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(actWhileCharging));
		Activity pluginTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(actWhileCharging));

		Leg plugoutLeg = activityWhileChargingFinder.getNextLegOfRoutingModeAfterActivity(ImmutableList.copyOf(modifiablePlan.getPlanElements()),
				actWhileCharging, routingMode);
		Activity plugoutTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
		Activity plugoutTripDestination = findRealOrChargingActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));

		{    //some consistency checks.. //TODO consider to put in a JUnit test..
			int offset = config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ? 1 : 3;

			Preconditions.checkNotNull(pluginTripOrigin, "pluginTripOrigin is null. should never happen..");
			Preconditions.checkState(!pluginTripOrigin.equals(actWhileCharging),
					"pluginTripOrigin is equal to actWhileCharging. should never happen..");

			PlanElement legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) + offset);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg)legToBeReplaced).equals(routingMode),
					"leg after pluginTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(actWhileCharging) - offset);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg)legToBeReplaced).equals(routingMode),
					"leg before actWhileCharging has the wrong routing mode. should not happen..");

			Preconditions.checkState(!plugoutTripDestination.equals(actWhileCharging),
					"plugoutTripDestination is equal to actWhileCharging. should never happen..");

			Preconditions.checkState(
					modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) < modifiablePlan.getPlanElements().indexOf(actWhileCharging));
			Preconditions.checkState(
					modifiablePlan.getPlanElements().indexOf(actWhileCharging) <= modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin));
			Preconditions.checkState(
					modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) < modifiablePlan.getPlanElements().indexOf(plugoutTripDestination));

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) + offset);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg)legToBeReplaced).equals(routingMode),
					"leg after plugoutTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripDestination) - offset);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg)legToBeReplaced).equals(routingMode),
					"leg before plugoutTripDestination has the wrong routing mode. should not happen..");
		}

		TripRouter tripRouter = tripRouterProvider.get();
		planPluginTrip(modifiablePlan, routingMode, electricVehicleSpecification, pluginTripOrigin, actWhileCharging, chargingLink, tripRouter);
		planPlugoutTrip(modifiablePlan, routingMode, electricVehicleSpecification, plugoutTripOrigin, plugoutTripDestination, chargingLink,
				tripRouter, timeInterpretation.decideOnActivityEndTimeAlongPlan(plugoutTripOrigin, modifiablePlan).seconds());
		return true;
	}

	private void planPlugoutTrip(Plan plan, String routingMode, ElectricVehicleSpecification electricVehicleSpecification, Activity origin,
			Activity destination, Link chargingLink, TripRouter tripRouter, double now) {
		Facility fromFacility = FacilitiesUtils.toFacility(origin, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(destination, scenario.getActivityFacilities());

		List<? extends PlanElement> routedSegment;
		//actually destination can not be null based on how we determine the actWhileCharging = origin at the moment...
		if (destination == null)
			throw new RuntimeException("should not happen");

		List<PlanElement> trip = new ArrayList<>();

		//add leg to charger
		routedSegment = tripRouter.calcRoute(TransportMode.walk, fromFacility, chargerFacility, now, plan.getPerson(), new AttributesImpl());
		Leg accessLeg = (Leg)routedSegment.get(0);
		now = timeInterpretation.decideOnElementEndTime(accessLeg, now).seconds();

		TripStructureUtils.setRoutingMode(accessLeg, routingMode);
		trip.add(accessLeg);

		//add plugout act
		Activity plugOutAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(), chargingLink.getId(),
				routingMode + UrbanEVModule.PLUGOUT_IDENTIFIER );
		plugOutAct = PopulationUtils.createActivity(plugOutAct); // createStageActivity... creates a InteractionActivity where duration cannot be set.
		trip.add(plugOutAct);
		now = timeInterpretation.decideOnElementEndTime(accessLeg, now).seconds();

		//add leg to destination
		routedSegment = tripRouter.calcRoute(routingMode, chargerFacility, toFacility, now, plan.getPerson(), new AttributesImpl());
		trip.addAll(routedSegment);

		for (PlanElement element : routedSegment) {
			now = timeInterpretation.decideOnElementEndTime(accessLeg, now).seconds();
			//insert vehicle id
			if (element instanceof Leg) {
				Leg leg = (Leg)element;
				if (leg.getMode().equals(routingMode)) {
					NetworkRoute route = ((NetworkRoute)leg.getRoute());
					if (route.getVehicleId() == null)
						route.setVehicleId(Id.createVehicleId(electricVehicleSpecification.getId()));
				}
			}
		}

		//insert trip
		TripRouter.insertTrip(plan, origin, trip, destination);

		//reset activity end time
		if (!plan.getPlanElements().get(plan.getPlanElements().size() - 1).equals(destination)) {
			destination.setEndTime(timeInterpretation.decideOnActivityEndTime(destination, now).seconds());
		}
	}

	private void planPluginTrip(Plan plan, String routingMode, ElectricVehicleSpecification electricVehicleSpecification, Activity actBeforeCharging,
			Activity actWhileCharging, Link chargingLink, TripRouter tripRouter) {
		Facility fromFacility = FacilitiesUtils.toFacility(actBeforeCharging, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(actWhileCharging, scenario.getActivityFacilities());

		List<PlanElement> trip = new ArrayList<>();
		//add leg to charger
		List<? extends PlanElement> routedSegment = tripRouter.calcRoute(routingMode, fromFacility, chargerFacility,
				timeInterpretation.decideOnActivityEndTimeAlongPlan(actBeforeCharging, plan).seconds(), plan.getPerson(), new AttributesImpl());

		//set the vehicle id
		for (Leg leg : TripStructureUtils.getLegs(routedSegment)) {
			if (leg.getMode().equals(routingMode)) {
				NetworkRoute route = ((NetworkRoute)leg.getRoute());
				if (route.getVehicleId() == null)
					route.setVehicleId(Id.createVehicleId(electricVehicleSpecification.getId()));
			}
		}

		Leg lastLeg = (Leg)routedSegment.get(routedSegment.size() - 1);
		double now = lastLeg.getDepartureTime().seconds() + lastLeg.getRoute().getTravelTime().seconds();
		trip.addAll(routedSegment);

		//add plugin act
		Activity pluginAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(), chargingLink.getId(),
				routingMode + UrbanEVModule.PLUGIN_IDENTIFIER );
		pluginAct = PopulationUtils.createActivity(pluginAct); // createStageActivity... creates a InteractionActivity where duration cannot be set.
		trip.add(pluginAct);

		now = timeInterpretation.decideOnActivityEndTime(pluginAct, now).seconds();

		//add walk leg to destination
		routedSegment = tripRouter.calcRoute(TransportMode.walk, chargerFacility, toFacility, now, plan.getPerson(), new AttributesImpl());
		Leg egress = (Leg)routedSegment.get(0);
		TripStructureUtils.setRoutingMode(egress, routingMode);
		trip.add(egress);
		now = timeInterpretation.decideOnElementEndTime(egress, now).seconds();

		//insert trip
		TripRouter.insertTrip(plan, actBeforeCharging, trip, actWhileCharging);

		//reset activity end time
		if (!plan.getPlanElements().get(plan.getPlanElements().size() - 1).equals(actWhileCharging)) {
			actWhileCharging.setEndTime(timeInterpretation.decideOnActivityEndTime(actWhileCharging, now).seconds());
		}
	}

	//possibly put behind interface
	@Nullable
	private ChargerSpecification selectChargerNearToLink(Id<Link> linkId, ElectricVehicleSpecification vehicleSpecification, Network network) {

		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup)config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);
		double maxDistanceToAct = configGroup.getMaxDistanceBetweenActAndCharger_m();

		List<ChargerSpecification> chargerList = chargingInfrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.filter(charger -> vehicleSpecification.getChargerTypes().contains(charger.getChargerType()))
				.collect(Collectors.toList());

		StraightLineKnnFinder<Link, ChargerSpecification> straightLineKnnFinder = new StraightLineKnnFinder<>(1, l -> l.getToNode().getCoord(),
				s -> network.getLinks().get(s.getLinkId()).getToNode().getCoord()); //TODO get closest X chargers and choose randomly?
		List<ChargerSpecification> nearestChargers = straightLineKnnFinder.findNearest(network.getLinks().get(linkId), chargerList.stream());
		if (nearestChargers.isEmpty()) {
			throw new RuntimeException("no charger could be found for vehicle: " + vehicleSpecification);
		}
		ChargerSpecification chosenCharger = nearestChargers.get(0); // currently we chose the closest one
		double distanceFromActToCharger = NetworkUtils.getEuclideanDistance(network.getLinks().get(linkId).getToNode().getCoord(),
				network.getLinks().get(chosenCharger.getLinkId()).getToNode().getCoord());
		if (distanceFromActToCharger >= maxDistanceToAct) {
			return null;
		} else {
			return chosenCharger;
		}
	}

	/**
	 * this method has the side effect that the soc of the ev is altered by estimated energy consumption of the leg
	 *
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
			ev.getBattery().dischargeEnergy(consumption, missingEnergy -> {
				throw new RuntimeException("Energy consumed greater than the current charge. Missing energy: " + missingEnergy);
			});
			linkEnterTime += travelT;
		}
	}

	//	the following methods are modified versions of EditPlans.findRealActBefore() and EditPlans.findRealActAfter()

	private Activity findRealOrChargingActBefore(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		Activity prevAct = null;
		for (int ii = 0; ii < index; ii++) {
			if (planElements.get(ii) instanceof Activity) {
				Activity act = (Activity)planElements.get(ii);
				if (!StageActivityTypeIdentifier.isStageActivity(act.getType()) || act.getType()
						.contains( UrbanEVModule.PLUGIN_INTERACTION ) || act.getType()
												    .contains( UrbanEVModule.PLUGOUT_INTERACTION )) {
					prevAct = act;
				}
			}
		}
		return prevAct;
	}

	private Activity findRealOrChargingActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();
		return (Activity)planElements.get(findIndexOfRealActAfter(agent, index));
	}

	private int findIndexOfRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int theIndex = -1;
		for (int ii = planElements.size() - 1; ii > index; ii--) {
			if (planElements.get(ii) instanceof Activity) {
				Activity act = (Activity)planElements.get(ii);
				if (!StageActivityTypeIdentifier.isStageActivity(act.getType()) || act.getType()
						.contains( UrbanEVModule.PLUGIN_INTERACTION ) || act.getType()
												    .contains( UrbanEVModule.PLUGOUT_INTERACTION )) {
					theIndex = ii;
				}
			}
		}
		return theIndex;
	}

	private Boolean isHomeChargingTrip(MobsimAgent mobsimAgent, Plan modifiablePlan, List<Leg> evLegs, ElectricVehicle ev) {

		int firstEvLegIndex = modifiablePlan.getPlanElements().indexOf(evLegs.get(0));
		Id<Link> homeLink = EditPlans.findRealActBefore(mobsimAgent, firstEvLegIndex).getLinkId();
		boolean isHomeTrip = EditPlans.findRealActAfter(mobsimAgent, modifiablePlan.getPlanElements().indexOf(evLegs.get(evLegs.size() - 1)))
				.getLinkId()
				.equals(homeLink);
		boolean hasHomeCharger = chargingInfrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.filter(chargerSpecification -> ev.getChargerTypes().contains(chargerSpecification.getChargerType()))
				.map(chargerSpecification -> chargerSpecification.getLinkId())
				.anyMatch(linkId -> linkId.equals(homeLink));

		return isHomeTrip && hasHomeCharger;
	}

	private boolean hasHomeCharger(MobsimAgent mobsimAgent, Plan modifiablePlan, List<Leg> evLegs, ElectricVehicle ev) {

		int firstEvLegIndex = modifiablePlan.getPlanElements().indexOf(evLegs.get(0));
		Id<Link> homeLink = EditPlans.findRealActBefore(mobsimAgent, firstEvLegIndex).getLinkId();
		boolean hasHomeCharger = chargingInfrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.filter(chargerSpecification -> ev.getChargerTypes().contains(chargerSpecification.getChargerType()))
				.map(chargerSpecification -> chargerSpecification.getLinkId())
				.anyMatch(linkId -> linkId.equals(homeLink));
		return hasHomeCharger;
	}

	private final void logAgentsOutOfEnergy() {
		CSVPrinter csvPrinter;
		try {
			csvPrinter = new CSVPrinter(Files.newBufferedWriter(
					Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "agentsOutOfEnergy.csv"))),
					CSVFormat.DEFAULT.withDelimiter(';').withHeader("PersonID", "Reason"));
			{
				for (AgentsOutOfEnergyData agentsOutOfEnergyData : UrbanEVTripsPlanner.agentsOutOfEnergyDataSets) {
					csvPrinter.printRecord(agentsOutOfEnergyData.personId, agentsOutOfEnergyData.reason);
				}
			}
			csvPrinter.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		agentsOutOfEnergyDataSets.clear();
	}

	private class AgentsOutOfEnergyData {
		private final Id<Person> personId;
		private final String reason;

		AgentsOutOfEnergyData(Id<Person> personId, String reason) {
			this.personId = personId;
			this.reason = reason;

		}
	}
}



