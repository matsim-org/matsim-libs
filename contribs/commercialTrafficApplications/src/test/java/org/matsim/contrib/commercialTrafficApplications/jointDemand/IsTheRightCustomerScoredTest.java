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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.freight.carriers.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class IsTheRightCustomerScoredTest {

    private static final int MAX_JOB_SCORE = 6;

    Scenario scenario;

    @BeforeEach
    public void setUp() {

        Config config = ConfigUtils.loadConfig("./scenarios/grid/jointDemand_config.xml");
        config.controller().setLastIteration(0);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        JointDemandConfigGroup jointDemandConfigGroup = ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        jointDemandConfigGroup.setMaxJobScore(MAX_JOB_SCORE);
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setCarriersFile("jointDemand_carriers_car.xml");
        freightCarriersConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
        scenario = ScenarioUtils.loadScenario(config);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

        //limit the fleet size of carrier pizza_1 so that it can handly only one order/job
        CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("salamiPizza", Carrier.class)).getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

        preparePopulation(scenario);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule());
        controler.run();
    }

    private void preparePopulation(Scenario scenario) {
        scenario.getPopulation().getPersons().clear();

        PopulationFactory factory = scenario.getPopulation().getFactory();
        Person customerOrderingForParty = factory.createPerson(Id.createPersonId("customerOrderingForParty"));
        Person customerOrderingJustForItself = factory.createPerson(Id.createPersonId("customerOrderingJustForItself"));
        Person nonCustomer = factory.createPerson(Id.createPersonId("nonCustomer"));

        Plan planNonCustomer = factory.createPlan();
        Activity homeStarving = factory.createActivityFromLinkId("homeStarving", Id.createLinkId("116"));
        planNonCustomer.addActivity(homeStarving);
        nonCustomer.addPlan(planNonCustomer);
        scenario.getPopulation().addPerson(nonCustomer);

        Id<Carrier> pizzaCarrier = Id.create("salamiPizza", Carrier.class);
        Plan planCustomerOrderingForParty = factory.createPlan();
        Activity pizzaParty = factory.createActivityFromLinkId("pizzaAlone", Id.createLinkId("116"));
        JointDemandUtils.addCustomerCommercialJobAttribute(pizzaParty, pizzaCarrier, 6,
                6 * 3600, 7 * 3600, 180);
        planCustomerOrderingForParty.addActivity(pizzaParty);
        customerOrderingForParty.addPlan(planCustomerOrderingForParty);
        scenario.getPopulation().addPerson(customerOrderingForParty);

        Plan planCustomerOrderingJustForItself = factory.createPlan();
        Activity pizzaAlone = factory.createActivityFromLinkId("pizzaAlone", Id.createLinkId("116"));
        JointDemandUtils.addCustomerCommercialJobAttribute(pizzaAlone, pizzaCarrier, 2,
                6 * 3600, 7 * 3600, 180);
        planCustomerOrderingJustForItself.addActivity(pizzaAlone);
        customerOrderingJustForItself.addPlan(planCustomerOrderingJustForItself);
        scenario.getPopulation().addPerson(customerOrderingJustForItself);
    }


	@Test
	void testIfTheRightPersonIsScoredForReceivingAJob() {
        Plan partyPizzaPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("customerOrderingForParty")).getSelectedPlan();
        Plan lonelyPizzaPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("customerOrderingJustForItself")).getSelectedPlan();
        Plan nonCustomerPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("nonCustomer")).getSelectedPlan();

        //derive the service activity from the carrier plan and compare the service id (which should contain the customer id) with the person id of the expected customer
        Carrier pizzaCarrier = CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create("salamiPizza", Carrier.class));
        ScheduledTour tour = (ScheduledTour) pizzaCarrier.getSelectedPlan().getScheduledTours().toArray()[0];
        Id<CarrierService> serviceActivity = tour.getTour().getTourElements().stream()
                .filter(tourElement -> tourElement instanceof Tour.ServiceActivity)
                .map(tourElement -> ((Tour.ServiceActivity) tourElement).getService())
                .map(carrierService -> carrierService.getId())
                .findFirst().orElseThrow(() -> new RuntimeException("no service activity found in scheduledTours"));

        Assertions.assertEquals(partyPizzaPlan.getPerson().getId().toString(), serviceActivity.toString().split("_")[0], "the person that is delivered pizza should be customerOrderingForParty");

        //compare scores
        Assertions.assertTrue(partyPizzaPlan.getScore() > nonCustomerPlan.getScore(), "the plan of the customer receiving a job should get a higher score than the plan of the non customer ");
        Assertions.assertTrue(partyPizzaPlan.getScore() > lonelyPizzaPlan.getScore(), "the plan of the customer receiving a job should get a higher score than the plan of the customer not receiving one ");
        Assertions.assertTrue(partyPizzaPlan.getScore() - lonelyPizzaPlan.getScore() == MAX_JOB_SCORE, "the difference of receiving a job in time and not receiving it at all should be the maxJobScore=" + MAX_JOB_SCORE + " as job is performed in time");
        Assertions.assertEquals(lonelyPizzaPlan.getScore(), 0.0, MatsimTestUtils.EPSILON, "not receiving a job at all should be scored with zero");
        Assertions.assertTrue(lonelyPizzaPlan.getScore() - nonCustomerPlan.getScore() == 0);
    }


}
