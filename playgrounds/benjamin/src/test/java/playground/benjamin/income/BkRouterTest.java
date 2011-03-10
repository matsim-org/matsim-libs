/* *********************************************************************** *
 * project: org.matsim.*
 * BkRouterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.benjamin.income;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.households.Households;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the routing of the BkIncomeControler
 *
 * @author dgrether and benjamin
 */
public class BkRouterTest extends MatsimTestCase {
	
	private PersonHouseholdMapping personHouseholdMapping;


	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	/*package*/ final static Id id3 = new IdImpl("3");
	/*package*/ final static Id id8 = new IdImpl("8");
	/*package*/ final static Id id10 = new IdImpl("10");

	public void testGeneralizedCostRouting() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configRouterTest.xml");
		config.controler().setOutputDirectory(this.getOutputDirectory());
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansRouterTest.xml");
		//hh loading
		config.scenario().setUseHouseholds(true);
		config.households().setInputFile(this.getClassInputDirectory() + "households.xml");
		
		ScenarioLoaderImpl scLoader = new ScenarioLoaderImpl(config) ;
		ScenarioImpl sc = (ScenarioImpl) scLoader.loadScenario() ;

		Controler controler = new Controler(sc);
		controler.setCreateGraphs(false);
		final TestDataGatherer handler = new TestDataGatherer();
		
		this.personHouseholdMapping = new PersonHouseholdMapping( ((ScenarioImpl) controler.getScenario()).getHouseholds() );
		
		// Make sure both the scoring function and the travel cost calculator (for the routing) are configured in a consistent way!
		installScoringFunctionFactory(controler);
		installTravelCostCalculatorFactory(controler);

		
		controler.addControlerListener(new TestDataStartupListener(handler));

		controler.run();
		assertTrue("Person 2 should be routed on link 3", handler.link3Ok);
		assertTrue("Person 1 should be routed on link 8", handler.link8Ok);
		assertTrue("Person 3 should be routed on link 10", handler.link10Ok);
	}

	public void testGeneralizedTollCostRouting() {
			Config config = this.loadConfig(this.getClassInputDirectory() + "configRouterTest.xml");
			config.controler().setOutputDirectory(this.getOutputDirectory());
			String netFileName = this.getClassInputDirectory() + "networkForToll.xml";
			config.network().setInputFile(netFileName);
			config.plans().setInputFile(this.getClassInputDirectory() + "plansRouterTollTest.xml");
			//hh loading
			config.scenario().setUseHouseholds(true);
			config.households().setInputFile(this.getClassInputDirectory() + "householdsForToll.xml");
			//setting road pricing on link 8
			config.scenario().setUseRoadpricing(true);
			config.roadpricing().setTollLinksFile(this.getClassInputDirectory() + "tollLinksFile.xml");
	
			Controler controler = new Controler(config);
			controler.setCreateGraphs(false);
			final TestDataGathererToll handler = new TestDataGathererToll();
			
			controler.addControlerListener(new TestDataStartupListenerToll(handler));
	
			controler.run();
			assertTrue("Person 1 should be routed on link 10", handler.link10Ok);
		}

	
	private final class TestDataStartupListener implements StartupListener {
		private final TestDataGatherer handler;

		private TestDataStartupListener(TestDataGatherer handler) {
			this.handler = handler;
		}

		@Override
		public void notifyStartup(final StartupEvent event) {
			event.getControler().getEvents().addHandler(handler);
		}
	}

	public class TestDataStartupListenerToll implements StartupListener {
		private final TestDataGathererToll handler;
		
		public TestDataStartupListenerToll(TestDataGathererToll handler) {
			this.handler = handler;
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			event.getControler().getEvents().addHandler(handler);
		}
	}

	private static class TestDataGatherer implements LinkEnterEventHandler{

		boolean link3Ok = false;
		boolean link8Ok = false;
		boolean link10Ok = false;

		//links 3, 8, 10
		public void handleEvent(LinkEnterEvent e) {
			if (e.getLinkId().equals(id3) && e.getPersonId().equals(id2)) {
				link3Ok = true;
			}
			else if (e.getLinkId().equals(id8) && e.getPersonId().equals(id1)) {
				link8Ok = true;
			}
			else if (e.getLinkId().equals(id10) && e.getPersonId().equals(id3)) {
				link10Ok = true;
			}
		}
		public void reset(int iteration) {}
	}

	
	private static class TestDataGathererToll implements LinkEnterEventHandler{
	
		boolean link10Ok = false;
	
		//link 10
		public void handleEvent(LinkEnterEvent e) {
			if (e.getLinkId().equals(id10) && e.getPersonId().equals(id1)) {
				link10Ok = true;
			}
		}
		public void reset(int iteration) {}
	}
	
	private void installScoringFunctionFactory(Controler controler) {
		Scenario scenario = controler.getScenario();
		ScoringFunctionFactory scoringFactory = 
			new IncomeScoringFunctionFactory(scenario.getConfig(), personHouseholdMapping, scenario.getNetwork());
		controler.setScoringFunctionFactory(scoringFactory);
	}
	
	private void installTravelCostCalculatorFactory(Controler controler) {
		// returns null, if there is no road pricing
		if (controler.getConfig().scenario().isUseRoadpricing()){
			RoadPricingScheme roadPricingScheme = controler.getRoadPricing().getRoadPricingScheme();
			TravelCostCalculatorFactory travelCostCalculatorFactory = 
				new IncomeTollTravelCostCalculatorFactory(personHouseholdMapping, roadPricingScheme);
			controler.setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		}
		else{
			TravelCostCalculatorFactory travelCostCalculatorFactory = new IncomeTravelCostCalculatorFactory(personHouseholdMapping);
			controler.setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		}
	}



}