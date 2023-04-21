package org.matsim.contrib.drt.run.examples;

import com.google.inject.Inject;
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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.*;

public class DrtAbortTest{
	private static final Logger log = LogManager.getLogger(DrtAbortTest.class );

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	static final String walkAfterRejectMode = "walkAfterReject";
	@Test
	public void testAbortHandler() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("mielec" ), "mielec_drt_config.xml" );
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup() );

		config.controler().setLastIteration(1);
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
		for ( DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config ).getModalElements()) {
		//relatively high max age to prevent rejections
//			var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
//			drtRequestInsertionRetryParams.maxRequestAge = 7200;
//			drtCfg.addParameterSet(drtRequestInsertionRetryParams);
			// I don't know what the above does; might be useful to understand.

			drtCfg.maxTravelTimeAlpha = 1.;
			drtCfg.maxTravelTimeBeta = 0.;
			drtCfg.maxWaitTime = 1.;
			drtCfg.stopDuration = 1.;
			// (Trying to force abort(s); can't say if this is the correct syntax.  kai, apr'23)

		}


		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> components = qsimComponents.getActiveComponents();
		components.add( MyAbortHandler.COMPONENT_NAME );
		qsimComponents.setActiveComponents( components );

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get( config );
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute() );

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config );
		ScenarioUtils.loadScenario(scenario );

		// reduce to one person:
		{
			List<Id<Person>> keys = new ArrayList<>( scenario.getPopulation().getPersons().keySet() );
			keys.remove( 0 );
			for( Id<Person> personId : keys ){
				scenario.getPopulation().removePerson( personId );
			}
		}

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule() );
		controler.addOverridingModule(new MultiModeDrtModule() );
		controler.configureQSimComponents( DvrpQSimComponents.activateAllModes(multiModeDrtConfig ) );

		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.addQSimComponentBinding( MyAbortHandler.COMPONENT_NAME ).to( MyAbortHandler.class );
			}
		} );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(DrtRejectionHandler.class).toInstance(new DrtRejectionHandler());
				addEventHandlerBinding().to(DrtRejectionHandler.class);
				addControlerListenerBinding().to(DrtRejectionHandler.class);
			}
		});

		controler.run();

		// yy I cannot say if the expected status is useful here.  kai, apr'23

		var expectedStats = RunDrtExampleIT.Stats.newBuilder()
							 .rejectionRate(1.0)
							 .rejections(1)
							 .waitAverage(Double.NaN)
							 .inVehicleTravelTimeMean(Double.NaN)
							 .totalTravelTimeMean(Double.NaN)
							 .build();

		RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}


	private static class MyAbortHandler implements AbortHandler, MobsimEngine{
		public static final String COMPONENT_NAME = "DrtAbortHandler";
		@Inject Network network;
		@Inject Population population;
		@Inject MobsimTimer mobsimTimer;
		private InternalInterface internalInterface;
		private List<MobsimAgent> agents = new ArrayList<>();

		@Override public boolean handleAbort( MobsimAgent agent ){

			log.warn("need to handle abort of agent=" + agent );

			final String drtMode = "drt";

			PopulationFactory pf = population.getFactory();

			if ( agent.getMode().equals( drtMode ) ) {
				// yyyyyy this will have to work for all drt modes!!!

				Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );
				log.warn("\n current plan=" + plan );
				for( PlanElement planElement : plan.getPlanElements() ){
					log.warn( planElement );
				}

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
					secondLeg.setTravelTime( 22 ); // yyyy needs to be replaced by something meaningful
					secondLeg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class, interactionLink, originallyPlannedDestinationLink ) );
					plan.getPlanElements().add( index+2, secondLeg );

				}
				// (4) reset the agent caches:
				WithinDayAgentUtils.resetCaches( agent );

				// (5) add the agent to an internal list, which is processed during doSimStep, which formally ends the current
				// (aborted) leg, and moves the agent forward in its state machine.
				agents.add( agent );

				log.warn("\n plan after splicing=" + plan);
				for( PlanElement planElement : plan.getPlanElements() ){
					log.warn( planElement );
				}
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
	}

	private static class DrtRejectionHandler implements PassengerRequestRejectedEventHandler,
			PassengerRequestSubmittedEventHandler, IterationEndsListener {
		private final Map<Integer, MutableInt> numberOfRejectionsPerTimeBin = new HashMap<>();
		private final Map<Integer, MutableInt> numberOfSubmissionsPerTimeBin = new HashMap<>();
		private final Map<Integer, Double> probabilityOfRejectionPerTimeBin = new HashMap<>();
		private final double timeBinSize = 900;
		private final double rejectionCost = 12;
		// 12 -> 2 hour of default performing score

		@Inject
		private EventsManager events;

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			if (event.getMode().equals(TransportMode.drt)) {
				//TODO use drt config group to get mode
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

		private int getTimeBin(double time) {
			return (int) (Math.floor(time / timeBinSize));
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			// Calculate the probability of being rejected at each time bin
			for (Integer timeBin : numberOfSubmissionsPerTimeBin.keySet()) {
				double probability = numberOfRejectionsPerTimeBin.getOrDefault(timeBin, new MutableInt()).doubleValue() /
						numberOfSubmissionsPerTimeBin.get(timeBin).doubleValue();
				probabilityOfRejectionPerTimeBin.put(timeBin, probability);
			}
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			if (event.getMode().equals(TransportMode.drt)) {
				//TODO use drt config group to get mode
				int timeBin = getTimeBin(event.getTime());
				numberOfSubmissionsPerTimeBin.computeIfAbsent(timeBin, c -> new MutableInt()).increment();

				// Add a cost for potential rejection
				double extraScore = (-1) * rejectionCost * probabilityOfRejectionPerTimeBin.getOrDefault(getTimeBin(event.getTime()), 0.0);
				events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), extraScore, "Potential_of_being_rejected"));
			}
		}
	}

}
