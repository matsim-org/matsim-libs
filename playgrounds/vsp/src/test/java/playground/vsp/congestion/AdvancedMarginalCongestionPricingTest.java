/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalSumScoringFunction;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author ikaddoura
 *
 */
public class AdvancedMarginalCongestionPricingTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// test normal activities
	@Test
	public final void test0a(){
		
		PlanCalcScoreConfigGroup plansCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();
		ActivityParams activityParams = new ActivityParams("work");
		activityParams.setTypicalDuration(6 * 3600.);
		activityParams.setOpeningTime(7 * 3600.);
		activityParams.setClosingTime(18 * 3600.);
		
		plansCalcScoreConfigGroup.addActivityParams(activityParams);
		plansCalcScoreConfigGroup.setEarlyDeparture_utils_hr(0.);
		plansCalcScoreConfigGroup.setLateArrival_utils_hr(0.);
		plansCalcScoreConfigGroup.setMarginalUtlOfWaiting_utils_hr(0.);
		plansCalcScoreConfigGroup.setPerforming_utils_hr(6.);
		
		ScenarioConfigGroup scenarioConfig = new ScenarioConfigGroup();

		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters.Builder(plansCalcScoreConfigGroup, plansCalcScoreConfigGroup.getScoringParameters(null), scenarioConfig).build();

		MarginalSumScoringFunction marginaSumScoringFunction = new MarginalSumScoringFunction(params);
		
		Id<Link> linkId = null;
		
		// test if zero delay results in zero activity delay disutility
		Activity activity1 = PopulationUtils.createActivityFromLinkId("work", linkId);
		activity1.setStartTime(10 * 3600.);
		activity1.setEndTime(16 * 3600.);
		double delay1 = 0 * 3600.;
		double activityDelayDisutility1 = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity1, delay1);
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 0., activityDelayDisutility1, MatsimTestUtils.EPSILON);

		// test if a delay results in zero activity delay disutility if the agent would have arrived to late at the activity location anyway
		Activity activity2 = PopulationUtils.createActivityFromLinkId("work", linkId);
		activity2.setStartTime(19 * 3600.);
		activity2.setEndTime(20 * 3600.);
		double delay2 = 0.5 * 3600.;
		double activityDelayDisutility2 = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity2, delay2);
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 0., activityDelayDisutility2, MatsimTestUtils.EPSILON);
	
		// test if a delay results in zero activity delay disutility if the agent would have arrived to early at the activity location anyway
		Activity activity3 = PopulationUtils.createActivityFromLinkId("work", linkId);
		activity3.setStartTime(4 * 3600.);
		activity3.setEndTime(5 * 3600.);
		double delay3 = 0.5 * 3600.;
		double activityDelayDisutility3 = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity3, delay3);
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 0., activityDelayDisutility3, MatsimTestUtils.EPSILON);
		
		// test if a delay results in the right activity delay disutility if the agent would have had more time to perform the activity
		Activity activity4 = PopulationUtils.createActivityFromLinkId("work", linkId);
		activity4.setStartTime(10 * 3600.);
		activity4.setEndTime(16 * 3600.);
		double delay4 = 1 * 3600.;
		double activityDelayDisutility4 = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity4, delay4);
		// 6 hours --> 65.549424473781 utils
		// 5 hours --> 60 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 5.549424473781310, activityDelayDisutility4, MatsimTestUtils.EPSILON);
		
		// repeat the previous test: test if a delay results in the right activity delay disutility if the agent would have had more time to perform the activity
		double activityDelayDisutility4b = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity4, delay4);
		// 6 hours --> 65.549424473781 utils
		// 5 hours --> 60 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 5.549424473781310, activityDelayDisutility4b, MatsimTestUtils.EPSILON);
	}
	
	// test overnight activities with first and last activity of the same type
	@Test
	public final void test0b(){
		
		PlanCalcScoreConfigGroup plansCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();
		ActivityParams activityParams = new ActivityParams("overnightActivity");
		activityParams.setTypicalDuration(12 * 3600.);
		
		plansCalcScoreConfigGroup.addActivityParams(activityParams);
		plansCalcScoreConfigGroup.setEarlyDeparture_utils_hr(0.);
		plansCalcScoreConfigGroup.setLateArrival_utils_hr(0.);
		plansCalcScoreConfigGroup.setMarginalUtlOfWaiting_utils_hr(0.);
		plansCalcScoreConfigGroup.setPerforming_utils_hr(6.);
		
		ScenarioConfigGroup scenarioConfig = new ScenarioConfigGroup();

		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters.Builder(plansCalcScoreConfigGroup, plansCalcScoreConfigGroup.getScoringParameters(null), scenarioConfig).build();

		MarginalSumScoringFunction marginaSumScoringFunction = new MarginalSumScoringFunction(params);
		
		Id<Link> linkId = null;
		
		Activity activity1 = PopulationUtils.createActivityFromLinkId("overnightActivity", linkId);
		activity1.setEndTime(7 * 3600.);
		Activity activity2 = PopulationUtils.createActivityFromLinkId("overnightActivity", linkId);
		activity2.setStartTime(18 * 3600.);
		
		// test if zero delay results in zero activity delay disutility
		double delay1 = 0 * 3600.;
		double activityDelayDisutility1 = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activity1, activity2, delay1);
		// 6 + 7 hours --> 65.763074952494600 utils
		// 6 + 7 hours --> 65.763074952494600 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 0., activityDelayDisutility1, MatsimTestUtils.EPSILON);
	
		// test if a delay results in the right activity delay disutility
		double delay2 = 1 * 3600.;
		double activityDelayDisutility2 = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activity1, activity2, delay2);
		// 6 + 7 hours --> 65.763074952494600 utils
		// 7 + 7 hours --> 71.098848947562600 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 5.335773995067980, activityDelayDisutility2, MatsimTestUtils.EPSILON);
		
		// repeat the previous test: test if a delay results in the right activity delay disutility
		double activityDelayDisutility2b = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activity1, activity2, delay2);
		// 6 + 7 hours --> 65.763074952494600 utils
		// 7 + 7 hours --> 71.098848947562600 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 5.335773995067980, activityDelayDisutility2b, MatsimTestUtils.EPSILON);
	}
	
	// test overnight activities with first and last activity of different types
	@Test
	public final void test0c(){
		
		PlanCalcScoreConfigGroup plansCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();
		
		ActivityParams activityParams1 = new ActivityParams("firstActivityType");
		activityParams1.setTypicalDuration(12 * 3600.);
		
		ActivityParams activityParams2 = new ActivityParams("lastActivityType");
		activityParams2.setTypicalDuration(12 * 3600.);
		
		plansCalcScoreConfigGroup.addActivityParams(activityParams1);
		plansCalcScoreConfigGroup.addActivityParams(activityParams2);

		plansCalcScoreConfigGroup.setEarlyDeparture_utils_hr(0.);
		plansCalcScoreConfigGroup.setLateArrival_utils_hr(0.);
		plansCalcScoreConfigGroup.setMarginalUtlOfWaiting_utils_hr(0.);
		plansCalcScoreConfigGroup.setPerforming_utils_hr(6.);
		
		ScenarioConfigGroup scenarioConfig = new ScenarioConfigGroup();
		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters.Builder(plansCalcScoreConfigGroup, plansCalcScoreConfigGroup.getScoringParameters(null), scenarioConfig).build();

		MarginalSumScoringFunction marginaSumScoringFunction = new MarginalSumScoringFunction(params);
		
		Id<Link> linkId = null;
		
		Activity activity1 = PopulationUtils.createActivityFromLinkId("firstActivityType", linkId);
		activity1.setEndTime(7 * 3600.);
		Activity activity2 = PopulationUtils.createActivityFromLinkId("lastActivityType", linkId);
		activity2.setStartTime(18 * 3600.);
		
		// test if zero delay results in zero activity delay disutility
		double delay1 = 0 * 3600.;
		double activityDelayDisutility1 = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activity1, activity2, delay1);
		// 6 --> 10.0934029996839 utils + 7 --> 21.1922519472465 utils = 31.285654946930 utils
		// 6 --> 10.0934029996839 utils + 7 --> 21.1922519472465 utils = 31.285654946930 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 0., activityDelayDisutility1, MatsimTestUtils.EPSILON);
	
		// test if a delay results in the right activity delay disutility
		double delay2 = 1 * 3600.;
		double activityDelayDisutility2 = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activity1, activity2, delay2);
		// 6 --> 10.0934029996839 utils + 7 --> 21.1922519472465 utils = 31.285654946930 utils
		// 7 --> 21.1922519472465 utils + 7 --> 21.1922519472465 utils = 42.3845038944931 utils
		Assert.assertEquals("Wrong disutility from starting an activity with a delay (arriving later at the activity location).", 11.0988489475631, activityDelayDisutility2, MatsimTestUtils.EPSILON);
	}	
	
	// test if the delayed arrival at a normal activity (not the first or last activity) results in the right monetary amount
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "AdvancedMarginalCongestionPricingTest/config1.xml";
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		Controler controler = new Controler( scenario );

		EventsManager events = controler.getEvents();
				
		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
				
		events.addHandler( new PersonMoneyEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(PersonMoneyEvent event) {
				moneyEvents.add(event);
			}	
		});
		
		final TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;
					@Override
					public ControlerListener get() {
						return new AdvancedMarginalCongestionPricingContolerListener(scenario, tollHandler, new CongestionHandlerImplV3(eventsManager, (MutableScenario) scenario));
					}
				});
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});

		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		
		// test if there is only one congestion event and only one money event
		Assert.assertEquals("Wrong number of congestion events.", 1, congestionEvents.size());
		Assert.assertEquals("Wrong number of money events.", 1, moneyEvents.size());

		// test if the delay is 2 seconds
		double delay = congestionEvents.get(0).getDelay();
		Assert.assertEquals("Wrong delay.", 2.0, delay, MatsimTestUtils.EPSILON);

		double amountFromEvent = moneyEvents.get(0).getAmount();
		double tripDelayDisutility = delay / 3600. * controler.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() * (-1);
		// with delay --> 70.570685898554200
		// without delay --> 70.573360291244900
		double activityDelayDisutility = 70.573360291244900 - 70.570685898554200;
		double amount = (-1) * (activityDelayDisutility + tripDelayDisutility) / controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		Assert.assertEquals("Wrong amount.", amount, amountFromEvent, MatsimTestUtils.EPSILON);	
	 }
	
	// test if a delayed arrival at the last activity results in the right monetary amount
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory() + "AdvancedMarginalCongestionPricingTest/config2.xml";

		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		Controler controler = new Controler( scenario );

		EventsManager events = controler.getEvents();
				
		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
				
		events.addHandler( new PersonMoneyEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(PersonMoneyEvent event) {
				moneyEvents.add(event);
			}	
		});
		
		final TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;
					@Override
					public ControlerListener get() {
						return new AdvancedMarginalCongestionPricingContolerListener(scenario, tollHandler, new CongestionHandlerImplV3(eventsManager, (MutableScenario) scenario));
					}
				});
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		
		// test if there is only one congestion event and only one money event
		Assert.assertEquals("Wrong number of congestion events.", 1, congestionEvents.size());
		Assert.assertEquals("Wrong number of money events.", 1, moneyEvents.size());

		// test if the delay is 2 seconds
		double delay = congestionEvents.get(0).getDelay();
		Assert.assertEquals("Wrong delay.", 2.0, delay, MatsimTestUtils.EPSILON);

		double amountFromEvent = moneyEvents.get(0).getAmount();
		double tripDelayDisutility = delay / 3600. * controler.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() * (-1);
		
		// home duration morning: 28800.
		// home duration evening with delay: (24 * 3600.) - 57705.
		// home duration evening without delay: (24 * 3600.) - 57703.
		
		// with delay --> 80.581739442040600
		// without delay --> 80.584243964094500
				
		double activityDelayDisutility = 80.584243964094500 - 80.581739442040600;
		double amount = (-1) * (activityDelayDisutility + tripDelayDisutility) / controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		Assert.assertEquals("Wrong amount.", amount, amountFromEvent, MatsimTestUtils.EPSILON);	
	 }	
	
	// test if the right number of money events are thrown
	@Test
	public final void test3(){
		
		String configFile = testUtils.getPackageInputDirectory() + "AdvancedMarginalCongestionPricingTest/config3.xml";

		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);

		final Scenario scenario = ScenarioUtils.loadScenario( config);
		Controler controler = new Controler( scenario );
		
		EventsManager events = controler.getEvents();
				
		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
				
		events.addHandler( new PersonMoneyEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(PersonMoneyEvent event) {
				moneyEvents.add(event);
			}	
		});
		
		final TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;
					@Override
					public ControlerListener get() {
						return new AdvancedMarginalCongestionPricingContolerListener(scenario, tollHandler, new CongestionHandlerImplV3(eventsManager, (MutableScenario) scenario));
					}
				});
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		
		// test if there are three congestion events and three money events
		Assert.assertEquals("Wrong number of congestion events.", 3, congestionEvents.size());
		Assert.assertEquals("Wrong number of money events.", 3, moneyEvents.size());
	 }
	
	// test if the right number of money events are thrown
	@Test
	public final void test4(){
		
		String configFile = testUtils.getPackageInputDirectory() + "AdvancedMarginalCongestionPricingTest/config4.xml";

		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		Controler controler = new Controler( scenario );
		
		EventsManager events = controler.getEvents();
				
		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
				
		events.addHandler( new PersonMoneyEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(PersonMoneyEvent event) {
				moneyEvents.add(event);
			}	
		});
		
		final TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;
					@Override
					public ControlerListener get() {
						return new AdvancedMarginalCongestionPricingContolerListener(scenario, tollHandler, new CongestionHandlerImplV3(eventsManager, (MutableScenario) scenario));
					}
				});
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		
		// test if there are three congestion events and three money events
		Assert.assertEquals("Wrong number of congestion events.", 3, congestionEvents.size());
		Assert.assertEquals("Wrong number of money events.", 3, moneyEvents.size());
	 }
}
