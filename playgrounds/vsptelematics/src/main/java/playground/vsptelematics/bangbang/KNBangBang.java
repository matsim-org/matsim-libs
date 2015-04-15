/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsptelematics.bangbang;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author nagel
 *
 */
public class KNBangBang {

	static private final Id<Link> accidentLinkId = Id.createLinkId( "4706699_484108_484109-4706699_484109_26662372");
	static List<Id<Link>> replanningLinkIds = new ArrayList<>() ; 

	private static final class KNMobsimProvider implements Provider<Mobsim> {
		@Inject private Scenario scenario;
		@Inject private EventsManager events ;
		@Inject private Provider<TripRouter> tripRouterFactory;

		@Override
		public Mobsim get() {
			QSim qsim = QSimUtils.createDefaultQSim( scenario, events ) ;
			
			qsim.addQueueSimulationListeners( new KNWithinDayMobsimListener(this.tripRouterFactory.get()));
			
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),scenario, events, qsim);
			OTFClientLive.run(scenario.getConfig(), server);
//			server.getOTFVisConfig().setColoringScheme( ColoringScheme.telematics );
			
			return qsim ;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		replanningLinkIds.add( Id.createLinkId("4068014_26836040_26836036-4068014_26836036_251045850-4068014_251045850_251045852") ) ;

		// ---
		
		Config config = ConfigUtils.loadConfig("/Users/nagel/kairuns/telematics/baseconfig.xml") ;
		
		config.network().setInputFile("/Users/nagel/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz");
		config.network().setTimeVariantNetwork(true);
		
//		config.plans().setInputFile("/Users/nagel/kairuns/a100/7dec13-base/output_plans.xml.gz");
		config.plans().setInputFile("/Users/nagel/kairuns/telematics/reduced-plans.xml.gz");
		
		config.controler().setFirstIteration(9);
		config.controler().setLastIteration(9);
		config.controler().setOutputDirectory("/Users/nagel/kairuns/telematics/output");
		
		config.qsim().setFlowCapFactor(0.04);
		config.qsim().setStorageCapFactor(0.06);
		config.qsim().setStuckTime(100.);
		config.qsim().setStartTime(5.*3600.);
		
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.WARN );
		
		// ---
		
		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		for ( Iterator<? extends Person> it = scenario.getPopulation().getPersons().values().iterator() ; it.hasNext() ; ) {
			Person person = it.next() ;
			boolean retain = false ;
			for ( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
				if ( leg.getRoute() instanceof NetworkRoute ) {
					NetworkRoute route = (NetworkRoute) leg.getRoute() ;
					if ( route.getLinkIds().contains( accidentLinkId ) ) {
						retain = true ;
					}
				}
			}
			if ( !retain ) {
				it.remove(); 
			}
		}
		
		List<NetworkChangeEvent> events = new ArrayList<>() ;
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(8*3600.) ;
			event.addLink( scenario.getNetwork().getLinks().get( accidentLinkId ) ) ;
			ChangeValue change = new ChangeValue( ChangeType.FACTOR, 0.1 ) ;
			event.setFlowCapacityChange(change);
			ChangeValue lanesChange = new ChangeValue( ChangeType.FACTOR, 0.1 ) ;
			event.setLanesChange(lanesChange);
			events.add(event) ;
		}
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(9*3600.) ;
			event.addLink( scenario.getNetwork().getLinks().get( accidentLinkId ) );
			ChangeValue change = new ChangeValue( ChangeType.FACTOR, 10. ) ;
			event.setFlowCapacityChange(change);
			ChangeValue lanesChange = new ChangeValue( ChangeType.FACTOR, 10. ) ;
			event.setLanesChange(lanesChange);
			events.add(event) ;
		}
		((NetworkImpl) scenario.getNetwork()).setNetworkChangeEvents(events); 
		
		// ---
		
		final Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true);
		controler.setDirtyShutdown(true);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindMobsim().toProvider(KNMobsimProvider.class) ;
			}
		});

		Set<String> analyzedModes = new HashSet<>() ;
		analyzedModes.add( TransportMode.car ) ;
		final TravelTime travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(controler.getScenario(), analyzedModes);
		controler.getEvents().addHandler((TravelTimeCollector) travelTime);
		controler.getMobsimListeners().add((TravelTimeCollector) travelTime);
		
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install() {
				this.bind( TravelTime.class ).toInstance( travelTime );
			}
		} ) ;
		
		// ---
		
		controler.run() ;
		
	}

}
