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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;

import static org.matsim.core.config.ConfigUtils.loadConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class TestScenarioGeneration {


    public static Scenario generateScenario(){

        Config config = loadConfig("./scenarios/grid/jointDemand_config.xml",new JointDemandConfigGroup());
        Scenario scenario = createScenario(config);
        addPopulation(scenario);
        return scenario;
    }


    public static Carriers generateCarriers() {
        Carriers carriers = new Carriers();

        Carrier italianPizzaPlace = CarrierImpl.newInstance(Id.create("pizza_italian", Carrier.class));
        CarriersUtils.setCarrierMode(italianPizzaPlace, TransportMode.car);
        CarriersUtils.setJspritIterations(italianPizzaPlace, 20);
        italianPizzaPlace.getAttributes().putAttribute(JointDemandUtils.CARRIER_MARKET_ATTRIBUTE_NAME, "pizza");

        Carrier americanPizzaPlace = CarrierImpl.newInstance(Id.create("pizza_american", Carrier.class));
        CarriersUtils.setCarrierMode(americanPizzaPlace, TransportMode.car);
        CarriersUtils.setJspritIterations(americanPizzaPlace, 20);
        americanPizzaPlace.getAttributes().putAttribute(JointDemandUtils.CARRIER_MARKET_ATTRIBUTE_NAME, "pizza");

        Carrier shopping_1 = CarrierImpl.newInstance(Id.create("shopping_1", Carrier.class));
        CarriersUtils.setCarrierMode(shopping_1, TransportMode.car);
        CarriersUtils.setJspritIterations(shopping_1, 20);
        shopping_1.getAttributes().putAttribute(JointDemandUtils.CARRIER_MARKET_ATTRIBUTE_NAME, "shopping");

        VehicleType type = createLightType();

        italianPizzaPlace.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        italianPizzaPlace.getCarrierCapabilities().getVehicleTypes().add(createLightType());
        CarrierVehicle v = getLightVehicle(italianPizzaPlace.getId(), type, Id.createLinkId(111), "one");
        italianPizzaPlace.getCarrierCapabilities().getCarrierVehicles().put(v.getId(), v);

        americanPizzaPlace.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        v = getLightVehicle(americanPizzaPlace.getId(), type, Id.createLinkId(111), "one");
        americanPizzaPlace.getCarrierCapabilities().getCarrierVehicles().put(v.getId(), v);
        americanPizzaPlace.getCarrierCapabilities().getVehicleTypes().add(createLightType());

        shopping_1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        v = getLightVehicle(shopping_1.getId(), type, Id.createLinkId(111), "one");
        shopping_1.getCarrierCapabilities().getCarrierVehicles().put(v.getId(), v);
        shopping_1.getCarrierCapabilities().getVehicleTypes().add(createLightType());

        carriers.addCarrier(italianPizzaPlace);
        carriers.addCarrier(americanPizzaPlace);
        carriers.addCarrier(shopping_1);

        return carriers;
    }


    private static void addPopulation(Scenario scenario) {
        Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(1));
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        p.addPlan(plan);

        Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home.setLinkId(Id.createLinkId(116));
        home.setEndTime(8 * 3600);

        plan.addActivity(home);
        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

        Activity work = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        work.setLinkId(Id.createLinkId(259));
        work.setEndTime(16 * 3600);

        work.getAttributes().putAttribute(JointDemandUtils.COMMERCIALJOB_ATTRIBUTE_NAME + "1", List.of("pizza_italian","1",String.valueOf(12 * 3600), String.valueOf(13 * 3600),"180"));
        plan.addActivity(work);
        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));


        Activity home2 = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home2.setLinkId(Id.createLinkId(116));
        plan.addActivity(home2);
        scenario.getPopulation().addPerson(p);

    }


    private static CarrierVehicle getLightVehicle(Id<?> id, VehicleType type, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create((id.toString() + "_lightVehicle_" + depot), Vehicle.class), homeId, type );
        vBuilder.setEarliestStart(6 * 60 * 60);
        vBuilder.setLatestEnd(16 * 60 * 60);
        return vBuilder.build();
    }

    private static VehicleType createLightType() {
        VehicleType type = VehicleUtils.createVehicleType(Id.create("small", VehicleType.class));
        type.getCapacity().setOther(6);
        type.getCostInformation().setFixedCost(80.0);
        type.getCostInformation().setCostsPerMeter(0.00047);
        type.getCostInformation().setCostsPerSecond(0.008);
        return type;
    }


}
