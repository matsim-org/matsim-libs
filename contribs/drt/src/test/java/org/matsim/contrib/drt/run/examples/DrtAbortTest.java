package org.matsim.contrib.drt.run.examples;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbortHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		config.controler().setLastIteration(50);
		{
			StrategyConfigGroup.StrategySettings settings = new StrategyConfigGroup.StrategySettings();
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
			settings.setWeight(0.1);
			config.strategy().addStrategySettings(settings);
			config.strategy().setFractionOfIterationsToDisableInnovation(0.9);
		}
		{
			config.changeMode().setModes( new String[] { TransportMode.drt, TransportMode.bike });
		}
		{
			PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams( TransportMode.bike );
			params.setMarginalUtilityOfTraveling(-12.);
			config.planCalcScore().addModeParams( params );
		}

		for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get( config ).getModalElements()) {
			drtCfg.vehiclesFile = "vehicles-2-cap-4.xml";

			drtCfg.maxTravelTimeAlpha = 1.5;
			drtCfg.maxTravelTimeBeta = 600.;
			drtCfg.maxWaitTime = 300.;
			drtCfg.stopDuration = 10.;
		}

		switch ( variant ) {
			case simpleTest -> {
				config.controler().setLastIteration(1);
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

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		{
			final PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams( TripStructureUtils.createStageActivityType( walkAfterRejectMode ) );
			params.setScoringThisActivityAtAll( false );
			config.planCalcScore().addActivityParams( params );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams( walkAfterRejectMode );
			config.planCalcScore().addModeParams( params );
		}

		config.planCalcScore().setWriteExperiencedPlans( true );

//		for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config ).getModalElements()) {
		//relatively high max age to prevent rejections
//			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
//			drtRequestInsertionRetryParams.maxRequestAge = 7200;
//			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
			// I don't know what the above does; might be useful to understand.
//		}

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get( config );
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute() );

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config );
		ScenarioUtils.loadScenario(scenario );

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule() );
		controler.addOverridingModule(new MultiModeDrtModule() );
		controler.configureQSimComponents( DvrpQSimComponents.activateAllModes(multiModeDrtConfig ) );

		if (rejectionModule){
			QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
			List<String> components = qsimComponents.getActiveComponents();
			components.add( MyAbortHandler.COMPONENT_NAME );
			qsimComponents.setActiveComponents( components );

			controler.addOverridingQSimModule( new AbstractQSimModule(){
				@Override protected void configureQSim(){
					this.addQSimComponentBinding( MyAbortHandler.COMPONENT_NAME ).to( MyAbortHandler.class );
				}
			} );
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(DrtRejectionEventHandler.class).in(Singleton.class);
					addEventHandlerBinding().to(DrtRejectionEventHandler.class);
					addControlerListenerBinding().to(DrtRejectionEventHandler.class);
				}
			});
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

		// I commented this line, because NaN cannot be checked (NaN == NaN always false)
//		RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


	private static class MyAbortHandler implements AbortHandler, MobsimEngine{
		public static final String COMPONENT_NAME = "DrtAbortHandler";
		private static final String delimiter = "============";
		@Inject Network network;
		@Inject Population population;
		@Inject MobsimTimer mobsimTimer;

		TravelTime travelTime;
		LeastCostPathCalculator router;

		// TODO we would need a DRT config group here, if we don't want to hardcode the alpha and beta values
		// DrtConfigGroup drtConfigGroup;

		private InternalInterface internalInterface;
		private final List<MobsimAgent> agents = new ArrayList<>();

		@Inject MyAbortHandler(Network network, Map<String, TravelTime> travelTimeMap){
			travelTime = travelTimeMap.get(TransportMode.car);
			router =  new SpeedyALTFactory().createPathCalculator(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
		}

		@Override public boolean handleAbort( MobsimAgent agent ){
			log.warn("need to handle abort of agent=" + agent );

			final String drtMode = "drt";

			PopulationFactory pf = population.getFactory();

			if ( agent.getMode().equals( drtMode ) ) {
				// yyyyyy this will have to work for all drt modes!!!

				Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );

				printPlan( "\n current plan=", plan );

				int index = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );

				Id<Link> interactionLink = agent.getCurrentLinkId();

				double now = mobsimTimer.getTimeOfDay();

				Leg leg = (Leg) plan.getPlanElements().get( index );

				Id<Link> originallyPlannedDestinationLink = leg.getRoute().getEndLinkId();

				// (1) The current leg needs to be modified so that it ends at the current location.  And one should somehow tag this
				// as a failed drt trip.  (There is presumably already a drt rejected event, so this info could also be reconstructed.)
				{
					leg.setDepartureTime( now );
					leg.setTravelTime( 0 );
					leg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class, interactionLink, interactionLink ) );
					// (startLinkId and endLinkId are _only_ in the route)
				}

				// (2) An interaction activity needs to be inserted.
				{
					Coord interactionCoord = network.getLinks().get( interactionLink ).getCoord();
//					Activity activity = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix( interactionCoord, interactionLink, walkAfterRejectMode );
					Activity activity = pf.createActivityFromCoord( TripStructureUtils.createStageActivityType( walkAfterRejectMode ), interactionCoord );
					activity.setMaximumDuration( 1. );
					plan.getPlanElements().add( index+1, activity );
					// (inserts at given position; pushes everything else forward)
				}

				// (3) There needs to be a new teleportation leg from here to there.
				{
					Leg secondLeg = pf.createLeg( walkAfterRejectMode );
					secondLeg.setDepartureTime( now );
					double directTravelTime = VrpPaths.calcAndCreatePath
							(network.getLinks().get(interactionLink), network.getLinks().get(originallyPlannedDestinationLink), now, router, travelTime).getTravelTime();
					//TODO Chengqi: it would be better if we can get the alpha beta values from DRT config group directly
 					// double estimatedTravelTime = drtConfigGroup.maxTravelTimeAlpha * directTravelTime + drtConfigGroup.maxTravelTimeBeta;
					double estimatedTravelTime = 1.5 * directTravelTime + 900;
					secondLeg.setTravelTime( estimatedTravelTime );
					secondLeg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class, interactionLink, originallyPlannedDestinationLink ) );
					plan.getPlanElements().add( index+2, secondLeg );

				}
				// (4) reset the agent caches:
				WithinDayAgentUtils.resetCaches( agent );

				// (5) add the agent to an internal list, which is processed during doSimStep, which formally ends the current
				// (aborted) leg, and moves the agent forward in its state machine.
				agents.add( agent );

				printPlan( "plan after splicing=", plan );
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
		}
		@Override public void afterSim(){
		}
		@Override public void setInternalInterface( InternalInterface internalInterface ){
			this.internalInterface = internalInterface;
		}
		private static void printPlan( String x, Plan plan ){
			log.warn( delimiter );
			log.warn( x + plan );
			for( PlanElement planElement : plan.getPlanElements() ){
				log.warn( planElement );
			}
			log.warn( delimiter );
		}
	}

	private static class DrtRejectionEventHandler implements PassengerRequestRejectedEventHandler,
			PassengerRequestSubmittedEventHandler, IterationEndsListener {
		private final Map<Integer, MutableInt> numberOfRejectionsPerTimeBin = new HashMap<>();
		private final Map<Integer, MutableInt> numberOfSubmissionsPerTimeBin = new HashMap<>();
		private final Map<Integer, Double> probabilityOfRejectionPerTimeBin = new HashMap<>();

		// Key parameters
		private final double timeBinSize = 900;
		// Time bin to analyze the probability of being rejected
		private final double rejectionCost = 12;
		// 12 -> 2 hour of default performing score
		private final double learningRate = 0.75;
		// (1 - alpha) * old probability + alpha * new probability (0 < alpha <= 1)

		@Inject private EventsManager events;

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			// Currently, we assume there is only 1 DRT operator with standard DRT mode ("drt")
			// Because it is a little tricky to get DRT Config Group here (which can only be acquired via DvrpQSimModule),
			// we just use the simple way. For multi-operator, a map can be introduced to store the data for different DRT modes
			if (event.getMode().equals(TransportMode.drt)) {
				int timeBin = getTimeBin(event.getTime());
				numberOfRejectionsPerTimeBin.computeIfAbsent(timeBin, c -> new MutableInt()).increment();
			}
		}

		@Override
		public void reset(int iteration) {
			PassengerRequestRejectedEventHandler.super.reset(iteration);
			if (iteration != 0) {
				numberOfSubmissionsPerTimeBin.clear();
				numberOfRejectionsPerTimeBin.clear();
			}
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			// Calculate the probability of being rejected at each time bin
			for (Integer timeBin : numberOfSubmissionsPerTimeBin.keySet()) {
				double probability = numberOfRejectionsPerTimeBin.getOrDefault(timeBin, new MutableInt()).doubleValue() /
						numberOfSubmissionsPerTimeBin.get(timeBin).doubleValue();
				// Apply exponential discount
				probability = learningRate * probability + (1 - learningRate) * probabilityOfRejectionPerTimeBin.getOrDefault(timeBin, 0.);
				probabilityOfRejectionPerTimeBin.put(timeBin, probability);
			}
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			if (event.getMode().equals(TransportMode.drt)) {
				int timeBin = getTimeBin(event.getTime());
				numberOfSubmissionsPerTimeBin.computeIfAbsent(timeBin, c -> new MutableInt()).increment();

				// Add a cost for potential rejection
				double extraScore = (-1) * rejectionCost * probabilityOfRejectionPerTimeBin.getOrDefault(getTimeBin(event.getTime()), 0.);
				events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), extraScore, "Potential_of_being_rejected"));
			}
		}

		private int getTimeBin(double time) {
			return (int) (Math.floor(time / timeBinSize));
		}

	}

}
