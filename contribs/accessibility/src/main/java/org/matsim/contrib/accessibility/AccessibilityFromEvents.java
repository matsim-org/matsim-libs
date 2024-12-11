package org.matsim.contrib.accessibility;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
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
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;

import java.util.*;

/**
 * <code>
 *       AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario, eventsFile ) ;
 *       builder....
 *       builder.build().run() ;
 * </code>
 */
public final class AccessibilityFromEvents{
	private final List<String> actTypes;

	public static final class Builder {
		private final List<String> actTypes;
		private Scenario scenario;
		private String eventsFile;
		private final List<FacilityDataExchangeInterface> dataListeners = new ArrayList<>() ;

		public Builder( Scenario scenario, String eventsFile) {
			this(scenario, eventsFile, null);
		}
		public Builder( Scenario scenario, String eventsFile, List<String> actTypes) {
			this.scenario = scenario;
			this.eventsFile = eventsFile;
			this.actTypes = actTypes;
		}


		public void addDataListener( FacilityDataExchangeInterface dataListener ) {
			dataListeners.add( dataListener ) ;
		}
		public AccessibilityFromEvents build() {
			return new AccessibilityFromEvents(scenario, eventsFile, dataListeners, actTypes);
		}
	}

	private final Scenario scenario;
	private final String eventsFile;
	private final List<FacilityDataExchangeInterface> dataListeners ;

	private AccessibilityFromEvents(Scenario scenario, String eventsFile, List<FacilityDataExchangeInterface> dataListeners, List<String> actType) {
		this.scenario = scenario;
		this.eventsFile = eventsFile;
		this.dataListeners = dataListeners;
		this.actTypes = actType;
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

		AbstractModule module = new AbstractModule(){
			@Override public void install(){
				AccessibilityConfigGroup accessibilityConfig = ConfigUtils.addOrGetModule( this.getConfig(), AccessibilityConfigGroup.class );

				install( new TimeInterpretationModule() );
				// has to do with config

				install( new ScenarioByInstanceModule( scenario ) ) ;
				// (= scenario)

//				install( new NewControlerModule() );
				// (= some controler infrastructure because in particular dvrp wants it)

				bind(OutputDirectoryHierarchy.class).asEagerSingleton();

				DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
					.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(300))
					.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, 0.4))
					.setRideDurationEstimator(new ConstantRideDurationEstimator(1.25, 300))
					.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, 0.3))
					.build();

				bind(DrtEstimator.class).toInstance(drtEstimator);

				install( new TripRouterModule() ) ;
				// (= installs the trip router.  This includes (based on the config settings) installing everything that is needed
				// for: teleportation routers, network routers, pt routers.)

				for( String mode : getConfig().routing().getNetworkModes() ){
					addTravelTimeBinding( mode ).toInstance( map.get(mode) );
				}
				// (= sets the network travel times which are needed for the TripRouterModule)

				install( new TravelDisutilityModule() ) ;
				// (= installs the travel disuility which is necessary for routing.  The travel times are constructed earlier "by hand".)


				install(new EventsManagerModule()); // is this needed?
				if ( accessibilityConfig.getIsComputingMode().contains( Modes4Accessibility.estimatedDrt ) ){


					install( new DvrpModule() );

					MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), MultiModeDrtConfigGroup.class );
					for( DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements() ){
						install( new DrtModeModule( drtCfg ) );
						installQSimModule( new DrtModeQSimModule( drtCfg ) );
						drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = Double.MAX_VALUE;
//						install( new DrtModeAnalysisModule( drtCfg ) );


						install( new AbstractDvrpModeModule( drtCfg.getMode() ){
							// (= we need to install a ModeModule so we get access to the modal material)

							@Override public void install(){
								MapBinder<String, DvrpRoutingModule.AccessEgressFacilityFinder> mapBinder = MapBinder.newMapBinder( binder(), String.class,
										DvrpRoutingModule.AccessEgressFacilityFinder.class );
								// (this is just the "normal" map binder)

								// There is ModalProviders.InstanceGetter#getModal to get modal instances.  However, if one
								// looks in the internals, one finds that it is taken out of the injector, which means that it
								// can only be called _after_ the injector was constructed.  Which means that one needs to
								// program the material as (modal)provider which is lazily called only when needed.

								mapBinder.addBinding( getMode() ).toProvider(
										this.modalProvider( getter -> getter.getModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ) ) );
								// (I think that this works as follows:
								// * getter.getModal(...) takes whatever modal material is needed out of the injector.
								// * however, the provider that is bound to the mapBinder is not activated until this is really needed.
								// I think.  kai, oct'24)
							}
						} );
					}


//				install(new MultiModeDrtModule());
//				install(new MultiModeDrtCompanionModule());
				}

//
				// install the accessiblity module:
				if (actTypes == null || actTypes.isEmpty()) {
					final AccessibilityModule module = new AccessibilityModule();
					for( FacilityDataExchangeInterface dataListener : dataListeners ){
						module.addFacilityDataExchangeListener( dataListener );
					}
					install( module );
				}else {
					for(String actType : actTypes){
						final AccessibilityModule module = new AccessibilityModule();
						if (actType != null) {
							module.setConsideredActivityType(actType);
						}
						for( FacilityDataExchangeInterface dataListener : dataListeners ){
							module.addFacilityDataExchangeListener( dataListener );
						}
						install( module);
					}
				}

			}
		};

		com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , module );

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


}
