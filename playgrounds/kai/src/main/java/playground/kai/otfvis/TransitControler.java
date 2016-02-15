/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.kai.otfvis;

import com.google.inject.Provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class TransitControler {

	private static boolean useTransit = true ;
	private static boolean useOTFVis = true ;

	public static void main(final String[] args) {
		if ( args.length > 1 ) {
			useOTFVis = Boolean.parseBoolean(args[1]) ;
		}

		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).readFile(args[0]);
		if ( useTransit ) {
			config.transit().setUseTransit(true);
			//		config.otfVis().setColoringScheme( OTFVisConfigGroup.COLORING_BVG ) ;
		}

		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport ) ;
		//		config.otfVis().setShowTeleportedAgents(true) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Population pop = scenario.getPopulation();
		for ( Iterator it = pop.getPersons().entrySet().iterator() ; it.hasNext() ; ) {
			Entry entry = (Entry) it.next() ;
			Person person = (Person) entry.getValue() ;
			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe ;
					if ( act.getType().equals("pickup") || act.getType().equals("dropoff") ) {
						it.remove(); 
						break ;
					}
				}
			}
		}
		
//		Collection<Id<Person>> personIdsToRemove = new ArrayList<>() ;
//		for ( Person person : pop.getPersons().values() ) {
//			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
//				if ( pe instanceof Activity ) {
//					Activity act = (Activity) pe ;
//					if ( act.getType().equals("pickup") || act.getType().equals("dropoff") ) {
//						personIdsToRemove.add( person.getId() ) ;
//					}
//				}
//			}
//		}
//		for ( Id<Person> pid : personIdsToRemove ) {
//			pop.getPersons().remove( pid ) ;
//		}
		
		final Controler controler = new Controler(scenario) ;
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles ) ;
//		controler.setDirtyShutdown(true);

		//		Logger.getLogger("main").warn("warning: using randomized pt router!!!!") ;
		//		tc.addOverridingModule(new RandomizedTransitRouterModule());

		if ( useOTFVis ) {

			OTFVisConfigGroup otfconfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
			// (this should also materialize material from a config.xml--? kai, nov'15)
			
			otfconfig.setDrawTransitFacilityIds(false);
			otfconfig.setDrawTransitFacilities(false);
			controler.addOverridingModule( new OTFVisLiveModule() );
		}

		// the following is only possible when constructing the mobsim yourself:
		//			if(this.useHeadwayControler){
		//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
		//				this.events.addHandler(new FixedHeadwayControler(simulation));		
		//			}

//		controler.addOverridingModule(new OTFVisFileWriterModule());
		//		tc.setCreateGraphs(false);
		
		ActivityParams params = new ActivityParams("pt interaction") ;
		params.setScoringThisActivityAtAll(false);
		controler.getConfig().planCalcScore().addActivityParams(params);

		controler.run();
	}

}
