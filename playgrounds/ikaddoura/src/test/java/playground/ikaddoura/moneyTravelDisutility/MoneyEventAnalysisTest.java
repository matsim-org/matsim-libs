/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.moneyTravelDisutility;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */
public class MoneyEventAnalysisTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		final Id<Person> personId2 = Id.createPersonId("person2");
		final Id<Person> personId3 = Id.createPersonId("person3");

		final Id<Vehicle> vehicleId1 = Id.createVehicleId("vehicle1");
		final Id<Vehicle> vehicleId2 = Id.createVehicleId("vehicle2");
		final Id<Vehicle> vehicleId3 = Id.createVehicleId("vehicle3");
		
		Id<Link> linkId1 = Id.createLinkId("link1");
		Id<Link> linkId2 = Id.createLinkId("link2");
		Id<Link> linkId3 = Id.createLinkId("link3");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);

		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis(scenario);
				
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId1, vehicleId1));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId2, vehicleId2));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId3, vehicleId3));

		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId2, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId3, linkId1));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId1, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId2, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId3, linkId2));
		moneyAnalysis.handleEvent(new PersonMoneyEvent(966., personId2, -99.));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId1, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId2, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId3, linkId3));
		moneyAnalysis.handleEvent(new PersonMoneyEvent(3661., personId2, -123.));
		moneyAnalysis.handleEvent(new PersonMoneyEvent(3662., personId3, -8.));

		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 0));

		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(0));
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getEnteringAgents().size());
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getEnteringAgents().size());
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(1));
		Assert.assertEquals("wrong average amount", -99./3., moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(2)); 
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(3)); 
		Assert.assertEquals("wrong average amount", (-123 - 8.) / 3., moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(5)); 

	}
}
