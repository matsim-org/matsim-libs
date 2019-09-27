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

package commercialtraffic.commercialJob;

import commercialtraffic.integration.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * Generates carriers and tours depending on next iteration's freight demand
 */
public class DeliveryGenerator implements BeforeMobsimListener, AfterMobsimListener {



    private final double firsttourTraveltimeBuffer;
    private final CarrierJSpritIterations iterationsPerCarrier;
    private final CarrierMode carrierMode;
    private final int timeSliceWidth;

    private Scenario scenario;
    private Population population;

    private Carriers carriers;

    private final TravelTime carTT;

    private Set<Id<Person>> freightDrivers = new HashSet<>();
    private Set<Id<Vehicle>> freightVehicles = new HashSet<>();

    private final static Logger log = Logger.getLogger(DeliveryGenerator.class);

    private Set<String> drtModes = new HashSet<>();

    @Inject
    public DeliveryGenerator(Scenario scenario, Map<String, TravelTime> travelTimes, Carriers carriers, CarrierMode carrierMode, CarrierJSpritIterations iterationsPerCarrier) {
        CommercialTrafficConfigGroup ctcg = CommercialTrafficConfigGroup.get(scenario.getConfig());
        this.carriers = carriers;
        this.firsttourTraveltimeBuffer = ctcg.getFirstLegTraveltimeBufferFactor();
        this.timeSliceWidth = ctcg.getJspritTimeSliceWidth();
        this.carrierMode = carrierMode;
        this.iterationsPerCarrier = iterationsPerCarrier;
        this.scenario = scenario;
        this.population = scenario.getPopulation();
        carTT = travelTimes.get(TransportMode.car);
        if (CommercialTrafficChecker.hasMissingAttributes(population)) {
            throw new RuntimeException("Not all agents expecting deliveries contain all required attributes fo receival. Please check the log for DeliveryConsistencyChecker. Aborting.");
        }
        getDrtModes(scenario.getConfig());
    }

    /**
     * Test only
     */
    DeliveryGenerator(Scenario scenario, Carriers carriers, CarrierJSpritIterations iterationsPerCarrier) {
        this.population = scenario.getPopulation();
        this.carriers = carriers;
        this.scenario = scenario;
        this.firsttourTraveltimeBuffer = 2;
        this.timeSliceWidth = 1800;
        carrierMode = (m -> TransportMode.car);
        carTT = new FreeSpeedTravelTime();
        this.iterationsPerCarrier = iterationsPerCarrier;
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        generateIterationServices();
        buildTours();
        createFreightAgents();

        String dir = event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/";
        log.info("writing carrier file of iteration " + event.getIteration() + " to " + dir);
        CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers.getCarriers().values());
        planWriter.write(dir + "carriers_it" + event.getIteration() + ".xml");
    }

    private void getDrtModes(Config config){
        MultiModeDrtConfigGroup drtCfgGroup = MultiModeDrtConfigGroup.get(config);
        if (drtCfgGroup != null){
            drtCfgGroup.getModalElements().forEach(cfg -> drtModes.add(cfg.getMode()));
        }
    }

    private void generateIterationServices() {
        carriers.getCarriers().values().forEach(carrier -> carrier.getServices().clear());

        Map<Person,Set<Activity>> customer2ActsWithJobs = new HashMap();

        population.getPersons().values().forEach(p ->
        {
            Set<Activity> activitiesWithServices = CommercialJobUtils.getActivitiesWithJobs(p.getSelectedPlan());
            if(!activitiesWithServices.isEmpty()) customer2ActsWithJobs.put(p,activitiesWithServices);
        });
        for(Person customer : customer2ActsWithJobs.keySet()){
            for (Activity activity : customer2ActsWithJobs.get(customer)) {
                Map<String,Object> commercialJobAttributes = CommercialJobUtils.getCommercialJobAttributes(activity);
                for (String commercialJobAttributeKey : commercialJobAttributes.keySet()) {
                    String[] commercialJobProperties = String.valueOf(commercialJobAttributes.get(commercialJobAttributeKey)).split(CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_DELIMITER);

                    int jobIdx = Integer.valueOf(commercialJobAttributeKey.substring(CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_NAME.length()));
                    Id<CarrierService> serviceId = createCarrierServiceIdXForCustomer(customer,jobIdx);

                    double earliestStart = Double.valueOf(commercialJobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_START_IDX]);
                    double latestStart = Double.valueOf(commercialJobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_END_IDX]);

                    CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(serviceId, activity.getLinkId());
                    serviceBuilder.setCapacityDemand(Integer.valueOf(commercialJobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_AMOUNT_IDX]));
                    serviceBuilder.setServiceDuration(Double.valueOf(commercialJobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_DURATION_IDX]));
                    serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(earliestStart,latestStart));

                    Id<Carrier> carrierId = CommercialJobUtils.getCurrentCarrierForJob(activity,jobIdx);
                    if (carriers.getCarriers().containsKey(carrierId)) {
                        Carrier carrier = carriers.getCarriers().get(carrierId);
                        carrier.getServices().add(serviceBuilder.build());
                    } else {
                        throw new RuntimeException("Carrier Id does not exist: " + carrierId.toString());
                    }
                }
            }
        }

    }

    private static  Id<CarrierService> createCarrierServiceIdXForCustomer(Person customer, int x){
        return Id.create(customer.getId().toString() + CommercialJobUtils.CARRIERSPLIT + x, CarrierService.class);
    }

    private static String createDeliveryActTypeFromServiceId(Id<CarrierService> id) {
        String idStr = id.toString();
        return FreightConstants.DELIVERY + CommercialJobUtils.CARRIERSPLIT + idStr.substring(0,idStr.lastIndexOf(CommercialJobUtils.CARRIERSPLIT));
    }

    public static Id<Person> getCustomerIdFromDeliveryActivityType(String actType){
        return Id.createPersonId(actType.substring(actType.indexOf(CommercialJobUtils.CARRIERSPLIT)+1,actType.length()));
    }

    private void buildTours() {
        TourPlanning.runTourPlanningForCarriers(carriers,scenario,iterationsPerCarrier,timeSliceWidth,carTT);
    }

    private void createFreightAgents() {
        for (Carrier carrier : carriers.getCarriers().values()) {
            int nextId = 0;
            String modeForCarrier = carrierMode.getCarrierMode(carrier.getId());
            for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {

                CarrierVehicle carrierVehicle = scheduledTour.getVehicle();

                Id<Person> driverId = Id.createPersonId("freight_" + carrier.getId() + "_veh_" + carrierVehicle.getVehicleId() + "_" + nextId);
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
                        leg.setTravelTime(tourLeg.getExpectedDepartureTime() + tourLeg.getExpectedTransportTime() - leg.getDepartureTime());
                        plan.addLeg(leg);
                        if (lastTourElementActivity != null) {
                            lastTourElementActivity.setEndTime(tourLeg.getExpectedDepartureTime());
                            if (Time.isUndefinedTime(startActivity.getEndTime())) {
                                startActivity.setEndTime(lastTourElementActivity.getEndTime() - lastTourElementActivity.getMaximumDuration() - lastTourLeg.getTravelTime() * firsttourTraveltimeBuffer);
                                lastTourElementActivity.setMaximumDuration(Time.getUndefinedTime());
                            }
                        }
                        lastTourLeg = leg;

                    } else if (tourElement instanceof Tour.TourActivity) {
                        if(! (tourElement instanceof  Tour.ServiceActivity)) throw new RuntimeException("currently only services (not shipments) are supported!");

                        Tour.ServiceActivity act = (Tour.ServiceActivity) tourElement;

                        String actType = createDeliveryActTypeFromServiceId(act.getService().getId());

                        //here we would pass over the service Activity type containing the customer id...
                        Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(actType, act.getLocation());
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
                try {
                    scenario.getVehicles().addVehicleType(carrierVehicle.getVehicleType());
                } catch (IllegalArgumentException e) {
                }
                Id<Vehicle> vid = Id.createVehicleId(driverPerson.getId());
                VehicleUtils.insertVehicleIdIntoAttributes(driverPerson,carrierMode.getCarrierMode(carrier.getId()),vid);
                scenario.getVehicles().addVehicle(scenario.getVehicles().getFactory().createVehicle(vid, carrierVehicle.getVehicleType()));
                freightVehicles.add(vid);
                freightDrivers.add(driverPerson.getId());
            }
        }
    }

    private Person createDriverPerson(Id<Person> driverId) {
        final Id<Person> id = driverId;
        Person person = PopulationUtils.getFactory().createPerson(id);
        return person;
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
}
