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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.KeepLastExecutedAsPlanStrategy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.withinday.controller.ExecutedPlansServiceImpl;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

/**
 * @author nagel
 *
 */
public class KNAccidentScenario {
	private static final Logger log = Logger.getLogger(KNAccidentScenario.class) ;
	
	@SuppressWarnings("unused")
	private static final String KEEP_LAST_EXECUTED = "keepLastExecuted" ;

	static final class MyIterationCounter implements IterationStartsListener {
		private int iteration;

		@Inject MyIterationCounter( ControlerListenerManager cm) {
			cm.addControlerListener(this);
		}

		@Override public void notifyIterationStarts(IterationStartsEvent event) {
			this.iteration = event.getIteration() ;
			log.warn("got notified; iteration=" + this.iteration ) ;
		}

		final int getIteration() {
			return this.iteration;
		}
	}

	static final Id<Link> accidentLinkId = Id.createLinkId( "4706699_484108_484109-4706699_484109_26662372");
	static List<Id<Link>> replanningLinkIds = new ArrayList<>() ; 

	public static void main(String[] args) {
		replanningLinkIds.add( Id.createLinkId("4068014_26836040_26836036-4068014_26836036_251045850-4068014_251045850_251045852") ) ;

		// ===

		final Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin/telematics/funkturm-example/baseconfig.xml") ;

		config.network().setInputFile("../../counts/iv_counts/network.xml.gz");
		config.network().setTimeVariantNetwork(true);

		config.plans().setInputFile("reduced-plans.xml.gz");
		config.plans().setRemovingUnneccessaryPlanAttributes(true);

		config.controler().setFirstIteration(9);
		config.controler().setLastIteration(19);
		config.controler().setOutputDirectory("./output/telematics/funkturm-example");
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);

		config.qsim().setFlowCapFactor(0.04);
		config.qsim().setStorageCapFactor(0.06);
		config.qsim().setStuckTime(100.);
		config.qsim().setStartTime(6.*3600.);
		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);

		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}
		{
			ModeParams params = new ModeParams( "undefined" ) ;
			config.planCalcScore().addModeParams(params);
		}

		StrategySettings stratSets = new StrategySettings() ;
		//		stratSets.setStrategyName(DefaultSelector.KeepLastSelected.name());
		stratSets.setStrategyName(KEEP_LAST_EXECUTED);
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings(stratSets);

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		config.vspExperimental().setWritingOutputEvents(true);

		OTFVisConfigGroup otfConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
		otfConfig.setAgentSize(200);

		// ===

		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		preparePopulation(scenario);
		scheduleAccident(scenario); 

		Link link = scenario.getNetwork().getLinks().get( Id.createLinkId( "-418375_-248_247919764" ) ) ;
		link.setCapacity(2000.); // "repair" a capacity. (This is Koenigin-Elisabeth-Str., parallel to A100 near Funkturm, having visibly less capacity than links upstream and downstream.)

		Link link2 = scenario.getNetwork().getLinks().get( Id.createLinkId("40371262_533639234_487689293-40371262_487689293_487689300-40371262_487689300_487689306-40371262_487689306_487689312-40371262_487689312_487689316-40371262_487689316_487689336-40371262_487689336_487689344-40371262_487689344_487689349-40371262_487689349_487689356-40371262_487689356_533639223-4396104_533639223_487673629-4396104_487673629_487673633-4396104_487673633_487673636-4396104_487673636_487673640-4396104_487673640_26868611-4396104_26868611_484073-4396104_484073_26868612-4396104_26868612_26662459") ) ;
		link2.setCapacity( 300. ) ; // reduce cap on alt route. (This is the freeway entry link just downstream of the accident; reducing its capacity
		// means that the router finds a wider variety of alternative routes.)

		// ===

		final Controler controler = new Controler( scenario ) ;
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles ) ;


		Set<String> analyzedModes = new HashSet<>() ;
		analyzedModes.add( TransportMode.car ) ;
		final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
		
//		final MyTravelTime travelTime = new MyTravelTime(scenario) ;

		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				MobsimDataProvider mdp = new MobsimDataProvider() ;
				bind( MobsimDataProvider.class ).toInstance( mdp ) ;
				addMobsimListenerBinding().toInstance( mdp ) ;	

				bind( ExecutedPlansServiceImpl.class ).asEagerSingleton(); 
				addControlerListenerBinding().to( ExecutedPlansServiceImpl.class ) ;
//				
				addPlanStrategyBinding(KEEP_LAST_EXECUTED).toProvider(KeepLastExecutedAsPlanStrategy.class) ;

				this.bind( MyIterationCounter.class ).asEagerSingleton();
				

				// ===
				
				this.addEventHandlerBinding().toInstance( travelTime ) ;
				this.addMobsimListenerBinding().toInstance( travelTime );
				this.bind( TravelTime.class ).toInstance( travelTime );
				
				// ---
				// These are the possible strategies.  They have various pre-requisites.
				this.addMobsimListenerBinding().to( ManualDetour.class ) ;
//				this.addMobsimListenerBinding().to( WithinDayBangBangMobsimListener.class );
//				this.addMobsimListenerBinding().to( WithinDayReRouteMobsimListener.class );

			}
		}) ;



		// ===

		controler.run() ;

	}

	private static void scheduleAccident(final Scenario scenario) {
		List<NetworkChangeEvent> events = new ArrayList<>() ;
		{
			NetworkChangeEvent event = new NetworkChangeEvent(8*3600.) ;
			event.addLink( scenario.getNetwork().getLinks().get( accidentLinkId ) ) ;
			ChangeValue change = new ChangeValue( ChangeType.FACTOR, 0.1 ) ;
			event.setFlowCapacityChange(change);
			ChangeValue lanesChange = new ChangeValue( ChangeType.FACTOR, 0.1 ) ;
			event.setLanesChange(lanesChange);
			events.add(event) ;
		}
		{
			NetworkChangeEvent event = new NetworkChangeEvent(9*3600.) ;
			event.addLink( scenario.getNetwork().getLinks().get( accidentLinkId ) );
			ChangeValue change = new ChangeValue( ChangeType.FACTOR, 10. ) ;
			event.setFlowCapacityChange(change);
			ChangeValue lanesChange = new ChangeValue( ChangeType.FACTOR, 10. ) ;
			event.setLanesChange(lanesChange);
			events.add(event) ;
		}
		final List<NetworkChangeEvent> events1 = events;
		NetworkUtils.setNetworkChangeEvents(((Network) scenario.getNetwork()),events1);
	}

	private static void preparePopulation(final Scenario scenario) {
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
	}

}
