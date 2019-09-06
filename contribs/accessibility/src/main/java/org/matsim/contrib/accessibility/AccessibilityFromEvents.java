package org.matsim.contrib.accessibility;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

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
		for( String mode : scenario.getConfig().plansCalcRoute().getNetworkModes() ){
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
				install( new ScenarioByInstanceModule( scenario ) ) ;
				install( new TripRouterModule() ) ;
				for( String mode : getConfig().plansCalcRoute().getNetworkModes() ){
					addTravelTimeBinding( mode ).toInstance( map.get(mode) );
				}
				install( new TravelDisutilityModule() ) ;
				final AccessibilityModule module = new AccessibilityModule();
				for( FacilityDataExchangeInterface dataListener : dataListeners ){
					module.addFacilityDataExchangeListener( dataListener );
				}
				install( module ) ;
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
				ShutdownEvent shutdownEvent = new ShutdownEvent( controler, unexpected ) ;
				((ShutdownListener) controlerListener).notifyShutdown( shutdownEvent );
			}
		}
	}
}
