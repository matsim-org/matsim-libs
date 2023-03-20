/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.AbortHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.modal.AbstractModalQSimModule;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.base.MoreObjects;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

/**
 * @author jbischoff
 */
public class RunDrtExampleIT {
	private static final Logger log = LogManager.getLogger(RunDrtExampleIT.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDrtExampleWithNoRejections_ExtensiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(296.95)
				.inVehicleTravelTimeMean(387.02)
				.totalTravelTimeMean(683.97)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtExampleWithNoRejections_SelectiveSearch() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//replace extensive with selective search
			drtCfg.removeParameterSet(drtCfg.getDrtInsertionSearchParams());
			var selectiveInsertionSearchParams = new SelectiveInsertionSearchParams();
			// using exactly free-speed estimates
			selectiveInsertionSearchParams.restrictiveBeelineSpeedFactor = 1;
			drtCfg.addParameterSet(selectiveInsertionSearchParams);

			//disable rejections
			drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.0)
				.rejections(0)
				.waitAverage(293.63)
				.inVehicleTravelTimeMean(388.85)
				.totalTravelTimeMean(682.48)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtExampleWithRequestRetry() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//relatively high max age to prevent rejections
			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
			drtRequestInsertionRetryParams.maxRequestAge = 7200;
			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
					 .rejectionRate(0.0)
					 .rejections(1)
					 .waitAverage(305.97)
					 .inVehicleTravelTimeMean(378.18)
					 .totalTravelTimeMean(684.16)
					 .build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}
	@Test
	public void testAbortHandler() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

//		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//relatively high max age to prevent rejections
//			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
//			drtRequestInsertionRetryParams.maxRequestAge = 7200;
//			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
//		}
		// I don't know what the above does; might be useful to understand.

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> components = qsimComponents.getActiveComponents();
		components.add( MyAbortHandler.COMPONENT_NAME );
		qsimComponents.setActiveComponents( components );

		final Controler controler = DrtControlerCreator.createControler( config, false );

		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.addQSimComponentBinding( MyAbortHandler.COMPONENT_NAME ).to( MyAbortHandler.class );
			}
		} );

		controler.run();

		var expectedStats = Stats.newBuilder()
					 .rejectionRate(0.0)
					 .rejections(1)
					 .waitAverage(305.97)
					 .inVehicleTravelTimeMean(378.18)
					 .totalTravelTimeMean(684.16)
					 .build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunDrtStopbasedExample() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(17)
				.waitAverage(260.41)
				.inVehicleTravelTimeMean(374.87)
				.totalTravelTimeMean(635.28)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	public void testRunServiceAreabasedExampleWithSpeedUp() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_serviceArea_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);

		var expectedStats = Stats.newBuilder()
				.rejectionRate(0.03)
				.rejections(11)
				.waitAverage(223.86)
				.inVehicleTravelTimeMean(389.57)
				.totalTravelTimeMean(613.44)
				.build();

		verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	/**
	 * Early warning system: if customer stats vary more than the defined percentage above or below the expected values
	 * then the following unit tests will fail. This is meant to serve as a red flag.
	 * The following customer parameter checked are:
	 * rejectionRate, rejections, waitAverage, inVehicleTravelTimeMean, & totalTravelTimeMean
	 */

	private void verifyDrtCustomerStatsCloseToExpectedStats(String outputDirectory, Stats expectedStats) {

		String filename = outputDirectory + "/drt_customer_stats_drt.csv";

		final List<String> collect;
		try {
			collect = Files.lines(Paths.get(filename)).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int size = collect.size();
		List<String> keys = List.of(collect.get(0).split(";"));
		List<String> lastIterationValues = List.of(collect.get(size - 1).split(";"));

		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			params.put(keys.get(i), lastIterationValues.get(i));
		}

		var actualStats = Stats.newBuilder()
				.rejectionRate(Double.parseDouble(params.get("rejectionRate")))
				.rejections(Double.parseDouble(params.get("rejections")))
				.waitAverage(Double.parseDouble(params.get("wait_average")))
				.inVehicleTravelTimeMean(Double.parseDouble(params.get("inVehicleTravelTime_mean")))
				.totalTravelTimeMean(Double.parseDouble(params.get("totalTravelTime_mean")))
				.build();

		assertThat(actualStats).usingRecursiveComparison().isEqualTo(expectedStats);
	}

	private static class Stats {
		private final double rejectionRate;
		private final double rejections;
		private final double waitAverage;
		private final double inVehicleTravelTimeMean;
		private final double totalTravelTimeMean;

		private Stats(Builder builder) {
			rejectionRate = builder.rejectionRate;
			rejections = builder.rejections;
			waitAverage = builder.waitAverage;
			inVehicleTravelTimeMean = builder.inVehicleTravelTimeMean;
			totalTravelTimeMean = builder.totalTravelTimeMean;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("rejectionRate", rejectionRate)
					.add("rejections", rejections)
					.add("waitAverage", waitAverage)
					.add("inVehicleTravelTimeMean", inVehicleTravelTimeMean)
					.add("totalTravelTimeMean", totalTravelTimeMean)
					.toString();
		}

		public static Builder newBuilder() {
			return new Builder();
		}

		public static final class Builder {
			private double rejectionRate;
			private double rejections;
			private double waitAverage;
			private double inVehicleTravelTimeMean;
			private double totalTravelTimeMean;

			private Builder() {
			}

			public Builder rejectionRate(double val) {
				rejectionRate = val;
				return this;
			}

			public Builder rejections(double val) {
				rejections = val;
				return this;
			}

			public Builder waitAverage(double val) {
				waitAverage = val;
				return this;
			}

			public Builder inVehicleTravelTimeMean(double val) {
				inVehicleTravelTimeMean = val;
				return this;
			}

			public Builder totalTravelTimeMean(double val) {
				totalTravelTimeMean = val;
				return this;
			}

			public Stats build() {
				return new Stats(this);
			}
		}
	}

	private static class MyAbortHandler implements AbortHandler, MobsimEngine {
		public static final String COMPONENT_NAME = "DrtAbortHandler";

		@Inject EditTrips editTrips ;
		@Inject EditPlans editPlans;
		@Inject Network network;
		@Inject Population population;
		@Inject MobsimTimer mobsimTimer;
		private InternalInterface internalInterface;
		private List<MobsimAgent> agents = new ArrayList<>();
		@Override public boolean handleAbort( MobsimAgent agent ){

			log.warn("need to handle abort of agent=" + agent );


			final String drtMode = "drt";
			final String walkAfterRejectMode = "walkAfterReject";

			if ( agent.getMode().equals( drtMode ) ) {
				// yyyyyy this will have to work for all drt modes!!!

				Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );
				int index = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );

				Id<Link> interactionLink = agent.getCurrentLinkId();

				double now = mobsimTimer.getTimeOfDay();

				// (1) The current leg needs to be modified so that it ends at the current location.  And one should somehow tag this
				// as a failed drt trip.  (There is presumably already a drt rejected event, so this info could also be reconstructed.)

				PopulationFactory pf = population.getFactory();

				Leg leg = (Leg) plan.getPlanElements().get(index );
				leg.setDepartureTime( now );
				leg.setTravelTime( 0 );
				leg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class, interactionLink, interactionLink ) );
				// (startLinkId and endLinkId are _only_ in the route)

				// (2) An interaction activity needs to be inserted.

				String modePrefix = TripStructureUtils.createStageActivityType( walkAfterRejectMode );
				Coord interactionCoord = network.getLinks().get( interactionLink ).getCoord();
				Activity activity = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix( interactionCoord, interactionLink, modePrefix );
				editPlans.insertActivity( agent, index+1, activity, drtMode, walkAfterRejectMode );
				// (inserts at given position; pushes everything else forward)

				// (3) There needs to be a new teleportation leg from here to there.
				// yy This looks more involved than it should be; presumably some methods need to be improved in editTrips/Plans.  kai, mar'23

				Activity toActivity;
				for ( int ii=index+1; true; ii++ ) {
					if ( editPlans.isRealActivity( plan.getPlanElements().get(ii) ) ) {
						toActivity = (Activity) plan.getPlanElements().get(ii );
						break;
					}
				}
				TripStructureUtils.Trip trip = editTrips.insertEmptyTrip( plan, activity, toActivity, "walkAfterReject" );
				editTrips.replanFutureTrip( trip, plan, walkAfterRejectMode );


				// (4) reset the agent caches:
				WithinDayAgentUtils.resetCaches( agent );

				// (5) add the agent to an internal list, which is processed during doSimStep, which formally ends the current
				// (aborted) leg, and moves the agent forward in its state machine.
				agents.add( agent );
			}
			return true;
		}
		@Override public void doSimStep( double time ){
			for( MobsimAgent agent : agents ){
				agent.endLegAndComputeNextState( time );
				// (we haven't actually thrown an abort event, and are planning not to do this here.  We probably have thrown a drt
				// rejected event.  The "endLeg..." method will throw a person arrival event.)

				this.internalInterface.arrangeNextAgentState( agent );
			}
			agents.clear();
		}
		@Override public void onPrepareSim(){
			throw new RuntimeException( "not implemented" );
		}
		@Override public void afterSim(){
			throw new RuntimeException( "not implemented" );
		}
		@Override public void setInternalInterface( InternalInterface internalInterface ){
			this.internalInterface = internalInterface;
		}
	}
}
