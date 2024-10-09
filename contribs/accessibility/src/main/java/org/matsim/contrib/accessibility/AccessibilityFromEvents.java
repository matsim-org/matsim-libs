package org.matsim.contrib.accessibility;

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripDistanceBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.routing.DefaultDrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DecideOnLinkAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingModuleProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.timing.TimeInterpretationModule;

import java.util.*;

/**
 * <code>
 *       AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario, eventsFile ) ;
 *       builder....
 *       builder.build().run() ;
 * </code>
 */
public final class AccessibilityFromEvents{
	public static final class Builder {
		private Scenario scenario;
		private String eventsFile;
		private final List<FacilityDataExchangeInterface> dataListeners = new ArrayList<>() ;
		public Builder( Scenario scenario, String eventsFile ) {
			this.scenario = scenario;
			this.eventsFile = eventsFile;
		}
		public void addDataListener( FacilityDataExchangeInterface dataListener ) {
			dataListeners.add( dataListener ) ;
		}
		public AccessibilityFromEvents build() {
			return new AccessibilityFromEvents( scenario, eventsFile, dataListeners ) ;
		}
	}

	private final Scenario scenario;
	private final String eventsFile;
	private final List<FacilityDataExchangeInterface> dataListeners ;

	private AccessibilityFromEvents( Scenario scenario, String eventsFile, List<FacilityDataExchangeInterface> dataListeners) {
		this.scenario = scenario;
		this.eventsFile = eventsFile;
		this.dataListeners = dataListeners;
	}

	public void run() {
		LinkedHashMap<String, TravelTime> map = new LinkedHashMap<>(  ) ;
		EventsManager events = EventsUtils.createEventsManager();
		for( String mode : scenario.getConfig().routing().getNetworkModes() ){
			TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder( scenario.getNetwork() );
			builder.setCalculateLinkTravelTimes( true );
			builder.setCalculateLinkToLinkTravelTimes( false );
			builder.setAnalyzedModes( Collections.singleton( mode ) );
			TravelTimeCalculator ttCalculator = builder.build() ;
			map.put( mode, ttCalculator.getLinkTravelTimes() ) ;
			events.addHandler( ttCalculator );
		}
		new MatsimEventsReader(events).readFile( eventsFile );

		List<AbstractModule> modules = new ArrayList<>();


		{
			AbstractModule module = new AbstractModule(){
				@Override public void install(){

					bind( DrtEstimator.class ).toInstance(new DirectTripDistanceBasedDrtEstimator.Builder()
											      .setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(103.34) )
											      .setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, 0.0) )
											      .setRideDurationEstimator(new ConstantRideDurationEstimator(0.1087, 47.84) ) // TODO: I'm abusing this method a bit. It's supposed to calculate drt ride duration based on car ride duration; in my case it is based on car ride **distance**
											      .setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, 0.0))
											      .build() );


					install( new TimeInterpretationModule() );
					// has to do with config

					install( new ScenarioByInstanceModule( scenario ) );
					// (= scenario)

//				install( new NewControlerModule() );
					// (= some controler infrastructure because in particular dvrp wants it)

					bind( OutputDirectoryHierarchy.class ).asEagerSingleton();

					//TODO: Needed to bind DRT Estimator
//				bind(DrtEstimator).toInstance()

					install( new TripRouterModule() );
					// (= installs the trip router.  This includes (based on the config settings) installing everything that is needed
					// for: teleportation routers, network routers, pt routers.)

					for( String mode : getConfig().routing().getNetworkModes() ){
						addTravelTimeBinding( mode ).toInstance( map.get( mode ) );
					}
					// (= sets the network travel times which are needed for the TripRouterModule)

					install( new TravelDisutilityModule() );
					// (= installs the travel disuility which is necessary for routing.  The travel times are constructed earlier "by hand".)

					//				install(new EventsManagerModule());
//				install(new DvrpModule());
//
//
////				install(new MultiModeDrtModule());
////				install(new MultiModeDrtCompanionModule());


//
					// install the accessiblity module:
					{
						final AccessibilityModule module = new AccessibilityModule();
						for( FacilityDataExchangeInterface dataListener : dataListeners ){
							module.addFacilityDataExchangeListener( dataListener );
						}
						install( module );
					}
				}
			};
			modules.add( module );

			DrtOptimizationConstraintsSet optimizationConstraintsSet = drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();


			MapBinder<String, DvrpRoutingModule.AccessEgressFacilityFinder> facilityFinders;
			facilityFinders.addBinding( "drt" ).toInstance( new ClosestAccessEgressFacilityFinder(
					optimizationConstraintsSet.maxWalkDistance,
//					getter.get( Network.class ),
					scenario.getNetwork(),
					QuadTrees.createQuadTree(
							getter.getModal( DrtStopNetwork.class ).getDrtStops().values()
								)
			) );
		}

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			DrtOptimizationConstraintsSet optimizationConstraintsSet = drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();

			AbstractModule newModule = new AbstractModule(){
				@Override public void install(){

					abc = createDrtStopNetworkFromTransitSchedule(config, drtCfg);


					bind(DrtStopNetwork.class).toProvider(new DrtModeRoutingModule.DrtStopNetworkProvider(getConfig(), drtCfg) ).asEagerSingleton();




					TypeLiteral<String> keyType = TypeLiteral.get( String.class );
					TypeLiteral<DvrpRoutingModule.AccessEgressFacilityFinder> valueType = new TypeLiteral<DvrpRoutingModule.AccessEgressFacilityFinder>(){};

					MapBinder<String, DvrpRoutingModule.AccessEgressFacilityFinder> facilityFinders = MapBinder.newMapBinder( this.binder(), keyType, valueType );

					facilityFinders.addBinding( "drt" ).toInstance( new ClosestAccessEgressFacilityFinder(
							optimizationConstraintsSet.maxWalkDistance,
							scenario.getNetwork(),

							QuadTrees.createQuadTree(
									getter.getModal( DrtStopNetwork.class ).getDrtStops().values()
										)

					) );
				}
			};
			modules.add( newModule );

			AbstractDvrpModeModule modeModule = new AbstractDvrpModeModule( drtCfg.getMode() ){
				@Override public void install(){

					bindModal( DrtRouteConstraintsCalculator.class ).toProvider(modalProvider( getter -> new DefaultDrtRouteConstraintsCalculator(
							drtCfg, getter.getModal( ConstraintSetChooser.class )) ) ).in( Singleton.class );
					DrtOptimizationConstraintsSet optimizationConstraintsSet = drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
					bindModal(ConstraintSetChooser.class).toProvider(
							() -> (departureTime, accessActLink, egressActLink, person, tripAttributes)
									      -> Optional.of(optimizationConstraintsSet)
											).in(Singleton.class);

					switch( drtCfg.operationalScheme ){
						case door2door -> bindModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ).toProvider(
																		 modalProvider( getter -> new DecideOnLinkAccessEgressFacilityFinder( getter.getModal( Network.class ) ) ) )
																 .asEagerSingleton();
						case stopbased, serviceAreaBased -> {
							bindModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ).toProvider( modalProvider(
																       getter -> new ClosestAccessEgressFacilityFinder(
																		       optimizationConstraintsSet.maxWalkDistance,
																		       getter.get( Network.class ),
																		       QuadTrees.createQuadTree( getter.getModal( DrtStopNetwork.class ).getDrtStops().values() ) ) ) )
														       .asEagerSingleton();
						}
						default -> throw new IllegalStateException( "Unexpected value: " + drtCfg.operationalScheme );
					}
				}

//				bindModal( DvrpRoutingModule.class).toProvider( DvrpRoutingModuleProvider.class );


			};

			modules.add( modeModule );

		}

		com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , modules.toArray( new AbstractModule[0] ) ) ;

		// The following is more awkward than it should be because of Java type erasure; essentially, it says ...getInstance( Set<ControlerListener>.class ):
		Set<ControlerListener> result = injector.getInstance( Key.get( new TypeLiteral<Set<ControlerListener>>(){} ) );

		// We do not have named ControlerListeners, so we have to go through all of them (if the AccessibilityListener had a class name, then one might
		// be able to get around that):
		for( ControlerListener controlerListener : result ) {
			if ( controlerListener instanceof ShutdownListener ) {
				MatsimServices controler = null ;
				boolean unexpected = false ;
				ShutdownEvent shutdownEvent = new ShutdownEvent( controler, unexpected, 0 ) ;
				((ShutdownListener) controlerListener).notifyShutdown( shutdownEvent );
			}
		}
	}

	//	static class StopFinderProvider extends ModalProviders.AbstractProvider<DvrpMode, DvrpRoutingModule.AccessEgressFacilityFinder > {
//
//		StopFinderProvider( String mode ){
//			super( mode, DvrpModes::mode );
//		}
//
//		@Override public DvrpRoutingModule.AccessEgressFacilityFinder get(){
//			return getModalInstance( DvrpRoutingModule.AccessEgressFacilityFinder.class );
//		}
//
//	}

}
