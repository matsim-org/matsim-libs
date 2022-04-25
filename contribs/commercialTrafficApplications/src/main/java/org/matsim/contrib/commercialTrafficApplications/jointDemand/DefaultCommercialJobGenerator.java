/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.jsprit.VRPTransportCostsFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.contrib.commercialTrafficApplications.jointDemand.JointDemandUtils.*;

/**
 * Generates carriers and tours depending on next iteration's freight demand
 */
class DefaultCommercialJobGenerator implements CommercialJobGenerator {
	
	private final double firstTourTraveltimeBuffer;
    private Scenario scenario;
    private Carriers carriers;
	private boolean enableTourPlanning = true;
    private Set<Id<Person>> freightDrivers = new HashSet<>();
    private Set<Id<Vehicle>> freightVehicles = new HashSet<>();
	private StrategyManager strategyManager;
    private final static Logger log = LogManager.getLogger(DefaultCommercialJobGenerator.class);
	private int changeOperatorInterval;
    private Set<String> drtModes = new HashSet<>();
    private VRPTransportCostsFactory vrpTransportCostsFactory;

    @Inject
    DefaultCommercialJobGenerator(StrategyManager strategyManager, Scenario scenario, Carriers carriers, VRPTransportCostsFactory vrpTransportCostsFactory) {
        JointDemandConfigGroup cfg = JointDemandConfigGroup.get(scenario.getConfig());
        this.carriers = carriers;
        this.firstTourTraveltimeBuffer = cfg.getFirstLegTraveltimeBufferFactor();
        this.scenario = scenario;
        this.strategyManager = strategyManager;
        this.changeOperatorInterval = cfg.getChangeCommercialJobOperatorInterval();
        this.vrpTransportCostsFactory = vrpTransportCostsFactory;
        getDrtModes(scenario.getConfig());
    }

    /**
	 * Converts Jsprit tours to MATSim freight agents and adjusts departure times of
	 * leg and end times of activities
	 */
	public void createAndAddFreightAgents(Carriers carriers, Population population) {

		for (Carrier carrier : carriers.getCarriers().values()) {
			int nextId = 0;

			for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
				CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
				Id<Person> driverId = JointDemandUtils.generateDriverId(carrier, carrierVehicle, nextId);
				nextId++;
				Person driverPerson = createDriverPerson(driverId);

				// Transform Jsprit output to MATSim freight agents
				Plan plainPlan = createPlainPlanFromTour(carrier, scheduledTour);

				// Adjust Jsprit departure times and activity end times to avoid too early services
				manageJspritDepartureTimes(plainPlan);

				driverPerson.addPlan(plainPlan);
				plainPlan.setPerson(driverPerson);
				population.addPerson(driverPerson);
				buildVehicleAndDriver(carrier, driverPerson, carrierVehicle);
			}
		}
	}

	/**
	 * Creates fright vehicles and drivers
	 */
	private void buildVehicleAndDriver(Carrier carrier, Person driverPerson, CarrierVehicle carrierVehicle) {
		if (!scenario.getVehicles().getVehicleTypes().containsKey(carrierVehicle.getType().getId()))
			scenario.getVehicles().addVehicleType(carrierVehicle.getType());
		Id<Vehicle> vid = Id.createVehicleId(driverPerson.getId());
		VehicleUtils.insertVehicleIdsIntoAttributes(driverPerson, Map.of(CarrierUtils.getCarrierMode(carrier), vid));
		scenario.getVehicles()
				.addVehicle(scenario.getVehicles().getFactory().createVehicle(vid, carrierVehicle.getType()));
		freightVehicles.add(vid);
		freightDrivers.add(driverPerson.getId());
	}

	/**
	 * Jsprit creates tours that begin with the opening time of the carrier. As a
	 * consequence, the tour vehicle arrives may arrive too early at the first customer.
	 * Moreover, we need to enforce that a service does not end too early (before the of it's TimeWindow),
	 * otherwise following services would start too early.
	 * This method adjusts the (service) activity endTimes and leg departure times to avoid too early starts
	 * of the services. The firsttourTraveltimeBuffer tries to ensure that the carrier
	 * departures early enough (leg travel time x firsttourTraveltimeBuffer) to
	 * reach his first destination (service).
	 */
	private void manageJspritDepartureTimes(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();

		for (int i = 0; i < planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);

			if (planElement instanceof Activity) {

				Activity currentActivity = (Activity) planElement;

				// Handle all regular services
				if(!currentActivity.getType().equals(FreightConstants.START))
				{

					// ExpectedArrivalTime from jsprit needs to be recalculated
					// Recalculation is based on next leg departure time and current act service time
					Leg prevLeg = (Leg) planElements.get(i-1);
					Leg nextLeg = PopulationUtils.getNextLeg(plan, currentActivity);

					Activity prevAct = (Activity) planElements.get(i - 2);

					if (!prevAct.getType().equals(FreightConstants.START)) {

						//End of plan is reached
						if (nextLeg == null) {
							// If next activity is the end activity, departure is allowed to start immediately
							prevAct.setEndTime(prevLeg.getDepartureTime().seconds());
						} else {
							// Update recent prevAct and prevLeg
							// Expected arrival time at current activity
							double expectedArrivalTime = nextLeg.getDepartureTime().seconds()
									- Double.parseDouble(Objects.requireNonNull(currentActivity.getAttributes().getAttribute(SERVICE_DURATION_NAME)).toString());

							// Set endTimes to avoid a too early departure after service
							// The earliestPrevActEndTime will include a waiting time,
							// for which the vehicle remains at previous activity
							double earliestPrevActEndTime = expectedArrivalTime - prevLeg.getTravelTime().seconds();
							prevAct.setEndTime(earliestPrevActEndTime);
							prevLeg.setDepartureTime(earliestPrevActEndTime);
						}

					}

				} else if (currentActivity.getType().equals(FreightConstants.START))
				{

					double travelTimeToFirstJob = ((Leg) planElements.get(i+1)).getTravelTime().seconds();
					double departureTimeAtFirstJob = ((Leg) planElements.get(i+3)).getDepartureTime().seconds();
					double jobServiceDuration = Double.parseDouble(Objects.requireNonNull(planElements.get(i + 2).getAttributes().getAttribute(SERVICE_DURATION_NAME)).toString());
					double initialLegDepartureTime = departureTimeAtFirstJob - jobServiceDuration - travelTimeToFirstJob * this.firstTourTraveltimeBuffer;
					double expectedArrivalTimeAtFirstJob = departureTimeAtFirstJob - travelTimeToFirstJob*this.firstTourTraveltimeBuffer;

					//Set optimal endTimes to avoid a too early arrival at first job
					 planElements.get(i+2).getAttributes().putAttribute(EXPECTED_ARRIVALTIME_NAME, expectedArrivalTimeAtFirstJob );
					currentActivity.setEndTime(initialLegDepartureTime);
					((Leg) planElements.get(i+1)).setDepartureTime(initialLegDepartureTime);

				}
			}
		}
	}

	/**
	 * Creates just a simple copy of the ScheduledTour (from JSprit) and forms a MATSim plan
	 */
	private Plan createPlainPlanFromTour(Carrier carrier, ScheduledTour scheduledTour) {

		String carrierMode = CarrierUtils.getCarrierMode(carrier);

		// Create empty plan
		Plan plan = PopulationUtils.createPlan();

		// Create start activity

		Activity startActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.START,
				scheduledTour.getVehicle().getLocation());
		plan.addActivity(startActivity);

		for (Tour.TourElement tourElement : scheduledTour.getTour().getTourElements()) {
			if (tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg) {

				// Take information from scheduled leg and create a defaultLeg
				Tour.Leg tourLeg = (Tour.Leg) tourElement;
				Leg defaultLeg = PopulationUtils.createLeg(carrierMode);

				Route route = tourLeg.getRoute();
				Assert.isTrue(tourLeg.getRoute() != null, "Missing route for carrier: " + carrier.getId().toString());

				// Assign information from tourLeg to defaultLeg
				defaultLeg.setDepartureTime(tourLeg.getExpectedDepartureTime());
				defaultLeg.setTravelTime(tourLeg.getExpectedTransportTime());

				if (drtModes.contains(carrierMode)) {
					defaultLeg.setRoute(null); // DrtRoute gets calculated later on
				} else {
					double routeDistance = RouteUtils.calcDistance((NetworkRoute) route, 1.0, 1.0, scenario.getNetwork());
					route.setDistance(routeDistance);
					route.setTravelTime(tourLeg.getExpectedTransportTime());
					defaultLeg.setRoute(route);
				}

				plan.addLeg(defaultLeg);

			} else if (tourElement instanceof Tour.TourActivity) {

				// Take information and create a defaultActivity
				Tour.ServiceActivity tourActivity = (Tour.ServiceActivity) tourElement;

				Double expectedArrival = tourActivity.getExpectedArrival();
				Double serviceDuration = tourActivity.getDuration();
				CarrierService service = carrier.getServices().get(tourActivity.getService().getId());
				String actType = COMMERCIALJOB_ACTIVITYTYPE_PREFIX + "_" + carrier.getId();
				String customer = (String) service.getAttributes().getAsMap().get(CUSTOMER_ATTRIBUTE_NAME);

				// Assign information to defaultActivity taken from tourActivity
				Activity defaultActivity = PopulationUtils.createActivityFromLinkId(actType,
						tourActivity.getLocation());
				defaultActivity.getAttributes().putAttribute(CUSTOMER_ATTRIBUTE_NAME, customer);
				defaultActivity.getAttributes().putAttribute(SERVICEID_ATTRIBUTE_NAME, service.getId().toString());

				// Is used later to adjust endTimes of activities
				defaultActivity.getAttributes().putAttribute(EXPECTED_ARRIVALTIME_NAME, expectedArrival);
				defaultActivity.getAttributes().putAttribute(SERVICE_DURATION_NAME, serviceDuration);

				plan.addActivity(defaultActivity);
			}
		}

		// Create end activity
		Activity endActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.END,
				scheduledTour.getVehicle().getLocation());
		plan.addActivity(endActivity);

		return plan;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		// Assignment between jobs and operators has changed due to innovation.
		// Recalculate jsprit tour planning, and build new fright agents
		if (enableTourPlanning) {
			removeFreightAgents();
			cleanCarriers();
			carriers.getCarriers().values().forEach(carrier -> carrier.getServices().clear());

			CommercialTrafficChecker.run(scenario.getPopulation(), carriers);
			generateIterationServices(carriers, scenario.getPopulation());
			try {
				buildTours();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
        createAndAddFreightAgents(this.carriers, this.scenario.getPopulation());

        event.getServices().getInjector().getInstance(ScoreCommercialJobs.class).prepareTourArrivalsForDay();
        String dir = event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/";
        log.info("writing carrier file of iteration " + event.getIteration() + " to " + dir);
        CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers);
        planWriter.write(dir + "carriers_it" + event.getIteration() + ".xml");
    }

    private void getDrtModes(Config config){
        MultiModeDrtConfigGroup drtCfgGroup = MultiModeDrtConfigGroup.get(config);
        if (drtCfgGroup != null){
            drtCfgGroup.getModalElements().forEach(cfg -> drtModes.add(cfg.getMode()));
        }
    }

	/**
	 * Generates the services (out of the person population) and assigns them to the carriers
	 */
	public void generateIterationServices(Carriers carriers, Population population) {

        Map<Person,Set<Activity>> customer2ActsWithJobs = new HashMap<>();

        population.getPersons().values().forEach(p ->
        {
            Set<Activity> activitiesWithServices = JointDemandUtils.getCustomerActivitiesExpectingJobs(p.getSelectedPlan());
            if(!activitiesWithServices.isEmpty()) customer2ActsWithJobs.put(p,activitiesWithServices);
        });
        for(Person customer : customer2ActsWithJobs.keySet()){
            for (Activity activity : customer2ActsWithJobs.get(customer)) {
                Map<String,Object> commercialJobAttributes = JointDemandUtils.getCommercialJobAttributes(activity);
                for (String commercialJobAttributeKey : commercialJobAttributes.keySet()) {
					List<String> commercialJobProperties = new ArrayList<>((Collection<? extends String>) commercialJobAttributes.get(commercialJobAttributeKey));

                    int jobIdx = Integer.parseInt(commercialJobAttributeKey.substring(JointDemandUtils.COMMERCIALJOB_ATTRIBUTE_NAME.length()));
                    Id<CarrierService> serviceId = createCarrierServiceIdXForCustomer(customer,jobIdx);

                    double earliestStart = Double.parseDouble(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_START_IDX));
                    double latestStart = Double.parseDouble(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_END_IDX));

                    CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(serviceId, PopulationUtils.decideOnLinkIdForActivity(activity,scenario));
                    serviceBuilder.setCapacityDemand(Integer.parseInt(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_AMOUNT_IDX)));
                    serviceBuilder.setServiceDuration(Double.parseDouble(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_DURATION_IDX)));
                    serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(earliestStart,latestStart));

                    Id<Carrier> carrierId = JointDemandUtils.getCurrentlySelectedCarrierForJob(activity, jobIdx);
                    if (carriers.getCarriers().containsKey(carrierId)) {
                        Carrier carrier = carriers.getCarriers().get(carrierId);
                        CarrierService service = serviceBuilder.build();
                        service.getAttributes().putAttribute(CUSTOMER_ATTRIBUTE_NAME, customer.getId().toString());
                        carrier.getServices().put(serviceId, service);
                    } else {
                        throw new RuntimeException("Carrier Id does not exist: " + carrierId.toString()
                                + ". There is a wrong reference in activity attribute of person " + customer);
                    }
                }
            }
        }

    }

    private void buildTours() throws InterruptedException, ExecutionException {
        TourPlanning.runTourPlanningForCarriers(carriers,scenario,vrpTransportCostsFactory.createVRPTransportCosts());
    }

    private static Id<CarrierService> createCarrierServiceIdXForCustomer(Person customer, int x) {
        return Id.create(customer.getId().toString() + "_" + x, CarrierService.class);
    }


    private Person createDriverPerson(Id<Person> driverId) {
        return PopulationUtils.getFactory().createPerson(driverId);
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
		removeFreightAgents();
        //clear carrier plans
		toggleChangeCommercialJobOperatorStrategy(event.getIteration());
    }


	/**
	 * Remove freight drivers and vehicles from scenario
	 */
	private void removeFreightAgents() {
		freightDrivers.forEach(d -> scenario.getPopulation().removePerson(d));
		freightVehicles.forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
	}

	/**
	 * Clean services, shipments and plans of carriers
	 */
	private void cleanCarriers(){
		CarrierVehicleTypes.getVehicleTypes(carriers).getVehicleTypes().keySet()
				.forEach(vehicleTypeId -> scenario.getVehicles().removeVehicleType(vehicleTypeId));
		carriers.getCarriers().values().forEach(carrier -> {
			carrier.getServices().clear();
			carrier.getShipments().clear();
			carrier.clearPlans();
		});
	}

	/**
	 * Enable or disable ChangeCommercialJobOperator strategy depending on current situation
	 */
	private void toggleChangeCommercialJobOperatorStrategy(int currentIteration) {

		// 0 means always tour planning
		if (this.changeOperatorInterval != 0) {

			this.enableTourPlanning = (currentIteration + 1.0) % this.changeOperatorInterval == 0.0;

			if (enableTourPlanning) {
				log.info("Toggle " + ChangeCommercialJobOperator.SELECTOR_NAME);
			}

			Collection<StrategySettings> allStrategies = this.scenario.getConfig().strategy().getStrategySettings();

			for (StrategySettings strategy : allStrategies) {
				if (strategy.getStrategyName().equals(ChangeCommercialJobOperator.SELECTOR_NAME)) {
					double initialWeight = strategy.getWeight();

					// Update weight
					double newWeight = enableTourPlanning ? initialWeight : 0.0;
					for (GenericPlanStrategy<Plan, Person> currentStrategy : this.strategyManager.getStrategies(null)) {
						String strategyName = currentStrategy.toString();
						if (isChangeCommercialJobOperatorStrategy(currentStrategy)) {
							log.info("Set weight for " + strategyName + " --> " + newWeight);
							this.strategyManager.changeWeightOfStrategy(currentStrategy, null, newWeight);
							break;
						}
					}
				}
			}
		}
	}

	private boolean isChangeCommercialJobOperatorStrategy(GenericPlanStrategy<Plan, Person> strategy) {
		String name = strategy.toString();
		return name.endsWith(ChangeCommercialJobOperator.SELECTOR_NAME);
	}

}
