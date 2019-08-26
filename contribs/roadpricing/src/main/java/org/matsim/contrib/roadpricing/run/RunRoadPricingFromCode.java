/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.contrib.roadpricing.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic 'script' to run roadpricing. This example ahows how to, programatically,
 * configure the roadpricing scheme instead of reading it from an XML file.
 */
public class RunRoadPricingFromCode {
	private static final String TEST_CONFIG = "./contribs/roadpricing/test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	public static void main(String[] args) { run(args); }

	private static void run(String[] args){
		if(args.length==0){ args = new String[]{TEST_CONFIG}; }

		/* Start with a known config file (with population, network, and scoring
		parameteres specified) and just remove the road pricing file. */
		Config config = ConfigUtils.loadConfig(args[0], RoadPricingUtils.createConfigGroup());
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class).setTollLinksFile(null);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);

		// prepare scenario:

		Scenario sc = ScenarioUtils.loadScenario(config);

		createCustomRoadPricingScheme(sc);

		// prepare controler:

		Controler controler = new Controler(sc);

		controler.addOverridingModule( new RoadPricingModule() );

		// run controler:

		controler.run();
	}

	private static void runFromFile(String[] args){
		// yyyyyy this method is now totally in the wrong class!  kai, jul'19

		if(args.length==0){ args = new String[]{TEST_CONFIG}; }

		/* Start with a known config file (with population, network, and scoring
		parameteres specified) and just remove the road pricing file. */
		Config config = ConfigUtils.loadConfig(args[0], RoadPricingUtils.createConfigGroup());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);

		// prepare scenario:

		Scenario sc = ScenarioUtils.loadScenario(config);
		RoadPricingUtils.loadRoadPricingScheme( sc );

		// prepare scenario:

		Controler controler = new Controler(sc);

		controler.addOverridingModule( new RoadPricingModule() );

		controler.run();
	}


	private static void createCustomRoadPricingScheme( Scenario scenario){
		RoadPricingSchemeImpl scheme1 = RoadPricingUtils.createAndRegisterMutableScheme(scenario );

		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme1, "custom");
		RoadPricingUtils.setType(scheme1, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme1, "Custom coded road pricing scheme");

		/* Add the link-specific toll. */
		RoadPricingUtils.addLinkSpecificCost(scheme1,
				Id.createLinkId("link_4_5"),
				Time.parseTime("00:00:00"),
				Time.parseTime("30:00:00"),
				100.0);

		/* Add general toll. */
		RoadPricingUtils.addLink(scheme1, Id.createLinkId("link_1_2"));
		RoadPricingUtils.addLink(scheme1, Id.createLinkId("link_2_1"));
		RoadPricingUtils.createAndAddGeneralCost(scheme1,
				Time.parseTime("06:00:00"),
				Time.parseTime("10:00:00"),
				10.0);

		RoadPricingScheme scheme2 = new RoadPricingScheme(){
			@Override public String getName(){
				return scheme1.getName() ;
			}

			@Override public String getType(){
				return scheme1.getType() ;
			}

			@Override public String getDescription(){
				return scheme1.getDescription() ;
			}

			@Override public Set<Id<Link>> getTolledLinkIds(){
				return scheme1.getTolledLinkIds() ;
			}

			@Override
			public RoadPricingCost getLinkCostInfo( Id<Link> linkId , double time , Id<Person> personId , Id<Vehicle> vehicleId ){
				RoadPricingCost baseCost = scheme1.getLinkCostInfo( linkId , time , personId , vehicleId );

				Vehicle vehicle = scenario.getVehicles().getVehicles().get( vehicleId ) ;
				Gbl.assertNotNull(vehicle);
				if ( true ) {
					return baseCost ;
				} else {
					// i.e. has an eTag
					return new RoadPricingCost(baseCost.startTime, baseCost.endTime, baseCost.amount * 0.8 ) ;
				}
			}

			@Override public RoadPricingCost getTypicalLinkCostInfo( Id<Link> linkId , double time ){
				return scheme1.getTypicalLinkCostInfo( linkId, time ) ;
			}

			@Override public Iterable<RoadPricingCost> getTypicalCosts(){
				return scheme1.getTypicalCosts() ;
			}

			@Override public Map<Id<Link>, List<RoadPricingCost>> getTypicalCostsForLink(){
				return scheme1.getTypicalCostsForLink() ;
			}
		} ;


	}
}
