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

package org.matsim.contrib.commercialTrafficApplications.jointDemand.commercialJob;

import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.freight.FreightConfigGroup;
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.contrib.commercialTrafficApplications.jointDemand.commercialJob.JointDemandUtils.*;

/**
 * Generates carriers and tours depending on next iteration's freight demand
 */
class CommercialJobGenerator implements BeforeMobsimListener, AfterMobsimListener {


    static final String COMMERCIALJOB_ACTIVITYTYPE_PREFIX = "commercialJob";
    static final String CUSTOMER_ATTRIBUTE_NAME = "customer";
    static final String SERVICEID_ATTRIBUTE_NAME = "serviceId";
	static final String EXPECTED_ARRIVALTIME_NAME = "eta";
	static final String SERVICE_DURATION_NAME = "duration";

    private final double firsttourTraveltimeBuffer;
    private final int timeSliceWidth;

    private Scenario scenario;
    private Population population;

    private Carriers carriers;

    private final TravelTime carTT;

    private Set<Id<Person>> freightDrivers = new HashSet<>();
    private Set<Id<Vehicle>> freightVehicles = new HashSet<>();

    private final static Logger log = Logger.getLogger(CommercialJobGenerator.class);

    private Set<String> drtModes = new HashSet<>();

    @Inject
    /* package */ CommercialJobGenerator( Scenario scenario, Map<String, TravelTime> travelTimes, Carriers carriers ) {
        JointDemandConfigGroup cfg = JointDemandConfigGroup.get(scenario.getConfig());
        this.carriers = carriers;
        this.firsttourTraveltimeBuffer = cfg.getFirstLegTraveltimeBufferFactor();
        this.timeSliceWidth = ConfigUtils.addOrGetModule(scenario.getConfig(), FreightConfigGroup.class).getTravelTimeSliceWidth();
        this.scenario = scenario;
        this.population = scenario.getPopulation();
        carTT = travelTimes.get(TransportMode.car);
        getDrtModes(scenario.getConfig());
    }

    /**
	 * Converts Jsprit tours to MATSim freight agents and adjusts departure times of
	 * leg and end times of activities
	 */
	private void buildFreightAgents() {

		for (Carrier carrier : carriers.getCarriers().values()) {
			int nextId = 0;

			for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
				CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
				Id<Person> driverId = JointDemandUtils.generateDriverId(carrier, carrierVehicle, nextId);
				nextId++;
				Person driverPerson = createDriverPerson(driverId);

				// Transform Jsprit output to MATSim freight agents
				Plan plainPlan = createPlainPlanFromTour(carrier, scheduledTour);
				
				// adjust Jsprit departure times and activity end times to avoid too early services
				manageJspritDepartureTimes(plainPlan);
				
				driverPerson.addPlan(plainPlan);
				plainPlan.setPerson(driverPerson);
				scenario.getPopulation().addPerson(driverPerson);
				builldVehicleAndDriver(carrier, driverPerson, carrierVehicle);
			}

		}

	}
	
	/**
	 * Creates fright vehicles and drivers
	 * @param carrier
	 * @param driverPerson
	 * @param carrierVehicle
	 */
	private void builldVehicleAndDriver(Carrier carrier, Person driverPerson, CarrierVehicle carrierVehicle) {
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
	 * 
	 * @param plan
	 */
	void manageJspritDepartureTimes(Plan plan) {
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
							Double expactedArrivalTime = nextLeg.getDepartureTime().seconds()
									- (double) currentActivity.getAttributes().getAttribute(SERVICE_DURATION_NAME);

							// Set endTimes to avoid an too early departure after service
							// The earliestPrevActEndTime will include a waiting time,
							// for which the vehicle remains at previous activity
							Double earliestPrevActEndTime = expactedArrivalTime - prevLeg.getTravelTime().seconds();
							prevAct.setEndTime(earliestPrevActEndTime);
							prevLeg.setDepartureTime(earliestPrevActEndTime);
						}

					}

				} else if (currentActivity.getType().equals(FreightConstants.START))
				{
					
					Double travelTimeToFirstJob = ((Leg) planElements.get(i+1)).getTravelTime().seconds();
					Double departureTimeAtFirstJob = ((Leg) planElements.get(i+3)).getDepartureTime().seconds();
					Double jobServiceDuration = (double) ((Activity) planElements.get(i+2)).getAttributes().getAttribute(SERVICE_DURATION_NAME);
					Double initalLegDepartureTime = departureTimeAtFirstJob - jobServiceDuration - travelTimeToFirstJob*this.firsttourTraveltimeBuffer;
					Double expactedArrivalTimeAtFirstJob = departureTimeAtFirstJob - travelTimeToFirstJob*this.firsttourTraveltimeBuffer;					
					
					//Set optimal endTimes to avoid an too early arrival at first job
					 ((Activity) planElements.get(i+2)).getAttributes().putAttribute(EXPECTED_ARRIVALTIME_NAME, expactedArrivalTimeAtFirstJob );
					currentActivity.setEndTime(initalLegDepartureTime);
					((Leg) planElements.get(i+1)).setDepartureTime(initalLegDepartureTime);
					
				}
			}
		}
	}

	/**
	 * Creates just a simple copy of the ScheduledTour (from JSprit) and forms a MATSim plan
	 * @param carrier
	 * @param scheduledTour
	 * @return
	 */
	Plan createPlainPlanFromTour(Carrier carrier, ScheduledTour scheduledTour) {

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

				Double expactedArrival = tourActivity.getExpectedArrival();
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
				defaultActivity.getAttributes().putAttribute(EXPECTED_ARRIVALTIME_NAME, expactedArrival);
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
        carriers.getCarriers().values().forEach(carrier -> carrier.getServices().clear());

        CommercialTrafficChecker.run(population,carriers);
        generateIterationServices();
        try {
			buildTours();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        buildFreightAgents();

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

    private void generateIterationServices() {

        Map<Person,Set<Activity>> customer2ActsWithJobs = new HashMap();

        population.getPersons().values().forEach(p ->
        {
            Set<Activity> activitiesWithServices = JointDemandUtils.getCustomerActivitiesExpectingJobs(p.getSelectedPlan());
            if(!activitiesWithServices.isEmpty()) customer2ActsWithJobs.put(p,activitiesWithServices);
        });
        for(Person customer : customer2ActsWithJobs.keySet()){
            for (Activity activity : customer2ActsWithJobs.get(customer)) {
                Map<String,Object> commercialJobAttributes = JointDemandUtils.getCommercialJobAttributes(activity);
                for (String commercialJobAttributeKey : commercialJobAttributes.keySet()) {
                    List<String> commercialJobProperties = new ArrayList<>();
                    commercialJobProperties.addAll((Collection<? extends String>) commercialJobAttributes.get(commercialJobAttributeKey));

                    int jobIdx = Integer.parseInt(commercialJobAttributeKey.substring(JointDemandUtils.COMMERCIALJOB_ATTRIBUTE_NAME.length()));
                    Id<CarrierService> serviceId = createCarrierServiceIdXForCustomer(customer,jobIdx);

                    double earliestStart = Double.parseDouble(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_START_IDX));
                    double latestStart = Double.parseDouble(commercialJobProperties.get(COMMERCIALJOB_ATTRIBUTE_END_IDX));

                    CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(serviceId, activity.getLinkId());
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
        TourPlanning.runTourPlanningForCarriers(carriers,scenario, timeSliceWidth,carTT );
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
    }

    private void removeFreightAgents() {
        freightDrivers.forEach(d -> scenario.getPopulation().removePerson(d));
        freightVehicles.forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
        CarrierVehicleTypes.getVehicleTypes(carriers).getVehicleTypes().keySet().forEach(vehicleTypeId -> scenario.getVehicles().removeVehicleType(vehicleTypeId));
        carriers.getCarriers().values().forEach(carrier -> {
            carrier.getServices().clear();
            carrier.getShipments().clear();
            carrier.clearPlans();
        });
    }

    @Deprecated
    private void createFreightAgents() {
        for (Carrier carrier : carriers.getCarriers().values()) {
            int nextId = 0;
            String modeForCarrier = CarrierUtils.getCarrierMode( carrier ) ;
            for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {

                CarrierVehicle carrierVehicle = scheduledTour.getVehicle();

                Id<Person> driverId = JointDemandUtils.generateDriverId(carrier, carrierVehicle, nextId);
                nextId++;

                Person driverPerson = createDriverPerson(driverId);
                Plan plan = PopulationUtils.createPlan();
                Activity startActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.START, scheduledTour.getVehicle().getLocation());
                plan.addActivity(startActivity);
                Activity lastTourElementActivity = null;
                Leg lastTourLeg = null;


                for (Tour.TourElement tourElement : scheduledTour.getTour().getTourElements()) {
                    if (tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg) {


                        org.matsim.contrib.freight.carrier.Tour.Leg tourLeg = (org.matsim.contrib.freight.carrier.Tour.Leg) tourElement;
                        Route route = tourLeg.getRoute();
                        if(route == null) throw new IllegalStateException("missing route for carrier " + carrier.getId());
                        Leg leg = PopulationUtils.createLeg(modeForCarrier);
                        if(drtModes.contains(modeForCarrier)){
                            leg.setRoute(null); //let the DrtRoute be calculated later
                        } else{
                            double routeDistance = RouteUtils.calcDistance((NetworkRoute) route, 1.0, 1.0, scenario.getNetwork());
                            route.setDistance(routeDistance);
                            route.setTravelTime(tourLeg.getExpectedTransportTime());
                        }

                        leg.setDepartureTime(tourLeg.getExpectedDepartureTime());
                        leg.setTravelTime(tourLeg.getExpectedTransportTime());
						leg.setTravelTime(tourLeg.getExpectedDepartureTime() + tourLeg.getExpectedTransportTime() - leg.getDepartureTime()
								.seconds());
                        plan.addLeg(leg);
                        if (lastTourElementActivity != null) {
                            lastTourElementActivity.setEndTime(tourLeg.getExpectedDepartureTime());
                            if (startActivity.getEndTime().isUndefined()) {
								startActivity.setEndTime(lastTourElementActivity.getEndTime().seconds()
										- lastTourElementActivity.getMaximumDuration().seconds()
                                        - lastTourLeg.getTravelTime().seconds() * firsttourTraveltimeBuffer);
                                lastTourElementActivity.setMaximumDurationUndefined();
                            }
                        }
                        lastTourLeg = leg;

                    } else if (tourElement instanceof Tour.TourActivity) {
                        if(! (tourElement instanceof  Tour.ServiceActivity)) throw new RuntimeException("currently only services (not shipments) are supported!");

                        Tour.ServiceActivity act = (Tour.ServiceActivity) tourElement;

                        CarrierService service = carrier.getServices().get(act.getService().getId()); //for some reason, the serviceAct only has a copy of the CarrierService object and this copy does not have the attributes..
                        String actType = COMMERCIALJOB_ACTIVITYTYPE_PREFIX + "_" + carrier.getId();
                        String customer = (String) service.getAttributes().getAsMap().get(CUSTOMER_ATTRIBUTE_NAME);


                        //here we would pass over the service Activity type containing the customer id...
                        Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(actType, act.getLocation());
                        tourElementActivity.getAttributes().putAttribute(CUSTOMER_ATTRIBUTE_NAME, customer);
                        tourElementActivity.getAttributes().putAttribute(SERVICEID_ATTRIBUTE_NAME, service.getId().toString());

                        plan.addActivity(tourElementActivity);
                        if (lastTourElementActivity == null) {
                            tourElementActivity.setMaximumDuration(act.getDuration());
                        }

                        lastTourElementActivity = tourElementActivity;

                    }
                }
                Activity endActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.END, scheduledTour.getVehicle().getLocation());
                plan.addActivity(endActivity);
                driverPerson.addPlan(plan);
                plan.setPerson(driverPerson);

                scenario.getPopulation().addPerson(driverPerson);
                if (!scenario.getVehicles().getVehicleTypes().containsKey(carrierVehicle.getType().getId()))
                    scenario.getVehicles().addVehicleType(carrierVehicle.getType());
                Id<Vehicle> vid = Id.createVehicleId(driverPerson.getId());
                VehicleUtils.insertVehicleIdsIntoAttributes(driverPerson, Map.of(CarrierUtils.getCarrierMode(carrier), vid));
                scenario.getVehicles().addVehicle(scenario.getVehicles().getFactory().createVehicle(vid, carrierVehicle.getType()));
                freightVehicles.add(vid);
                freightDrivers.add(driverPerson.getId());
            }
        }
    }

}
