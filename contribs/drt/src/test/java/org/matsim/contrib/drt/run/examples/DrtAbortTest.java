package org.matsim.contrib.drt.run.examples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.abort.DrtRejectionModule;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

import static org.matsim.core.config.groups.ScoringConfigGroup.*;

public class DrtAbortTest{
	private static final Logger log = LogManager.getLogger(DrtAbortTest.class );

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	static final String walkAfterRejectMode = "walkAfterReject";

	enum Variant {simpleTest, iterations, benchmark };

	@Test public void testAbortHandler() {
		run( Variant.simpleTest);
	}

	@Test public void testIterations() {
		run( Variant.iterations );
	}

	@Test public void testBenchmark() {
		run( Variant.benchmark );
	}

	private void run( Variant variant) {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("mielec" ), "mielec_drt_config.xml" );
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup() );

		boolean rejectionModule = true;
		config.controller().setLastIteration(50);
		config.plans().setInputFile("plans_only_drt_4.0.xml.gz");
//		config.global().setRandomSeed(9999);
		{
			ReplanningConfigGroup.StrategySettings  settings = new ReplanningConfigGroup.StrategySettings();
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
			settings.setWeight(0.1);
			config.replanning().addStrategySettings(settings);
			config.replanning().setFractionOfIterationsToDisableInnovation(0.9);
			config.replanning().setMaxAgentPlanMemorySize(5);
		}
		{
			config.changeMode().setModes( new String[] { TransportMode.drt, TransportMode.bike });
		}
		{
			ModeParams params = new ModeParams( TransportMode.bike );
			params.setMarginalUtilityOfTraveling(-6.);
			config.scoring().addModeParams( params );
		}

		for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get( config ).getModalElements()) {
//			drtCfg.vehiclesFile = "vehicles-2-cap-4.xml";
			drtCfg.vehiclesFile = "vehicles-10-cap-4.xml";
//			drtCfg.vehiclesFile = "vehicles-20-cap-2.xml";

			drtCfg.maxTravelTimeAlpha = 1.5;
			drtCfg.maxTravelTimeBeta = 600.;
			drtCfg.maxWaitTime = 300.;
			drtCfg.stopDuration = 10.;
		}

		switch ( variant ) {
			case simpleTest -> {
				config.controller().setLastIteration(1);
				config.plans().setInputFile("plans_only_drt_rejection_test.xml");
				// Chengqi: I have created a special plan for the rejection handler test: 3 requests within 1 time bin (6:45 - 7:00)
				for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get( config ).getModalElements()) {
					drtCfg.vehiclesFile = "vehicles-rejection-test.xml";
					// Chengqi: I have created a special vehicle file for the rejection handler test: 1 vehicle locates at the departure place of one request

					drtCfg.maxTravelTimeAlpha = 1.2;
					drtCfg.maxTravelTimeBeta = 100.;
					drtCfg.maxWaitTime = 10.;
					drtCfg.stopDuration = 1.;
					// (Trying to force abort(s); can't say if this is the correct syntax.  kai, apr'23)
					// Chengqi: With this parameter, 2 out of the 3 requests during 6:45-7:00 will be rejected
					// -> 2/3 probability of being rejected -> 2/3 of penalty to everyone who submit DRT requests
					// Based on current setup, at iteration 1, we should see person score event for each person
					// with a negative score of -6: 12 (base penalty) * 2/3 (probability) * 0.75 (learning rate, current) + 0 (previous penalty) * 0.25 (learning rate, previous)
					// Currently a manual check is performed and passed. Perhaps an integrated test can be implemented here (TODO).
				}
			}
			case benchmark -> rejectionModule = false;
			case iterations -> {
			}
			// What do we want to see?

			// In early iterations, we want many (maybe 20% or 50%) drt_teleported (because of rejections).

			// In late iterations, we want few drt_teleported (because of mode choice).

			// Need to look at the numbers.

			// There should be a certain rejection rate in a given time bin.  That should translate into a penalty.  The penalty should be reasonable for us.

			// The drt_teleported score should be plausible.

			default -> throw new IllegalStateException("Unexpected value: " + variant);
		}

		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		config.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( walkAfterRejectMode ) ).setScoringThisActivityAtAll( false ) );
		config.scoring().addModeParams( new ModeParams( walkAfterRejectMode ) );

		config.scoring().setWriteExperiencedPlans( true );

//		for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config ).getModalElements()) {
		//relatively high max age to prevent rejections
//			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
//			drtRequestInsertionRetryParams.maxRequestAge = 7200;
//			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
			// I don't know what the above does; might be useful to understand.
//		}

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get( config );
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing() );

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config );
		ScenarioUtils.loadScenario(scenario );

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule() );
		controler.addOverridingModule(new MultiModeDrtModule() );
		controler.configureQSimComponents( DvrpQSimComponents.activateAllModes(multiModeDrtConfig ) );

		if (rejectionModule){
			controler.addOverridingModule( new DrtRejectionModule() );
		}

		controler.run();

		// yy I cannot say if the expected status is useful here.  kai, apr'23

		var expectedStats = RunDrtExampleIT.Stats.newBuilder()
							 .rejectionRate(1.0)
							 .rejections(1)
							 .waitAverage(Double.NaN)
							 .inVehicleTravelTimeMean(Double.NaN)
							 .totalTravelTimeMean(Double.NaN)
							 .build();

		// Chengqi: I commented this line, because NaN cannot be checked (NaN == NaN always false)
//		RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


}
