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

package commercialtraffic.jobGeneration;

import commercialtraffic.integration.CarrierMode;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class FreightAgentInserter {

    private final CarrierMode carrierMode;
    private final Scenario scenario;

    private Set<Id<Person>> freightDrivers = new HashSet<>();

    @Inject
    public FreightAgentInserter(Scenario scenario, CarrierMode carrierMode) {
        this.scenario = scenario;
        this.carrierMode = carrierMode;
    }

    public FreightAgentInserter(Scenario scenario) {
        this.carrierMode = carrierId -> TransportMode.car;
        this.scenario = scenario;
    }

    private static String createServiceActTypeFromServiceId(Id<CarrierService> id) {
        return FreightConstants.DELIVERY + CommercialJobUtils.CARRIERSPLIT + id;
    }

    private Set<Id<Vehicle>> freightVehicles = new HashSet<>();


    public static Id<CarrierService> getServiceIdFromActivityType(String actType){
        return Id.create(actType.substring(actType.indexOf(CommercialJobUtils.CARRIERSPLIT)+1),CarrierService.class);
    }

    void createFreightAgents(Carriers carriersWithTours, double firsttourTraveltimeBuffer) {
        for (Carrier carrier : carriersWithTours.getCarriers().values()) {
            int nextId = 0;
            for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {

                CarrierVehicle carrierVehicle = scheduledTour.getVehicle();

                Id<Person> driverId = CommercialJobUtils.createDriverId(carrier, nextId, carrierVehicle);
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
                        Leg leg = PopulationUtils.createLeg(carrierMode.getCarrierMode(carrier.getId()));
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
                        if(! (tourElement instanceof  Tour.ServiceActivity)) throw new RuntimeException("currently only services (not shipments) are supported!");

                        Tour.ServiceActivity act = (Tour.ServiceActivity) tourElement;

                        String actType = createServiceActTypeFromServiceId(act.getService().getId());

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

    void removeFreightAgents(Carriers carriers) {
        freightDrivers.forEach(d -> scenario.getPopulation().removePerson(d));
        freightVehicles.forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
        CarrierVehicleTypes.getVehicleTypes(carriers).getVehicleTypes().keySet().forEach(vehicleTypeId -> scenario.getVehicles().removeVehicleType(vehicleTypeId));

    }

}
