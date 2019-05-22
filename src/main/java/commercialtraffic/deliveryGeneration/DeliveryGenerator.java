/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic.deliveryGeneration;/*
 * created by jbischoff, 11.04.2019
 */

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.termination.VariationCoefficientTermination;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import commercialtraffic.integration.CommercialTrafficConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
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

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates carriers and tours depending on next iteration's freight demand
 */
public class DeliveryGenerator implements BeforeMobsimListener, AfterMobsimListener {



    public final double firsttourTraveltimeBuffer;
    private final int maxIterations;

    @Inject
    private Scenario scenario;

    private Population population;

    private Carriers carriers;
    private final TravelTime carTT;

    private Set<Id<Person>> freightDrivers = new HashSet<>();
    private Set<Id<Vehicle>> freightVehicles = new HashSet<>();


    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        generateIterationServices();
        buildTours();
        createFreightAgents();
    }

    @Inject
    public DeliveryGenerator(Scenario scenario, Map<String, TravelTime> travelTimes) {
        CommercialTrafficConfigGroup ctcg = CommercialTrafficConfigGroup.get(scenario.getConfig());
        this.carriers = new Carriers();
        firsttourTraveltimeBuffer = ctcg.getFirstLegTraveltimeBufferFactor();
        new CarrierPlanXmlReaderV2(carriers).readFile(ctcg.getCarriersFileUrl(scenario.getConfig().getContext()).getFile());
        CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(vehicleTypes).readFile(ctcg.getCarriersVehicleTypesFileUrl(scenario.getConfig().getContext()).getFile());
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
        this.scenario = scenario;
        this.population = scenario.getPopulation();
        maxIterations = ctcg.getJspritIterations();
        carTT = travelTimes.get(TransportMode.car);

    }

    /**
     * Test only
     */
    DeliveryGenerator(Scenario scenario, Carriers carriers) {
        this.population = scenario.getPopulation();
        this.carriers = carriers;
        this.scenario = scenario;
        firsttourTraveltimeBuffer = 2;
        maxIterations = 100;
        carTT = new FreeSpeedTravelTime();
    }

    private void generateIterationServices() {
        carriers.getCarriers().values().forEach(carrier -> carrier.getServices().clear());
        Set<PlanElement> activitiesWithServcies = new HashSet<>();
        population.getPersons().values().forEach(p ->
        {
            activitiesWithServcies.addAll(p.getSelectedPlan().getPlanElements().stream().filter(Activity.class::isInstance).filter(a -> a.getAttributes().getAsMap().containsKey(PersonDelivery.DELIEVERY_TYPE)).collect(Collectors.toSet()));
        });
        int i = 0;
        for (PlanElement pe : activitiesWithServcies) {
            Activity activity = (Activity) pe;
            CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(i, CarrierService.class), activity.getLinkId());
            serviceBuilder.setCapacityDemand((int) activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_SIZE));
            serviceBuilder.setServiceDuration((int) activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_DURATION));
            serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance((int) activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_TIME_START), (int) activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_TIME_END)));
            i++;
            Id<Carrier> carrierId = PersonDelivery.getCarrierId(activity);
            if (carriers.getCarriers().containsKey(carrierId)) {
                Carrier carrier = carriers.getCarriers().get(carrierId);
                carrier.getServices().add(serviceBuilder.build());

            } else {
                throw new RuntimeException("Carrier Id does not exist: " + carrierId.toString());
            }


        }
    }

    private void buildTours() {
        Set<CarrierVehicleType> vehicleTypes = new HashSet<>();
        carriers.getCarriers().values().forEach(carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));
        NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes);
        netBuilder.setTimeSliceWidth(900); // !!!! otherwise it will not do anything.
        netBuilder.setTravelTime(carTT);
        final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

        for (Carrier carrier : carriers.getCarriers().values()) {
            //Build VRP
            VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
            vrpBuilder.setRoutingCost(netBasedCosts);
            VehicleRoutingProblem problem = vrpBuilder.build();

            // get the algorithm out-of-the-box, search solution and get the best one.
            VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

            algorithm.setMaxIterations(maxIterations);

            // variationCoefficient = stdDeviation/mean. so i set the threshold rather soft
            algorithm.addTerminationCriterion(new VariationCoefficientTermination(50, 0.01));


            Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
            VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

            //get the CarrierPlan
            CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, bestSolution);
            NetworkRouter.routePlan(carrierPlan, netBasedCosts);

            carrier.setSelectedPlan(carrierPlan);

        }


        new CarrierPlanXmlWriterV2(carriers).write("test-carriers.xml");
    }

    private void createFreightAgents() {
        for (Carrier carrier : carriers.getCarriers().values()) {
            int nextId = 0;
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
                        route.setDistance(RouteUtils.calcDistance((NetworkRoute) route, 1.0, 1.0, scenario.getNetwork()));
                        if (route == null)
                            throw new IllegalStateException("missing route for carrier " + carrier.getId());
                        route.setTravelTime(tourLeg.getExpectedTransportTime());
                        Leg leg = PopulationUtils.createLeg(TransportMode.car);
                        leg.setRoute(route);
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
                        Tour.TourActivity act = (Tour.TourActivity) tourElement;
                        Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(act.getActivityType(), act.getLocation());
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
                if (!scenario.getVehicles().getVehicleTypes().containsKey(carrierVehicle.getVehicleTypeId())) {
                    scenario.getVehicles().addVehicleType(carrierVehicle.getVehicleType());
                }
                Id<Vehicle> vid = Id.createVehicleId(driverPerson.getId());
                scenario.getVehicles().addVehicle(scenario.getVehicles().getFactory().createVehicle(vid, carrierVehicle.getVehicleType()));
                ;
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
    }
}
