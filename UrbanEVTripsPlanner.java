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
import com.sun.istack.Nullable;
import one.util.streamex.StreamEx;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.jcodec.containers.mp4.boxes.Edit;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.EvUnits;
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
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.*;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.urbanEV.analysis.ActsWhileChargingAnalyzer;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.utils.EditPlans;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	OutputDirectoryHierarchy controlerIO;
	@Inject
	IterationCounter iterationCounter;

	@Inject
	Config config;

	private QSim qsim;

	private static final Logger log = Logger.getLogger(UrbanEVTripsPlanner.class);
	private static List<PersonContainer2> personContainer2s = new ArrayList<>();

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		if (!(e.getQueueSimulation() instanceof QSim)) {
			throw new IllegalStateException(UrbanEVTripsPlanner.class.toString() + " only works with a mobsim of type " + QSim.class);
		}
		//collect all selected plans that contain ev legs and map them to the set of ev used
		Map<Plan, Set<Id<Vehicle>>> selectedEVPlans = StreamEx.of(scenario.getPopulation().getPersons().values())
				.mapToEntry(p -> p.getSelectedPlan(), p -> getUsedEV(p.getSelectedPlan()))
				.filterValues(evSet -> !evSet.isEmpty())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		this.qsim = (QSim) e.getQueueSimulation();
		processPlans(selectedEVPlans);
		CSVPrinter csvPrinter;
		try {
			csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "outOfEnergy.csv"))), CSVFormat.DEFAULT.withDelimiter(';').
					withHeader("PersonID", "Reason"));

			{
				for (PersonContainer2 personContainer2 : personContainer2s) {


					csvPrinter.printRecord(personContainer2.personId, personContainer2.reason);
				}
			}




			csvPrinter.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	personContainer2s.clear();
	}

	/**
	 * retrieve all used EV in the given plan
	 *
	 * @param plan
	 * @return
	 */
	private Set<Id<Vehicle>> getUsedEV(Plan plan) {
		return TripStructureUtils.getLegs(plan).stream()
				.map(leg -> VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode()))
				.filter(vehicleId -> isEV(vehicleId))
				.collect(toSet());
	}

	private boolean isEV(Id<Vehicle> vehicleId) {
		return this.electricFleetSpecification.getVehicleSpecifications().containsKey(getWrappedElectricVehicleId(vehicleId));
	}

	private void processPlans(Map<Plan, Set<Id<Vehicle>>> selectedEVPlans) {


		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup) config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);

		for (Plan plan : selectedEVPlans.keySet()) {

			//from here we deal with the modifiable plan (only!?)

			MobsimAgent mobsimagent = qsim.getAgents().get(plan.getPerson().getId());
			Plan modifiablePlan = WithinDayAgentUtils.getModifiablePlan(mobsimagent);
			TripRouter tripRouter = tripRouterProvider.get();
			Set<String> modesWithVehicles = new HashSet<>(scenario.getConfig().qsim().getMainModes());
			modesWithVehicles.addAll(scenario.getConfig().plansCalcRoute().getNetworkModes());


			for (Id<Vehicle> ev : selectedEVPlans.get(plan)) {
				//only replan cnt times per vehicle and person. otherwise, there might be a leg which is just too long and we end up in an infinity loop...
				int cnt = configGroup.getMaximumChargingProceduresPerAgent();
//				boolean pluginAtHomeBeforeMobSim = configGroup.getPluginAtHomeBeforeMobSim;
				/*
				 * i had all of this implemented without so many if-statements and without do-while-loop. However, i felt like when replanning takes place, we need to start
				 * consumption estimation all over. The path to avoid this would be by complicated date/method structure, which would also be bad (especially to maintain...)
				 * ts, nov' 27, 2020
				 */
				ElectricVehicleSpecification electricVehicleSpecification = electricFleetSpecification.getVehicleSpecifications()
						.get(getWrappedElectricVehicleId(ev));
				Leg legWithCriticalSOC;
				ElectricVehicle pseudoVehicle = ElectricVehicleImpl.create(electricVehicleSpecification, driveConsumptionFactory, auxConsumptionFactory, chargingPowerFactory);;
				List <Leg> evCarLegs = TripStructureUtils.getLegs(modifiablePlan).stream().filter(leg -> leg.getMode().equals(TransportMode.car)).collect(toList());
				boolean pluginBeforeStart = configGroup.getPluginBeforeStartingThePlan();

				if(pluginBeforeStart && hasHomeCharger(mobsimagent, modifiablePlan, evCarLegs, pseudoVehicle)){ //TODO potentially check for activity duration and/or SoC

					Leg firstEvLeg = evCarLegs.get(0);
//					Activity originalActWhileCharging = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(firstEvLeg));
//					Activity lastAct = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(firstEvLeg));
					Activity actWhileCharging = (Activity) modifiablePlan.getPlanElements().get(0);
					Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(firstEvLeg.getMode());
					Link chargingLink = modeNetwork.getLinks().get(actWhileCharging.getLinkId());
					String routingMode = TripStructureUtils.getRoutingMode(firstEvLeg);
					planPluginTripFromHomeToCharger(modifiablePlan, routingMode, electricVehicleSpecification, actWhileCharging, chargingLink, tripRouter);
					Leg plugoutLeg = firstEvLeg;
					Activity plugoutTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
					Activity plugoutTripDestination = findRealOrChargingActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
					planPlugoutTrip(modifiablePlan, routingMode, electricVehicleSpecification, plugoutTripOrigin, plugoutTripDestination, chargingLink, tripRouter, PlanRouter.calcEndOfActivity(plugoutTripOrigin, modifiablePlan, config));
					//TODO don't we need to trigger SoC emulation here (in order to account for the energy charged before home actvity is ended?) see also todo-comment below
				}
				

				do {
					double newSoC = EVUtils.getInitialEnergy(vehicles.getVehicles().get(ev).getType().getEngineInformation())* EvUnits.J_PER_kWh; //TODO is this correct if vehicle was plugged in before start (sse above) ?
					pseudoVehicle.getBattery().setSoc(newSoC);
					double capacityThreshold = pseudoVehicle.getBattery().getCapacity() * (configGroup.getCriticalRelativeSOC());
					legWithCriticalSOC = getCriticalOrLastEvLeg(modifiablePlan, pseudoVehicle, ev);
					String mode = legWithCriticalSOC.getMode();
					List <Leg> evLegs = TripStructureUtils.getLegs(modifiablePlan).stream().filter(leg -> leg.getMode().equals(mode)).collect(toList());
					

					if (legWithCriticalSOC != null) {

						if (evLegs.get(0).equals(legWithCriticalSOC)) {
							log.warn("SoC of Agent" + mobsimagent.getId() + "is running beyond capacity threshold during the first leg of the day.");
							PersonContainer2 personContainer2 = new PersonContainer2(mobsimagent.getId(), "is running beyond capacity threshold during the first leg of the day.");
							personContainer2s.add(personContainer2);
							break;
						}

						else if (evLegs.get(evLegs.size()-1).equals(legWithCriticalSOC) && isHomeChargingTrip(mobsimagent, modifiablePlan, evLegs, pseudoVehicle) && pseudoVehicle.getBattery().getSoc() > 0) {

							//trip leads to location of the first activity in the plan and there is a charger and so we can charge at home do not search for opportunity charge before
							Activity originalActWhileCharging = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(legWithCriticalSOC));
							Activity lastAct = EditPlans.findRealActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(legWithCriticalSOC));
							Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(legWithCriticalSOC.getMode());
							Link chargingLink = modeNetwork.getLinks().get(lastAct.getLinkId());
							String routingMode = TripStructureUtils.getRoutingMode(legWithCriticalSOC);

							planPluginTrip(modifiablePlan, routingMode, electricVehicleSpecification, originalActWhileCharging, lastAct, chargingLink, tripRouter);
							log.info(mobsimagent.getId() + " is charging at home.");
							PersonContainer2 personContainer2 = new PersonContainer2(mobsimagent.getId(), "is charging at home.");
							personContainer2s.add(personContainer2);
							break;

						} else if( evLegs.get(evLegs.size()-1).equals(legWithCriticalSOC) && pseudoVehicle.getBattery().getSoc() > capacityThreshold ){
							cnt = 0;

						} else {
							replanPrecedentAndCurrentEVLegs(mobsimagent, modifiablePlan, electricVehicleSpecification, legWithCriticalSOC);
							log.info(mobsimagent.getId() + " is charging on the route.");
							PersonContainer2 personContainer2 = new PersonContainer2(mobsimagent.getId(), "is charging on the route.");
							personContainer2s.add(personContainer2);
							cnt--;
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
		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup) config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);


		double capacityThreshold = pseudoVehicle.getBattery().getCapacity() * (configGroup.getCriticalRelativeSOC()); //TODO randomize? Might also depend on the battery size!

		Double chargingBegin = null;

		Set<String> modesWithVehicles = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		modesWithVehicles.addAll(scenario.getConfig().plansCalcRoute().getNetworkModes());

		Leg lastLegWithVehicle = null;

		for (PlanElement planElement : modifiablePlan.getPlanElements()) {
			if (planElement instanceof Leg) {

				Leg leg = (Leg) planElement;
				if (modesWithVehicles.contains(leg.getMode()) && VehicleUtils.getVehicleId(modifiablePlan.getPerson(), leg.getMode()).equals(originalVehicleId)) {
					lastLegWithVehicle = leg;
					emulateVehicleDischarging(pseudoVehicle, leg);
					if (pseudoVehicle.getBattery().getSoc() <= capacityThreshold) {
						return leg;
					}
				}
			} else if (planElement instanceof Activity) {
				if (((Activity) planElement).getType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION)) {
					Leg legToCharger = (Leg) modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(planElement) - 1);
					chargingBegin = legToCharger.getDepartureTime().seconds() + legToCharger.getTravelTime().seconds();

				} else if (((Activity) planElement).getType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION)) {

					Leg legFromCharger = (Leg) modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(planElement) + 1);
					if (chargingBegin == null) throw new IllegalStateException();
					double chargingDuration = legFromCharger.getDepartureTime().seconds() - chargingBegin;

					ChargerSpecification chargerSpecification = chargingInfrastructureSpecification.getChargerSpecifications()
							.values()
							.stream()
							.filter(charger -> charger.getLinkId().equals(((Activity) planElement).getLinkId()))
							.filter(charger -> pseudoVehicle.getChargerTypes().contains(charger.getChargerType()))
							.findAny().orElseThrow();

					pseudoVehicle.getBattery().changeSoc(pseudoVehicle.getChargingPower().calcChargingPower(chargerSpecification) * chargingDuration);
				}
			} else throw new IllegalArgumentException();
		}

		if(lastLegWithVehicle == null){
			throw new RuntimeException("found no leg with vehicle " + originalVehicleId + ". Should not happen. Is the corresponding mode a network mode?");
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
	private void replanPrecedentAndCurrentEVLegs(MobsimAgent mobsimagent, Plan modifiablePlan, ElectricVehicleSpecification electricVehicleSpecification, Leg leg) {
		Network modeNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(leg.getMode());

		String routingMode = TripStructureUtils.getRoutingMode(leg);
		List <Leg> evLegs = TripStructureUtils.getLegs(modifiablePlan).stream().filter(evleg -> evleg.getMode().equals(routingMode)).collect(toList());
		int legIndex = modifiablePlan.getPlanElements().indexOf(leg);
		Preconditions.checkState(legIndex > -1, "could not locate leg in plan");
		Activity actWhileCharging;
		ChargerSpecification selectedCharger;
		int legIndexCounter = legIndex;
		//find suitable non-stage activity before SOC threshold passover
		do {
			actWhileCharging = activityWhileChargingFinder.findActivityWhileChargingBeforeLeg(mobsimagent, modifiablePlan, (Leg) modifiablePlan.getPlanElements().get(legIndexCounter));
			if (actWhileCharging == null){
				log.warn(mobsimagent + " can't find a suitable activity prior the critical leg!");
				PersonContainer2 personContainer2 = new PersonContainer2(mobsimagent.getId(), "can't find a suitable activity prior the critical leg!");
				personContainer2s.add(personContainer2);
				return;
			}
			selectedCharger = selectChargerNearToLink(actWhileCharging.getLinkId(), electricVehicleSpecification, modeNetwork);

			if(selectedCharger == null){

				leg = evLegs.get(evLegs.indexOf(leg)-1);
				legIndexCounter = modifiablePlan.getPlanElements().indexOf(leg);
			}
		} while (actWhileCharging != null && selectedCharger == null);


//		Preconditions.checkNotNull(actWhileCharging, "could not insert plugin activity in plan of agent " + mobsimagent.getId() +
//				".\n One reason could be that the agent has no suitable activity prior to the leg for which the " +
//				" energy threshold is expected to be exceeded. \n" +
//				" Another reason  might be that it's vehicle is running beyond energy threshold during the first leg of the day." +
//				"That could possibly be avoided by using EVNetworkRoutingModule..."); //TODO let the sim just run and let the ev run empty!?

		//TODO what if actWhileCharging does not hold a link id?

		Link chargingLink = modeNetwork.getLinks().get(selectedCharger.getLinkId());

//		Activity pluginTripOrigin = EditPlans.findRealActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(actWhileCharging));
		Activity pluginTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(actWhileCharging));

		Leg plugoutLeg = activityWhileChargingFinder.getNextLegOfRoutingModeAfterActivity(ImmutableList.copyOf(modifiablePlan.getPlanElements()), actWhileCharging, routingMode);
		Activity plugoutTripOrigin = findRealOrChargingActBefore(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));
		Activity plugoutTripDestination = findRealOrChargingActAfter(mobsimagent, modifiablePlan.getPlanElements().indexOf(plugoutLeg));

//		{    //some consistency checks.. //TODO consider to put in a JUnit test..
			Preconditions.checkNotNull(pluginTripOrigin, "pluginTripOrigin is null. should never happen..");
			Preconditions.checkState(!pluginTripOrigin.equals(actWhileCharging), "pluginTripOrigin is equal to actWhileCharging. should never happen..");

			PlanElement legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) + 3);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg after pluginTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(actWhileCharging) - 3);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg before actWhileCharging has the wrong routing mode. should not happen..");

			Preconditions.checkState(!plugoutTripDestination.equals(actWhileCharging), "plugoutTripDestination is equal to actWhileCharging. should never happen..");

			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(pluginTripOrigin) < modifiablePlan.getPlanElements().indexOf(actWhileCharging));
			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(actWhileCharging) <= modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin));
			Preconditions.checkState(modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) < modifiablePlan.getPlanElements().indexOf(plugoutTripDestination));

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripOrigin) + 3);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg after plugoutTripOrigin has the wrong routing mode. should not happen..");

			legToBeReplaced = modifiablePlan.getPlanElements().get(modifiablePlan.getPlanElements().indexOf(plugoutTripDestination) - 3);
			Preconditions.checkState(legToBeReplaced instanceof Leg);
			Preconditions.checkState(TripStructureUtils.getRoutingMode((Leg) legToBeReplaced).equals(routingMode), "leg before plugoutTripDestination has the wrong routing mode. should not happen..");
//		}

		TripRouter tripRouter = tripRouterProvider.get();
		planPluginTrip(modifiablePlan, routingMode, electricVehicleSpecification, pluginTripOrigin, actWhileCharging, chargingLink, tripRouter);
		planPlugoutTrip(modifiablePlan, routingMode, electricVehicleSpecification, plugoutTripOrigin, plugoutTripDestination, chargingLink, tripRouter, PlanRouter.calcEndOfActivity(plugoutTripOrigin, modifiablePlan, config));

	}

	private void planPlugoutTrip(Plan plan, String routingMode, ElectricVehicleSpecification electricVehicleSpecification, Activity origin, Activity destination, Link chargingLink, TripRouter tripRouter, double now) {
		Facility fromFacility = FacilitiesUtils.toFacility(origin, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(destination, scenario.getActivityFacilities());

		List<? extends PlanElement> routedSegment;
		//actually destination can not be null based on how we determine the actWhileCharging = origin at the moment...
		if (destination == null) throw new RuntimeException("should not happen");

		List<PlanElement> trip = new ArrayList<>();

		//add leg to charger
		routedSegment = tripRouter.calcRoute(TransportMode.walk, fromFacility, chargerFacility,
				now, plan.getPerson());
		Leg accessLeg = (Leg) routedSegment.get(0);
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
		trip.addAll(routedSegment);

		for (PlanElement element : routedSegment) {
			now = TripRouter.calcEndOfPlanElement(now, element, config);
			//insert vehicle id
			if(element instanceof Leg){
				Leg leg = (Leg) element;
				if(leg.getMode().equals(routingMode)){
					NetworkRoute route = ((NetworkRoute) leg.getRoute());
					if(route.getVehicleId() == null) route.setVehicleId(Id.createVehicleId(electricVehicleSpecification.getId()));
				}
			}
		}

		//insert trip
		TripRouter.insertTrip(plan, origin, trip, destination);

		//reset activity end time
		if (!plan.getPlanElements().get(plan.getPlanElements().size() - 1).equals(destination)) {
			destination.setEndTime(PopulationUtils.decideOnActivityEndTime(destination, now, config).seconds());
		}
	}

	private void planPluginTrip(Plan plan, String routingMode, ElectricVehicleSpecification electricVehicleSpecification, Activity actBeforeCharging, Activity actWhileCharging, Link chargingLink, TripRouter tripRouter) {
		Facility fromFacility = FacilitiesUtils.toFacility(actBeforeCharging, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(actWhileCharging, scenario.getActivityFacilities());

		List<PlanElement> trip = new ArrayList<>();
		//add leg to charger
		List<? extends PlanElement> routedSegment = tripRouter.calcRoute(routingMode, fromFacility, chargerFacility,
				PlanRouter.calcEndOfActivity(actBeforeCharging, plan, config), plan.getPerson());

		//set the vehicle id
		for (Leg leg : TripStructureUtils.getLegs(routedSegment)) {
			if(leg.getMode().equals(routingMode)){
				NetworkRoute route = ((NetworkRoute) leg.getRoute());
				if(route.getVehicleId() == null) route.setVehicleId(Id.createVehicleId(electricVehicleSpecification.getId()));
			}
		}

		Leg lastLeg = (Leg) routedSegment.get(routedSegment.size() - 1);
		double now = lastLeg.getDepartureTime().seconds() + lastLeg.getRoute().getTravelTime().seconds();
		trip.addAll(routedSegment);

		//add plugin act
		Activity pluginAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(),
				chargingLink.getId(), routingMode + UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER);
		trip.add(pluginAct);

		now = TripRouter.calcEndOfPlanElement(now, pluginAct, config);

		//add walk leg to destination
		routedSegment = tripRouter.calcRoute(TransportMode.walk, chargerFacility, toFacility, now, plan.getPerson());
		Leg egress = (Leg) routedSegment.get(0);
		TripStructureUtils.setRoutingMode(egress, routingMode);
		trip.add(egress);
		now = TripRouter.calcEndOfPlanElement(now, egress, config);

		//insert trip
		TripRouter.insertTrip(plan, actBeforeCharging, trip, actWhileCharging);

		//reset activity end time
		if (!plan.getPlanElements().get(plan.getPlanElements().size()-1).equals(actWhileCharging)) {
			actWhileCharging.setEndTime(PopulationUtils.decideOnActivityEndTime(actWhileCharging, now, config).seconds());
		}
	}


	//TODO possibly put behind interface
	@Nullable
	private ChargerSpecification selectChargerNearToLink(Id<Link> linkId, ElectricVehicleSpecification vehicleSpecification, Network network) {

		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup) config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);
		double maxDistanceToAct = configGroup.getMaxDistanceBetweenActAndCharger_m();

		List<ChargerSpecification> chargerList = chargingInfrastructureSpecification.getChargerSpecifications()
				.values()
				.stream()
				.filter(charger -> vehicleSpecification.getChargerTypes().contains(charger.getChargerType()))
				.collect(Collectors.toList());

		StraightLineKnnFinder<Link, ChargerSpecification> straightLineKnnFinder = new StraightLineKnnFinder<>(
				1, l -> l.getFromNode().getCoord(), s -> network.getLinks().get(s.getLinkId()).getToNode().getCoord()); //TODO get closest X chargers and choose randomly?
		List<ChargerSpecification> nearestChargers = straightLineKnnFinder.findNearest(network.getLinks().get(linkId),chargerList.stream());
		if (nearestChargers.isEmpty()) {
			throw new RuntimeException("no charger could be found for vehicle type " + vehicleSpecification.getVehicleType());
		}
		double distanceFromActToCharger = NetworkUtils.getEuclideanDistance(network.getLinks().get(linkId).getToNode().getCoord(), network.getLinks().get(nearestChargers.get(0).getLinkId()).getToNode().getCoord());
		if (distanceFromActToCharger >= maxDistanceToAct) {
			return null;
		}
//			//throw new RuntimeException("There are no chargers within 1000m");
//			log.warn("Charger out of range. Inefficient charging " + NetworkUtils.getEuclideanDistance(network.getLinks().get(linkId).getToNode().getCoord(), network.getLinks().get(nearestChargers.get(0).getLinkId()).getToNode().getCoord()));
//		}
		else{
			return nearestChargers.get(0);
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
		NetworkRoute route = (NetworkRoute) leg.getRoute();
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

//	the following methods are modified versions of EditPlans.findRealActBefore() and EditPlans.findRealActAfter()

	private Activity findRealOrChargingActBefore(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		Activity prevAct = null;
		for (int ii = 0; ii < index; ii++) {
			if (planElements.get(ii) instanceof Activity) {
				Activity act = (Activity) planElements.get(ii);
				if (!StageActivityTypeIdentifier.isStageActivity(act.getType()) ||
						act.getType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION) ||
						act.getType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION)) {
					prevAct = act;
				}
			}
		}
		return prevAct;
	}

	private Activity findRealOrChargingActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();
		return (Activity) planElements.get(findIndexOfRealActAfter(agent, index));
	}

	private int findIndexOfRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int theIndex = -1;
		for (int ii = planElements.size() - 1; ii > index; ii--) {
			if (planElements.get(ii) instanceof Activity) {
				Activity act = (Activity) planElements.get(ii);
				if (!StageActivityTypeIdentifier.isStageActivity(act.getType()) ||
						act.getType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION) ||
						act.getType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION)) {
					theIndex = ii;
				}
			}
		}
		return theIndex;
	}

	private Boolean isHomeChargingTrip(MobsimAgent mobsimAgent, Plan modifiablePlan, List<Leg> evLegs, ElectricVehicle ev) {

		int firstEvLegIndex = modifiablePlan.getPlanElements().indexOf(evLegs.get(0));
		Id<Link> homeLink = EditPlans.findRealActBefore(mobsimAgent,firstEvLegIndex).getLinkId();
		boolean isHomeTrip = EditPlans.findRealActAfter(mobsimAgent,modifiablePlan.getPlanElements().indexOf(evLegs.get(evLegs.size()-1))).getLinkId().equals(homeLink);
		boolean hasHomeCharger = chargingInfrastructureSpecification.getChargerSpecifications().values().stream()
				.filter(chargerSpecification -> ev.getChargerTypes().contains(chargerSpecification.getChargerType()))
				.map(chargerSpecification -> chargerSpecification.getLinkId())
				.anyMatch(linkId -> linkId.equals(homeLink));


		return isHomeTrip && hasHomeCharger;
	}
	
	private boolean hasHomeCharger(MobsimAgent mobsimAgent, Plan modifiablePlan, List<Leg> evLegs, ElectricVehicle ev){
		
		int firstEvLegIndex = modifiablePlan.getPlanElements().indexOf(evLegs.get(0));
		Id<Link> homeLink = EditPlans.findRealActBefore(mobsimAgent,firstEvLegIndex).getLinkId();
		boolean hasHomeCharger = chargingInfrastructureSpecification.getChargerSpecifications().values().stream()
				.filter(chargerSpecification -> ev.getChargerTypes().contains(chargerSpecification.getChargerType()))
				.map(chargerSpecification -> chargerSpecification.getLinkId())
				.anyMatch(linkId -> linkId.equals(homeLink));
		return hasHomeCharger;
	}
	private void planPluginTripFromHomeToCharger(Plan plan, String routingMode, ElectricVehicleSpecification electricVehicleSpecification,Activity actWhileCharging, Link chargingLink, TripRouter tripRouter) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Facility fromFacility = FacilitiesUtils.toFacility(actWhileCharging, scenario.getActivityFacilities());
		Facility chargerFacility = new LinkWrapperFacility(chargingLink);
		Facility toFacility = FacilitiesUtils.toFacility(actWhileCharging, scenario.getActivityFacilities());

		//copy the First Act and set the end time to 0s. Since every plan has to start with an act
		Activity newFirstAct = factory.createActivityFromLinkId(actWhileCharging.getType(), actWhileCharging.getLinkId());
		newFirstAct.setEndTime(0);
		double now = 0;
		TripRouter.calcEndOfPlanElement(now, newFirstAct, config);
		plan.getPlanElements().add(0, newFirstAct);


		//add leg to charger
		List<? extends PlanElement> routeToCharger = tripRouter.calcRoute(TransportMode.car, fromFacility, chargerFacility, now, plan.getPerson());
		//set the vehicle id
		for (Leg leg : TripStructureUtils.getLegs(routeToCharger)) {
			if(leg.getMode().equals(routingMode)){
				NetworkRoute route = ((NetworkRoute) leg.getRoute());
				if(route.getVehicleId() == null) route.setVehicleId(Id.createVehicleId(electricVehicleSpecification.getId()));
			}
		}


		//add plugin act
		Activity pluginAct = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(chargingLink.getCoord(),
				chargingLink.getId(), routingMode + UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER);
		plan.getPlanElements().add(2, pluginAct);
		TripRouter.insertTrip(plan, newFirstAct,routeToCharger,pluginAct);
		//add walk leg to destination
		List<? extends PlanElement> routeFromChargerToAct = tripRouter.calcRoute(TransportMode.walk, chargerFacility, toFacility,
				PlanRouter.calcEndOfActivity(newFirstAct, plan, config), plan.getPerson());
		int indexRouteFromChargerToAct = plan.getPlanElements().indexOf(pluginAct) + 1;
		plan.getPlanElements().add(indexRouteFromChargerToAct, routeFromChargerToAct.get(0));
		plan.getPlanElements().add(indexRouteFromChargerToAct+1, actWhileCharging);

	}


		class PersonContainer2{
	private final Id<Person> personId;
	private final String reason;


	PersonContainer2 (Id<Person> personId, String reason){
		this.personId = personId;
		this.reason = reason;

	}


}
}



