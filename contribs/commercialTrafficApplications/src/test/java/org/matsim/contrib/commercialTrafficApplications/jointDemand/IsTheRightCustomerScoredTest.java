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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class IsTheRightCustomerScoredTest {

    private static final int MAX_JOB_SCORE = 6;

    Scenario scenario;

    @Before
    public void setUp() {

        Config config = ConfigUtils.loadConfig("./scenarios/grid/jointDemand_config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        JointDemandConfigGroup jointDemandConfigGroup = ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        jointDemandConfigGroup.setMaxJobScore(MAX_JOB_SCORE);
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile("jointDemand_carriers_car.xml");
        freightConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
        scenario = ScenarioUtils.loadScenario(config);
        FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

        //limit the fleet size of carrier pizza_1 so that it can handly only one order/job
        FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("salamiPizza", Carrier.class)).getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

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
    public void testIfTheRightPersonIsScoredForReceivingAJob() {
        Plan partyPizzaPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("customerOrderingForParty")).getSelectedPlan();
        Plan lonelyPizzaPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("customerOrderingJustForItself")).getSelectedPlan();
        Plan nonCustomerPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("nonCustomer")).getSelectedPlan();

        //derive the service activity from the carrier plan and compare the service id (which should contain the customer id) with the person id of the expected customer
        Carrier pizzaCarrier = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("salamiPizza", Carrier.class));
        ScheduledTour tour = (ScheduledTour) pizzaCarrier.getSelectedPlan().getScheduledTours().toArray()[0];
        Id<CarrierService> serviceActivity = tour.getTour().getTourElements().stream()
                .filter(tourElement -> tourElement instanceof Tour.ServiceActivity)
                .map(tourElement -> ((Tour.ServiceActivity) tourElement).getService())
                .map(carrierService -> carrierService.getId())
                .findFirst().orElseThrow(() -> new RuntimeException("no service activity found in scheduledTours"));

        Assert.assertEquals("the person that is delivered pizza should be customerOrderingForParty", partyPizzaPlan.getPerson().getId().toString(), serviceActivity.toString().split("_")[0]);

        //compare scores
        Assert.assertTrue("the plan of the customer receiving a job should get a higher score than the plan of the non customer ", partyPizzaPlan.getScore() > nonCustomerPlan.getScore());
        Assert.assertTrue("the plan of the customer receiving a job should get a higher score than the plan of the customer not receiving one ", partyPizzaPlan.getScore() > lonelyPizzaPlan.getScore());
        Assert.assertTrue("the difference of receiving a job in time and not receiving it at all should be the maxJobScore=" + MAX_JOB_SCORE + " as job is performed in time", partyPizzaPlan.getScore() - lonelyPizzaPlan.getScore() == MAX_JOB_SCORE);
        Assert.assertEquals("not receiving a job at all should be scored with zero", lonelyPizzaPlan.getScore(), 0.0, MatsimTestUtils.EPSILON);
        Assert.assertTrue(lonelyPizzaPlan.getScore() - nonCustomerPlan.getScore() == 0);
    }


}
