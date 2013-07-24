/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.droeder.southAfrica.old.run;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.hook.PHook;
import playground.andreas.P2.hook.PTransitRouterFactory;
import playground.droeder.southAfrica.helper.Mode2LineSetterRSA;
import playground.droeder.southAfrica.qSimHook.TransitSubModeQSimFactory;


/**
 * @author droeder
 *
 */
public class RsaRunnerOld {
	
	
	private final static Logger log = Logger.getLogger(RsaRunnerOld.class);
	private static String CONFIGFILE = 
			"E:/VSP/svn/droeder/southAfrica/test/configReRouteFixedSubMode.xml";
//			"E:/rsa/test/configRSAtest.xml";
	

	public static void main(final String[] args) {
		String configFile = null;
		
		if(args.length == 0){
			if(new File(CONFIGFILE).exists()){
				configFile = CONFIGFILE;
			}else{
				log.error("no config Found...");
				System.exit(1);
			}
		}else if(args.length == 1){
			configFile = args[0];
		}else{
			log.error("no config Found...");
			System.exit(1);
		}
		
				
		sim(configFile);
		
	}

	
	private static void sim(String conf) {
		
		boolean fixedSubMode = true;
		Config config = new Config();
		config.addModule(new PConfigGroup());
		ConfigUtils.loadConfig(config, conf);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(conf));
	
		Controler controler = new PtSubModeControlerOld(scenario, fixedSubMode);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		PHook pFact = new PHook(controler, new Mode2LineSetterRSA(), (PTransitRouterFactory) controler.getTransitRouterFactory(), null, null);
		controler.addControlerListener(pFact);		
		
		//necessary because PHook overwrites setting, made in PtSubModeControler-c'tor
		controler.setMobsimFactory(new TransitSubModeQSimFactory(fixedSubMode));
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		controler.setUseTripRouting(false);
		throw new RuntimeException("setting useTripRouting to false no longer possible since this only affects initialization and my thus be " +
				"inconsistent.  kai, may'13.  aborting ... ") ;

//		controler.run();
		

	}
	
}
//class ReRouteStuckHandler implements AgentStuckEventHandler, IterationStartsListener, StartupListener{
//	
//	
//	private List<Id> agentToReRoute;
//
//	public ReRouteStuckHandler(){
//		this.agentToReRoute = new ArrayList<Id>();
//	}
//
//	@Override
//	public void reset(int iteration) {
//	}
//
//	@Override
//	public void notifyIterationStarts(IterationStartsEvent event) {
//		PlanStrategy strategy = null;
//		for(PlanStrategy ps: event.getControler().getStrategyManager().getStrategies()){
//			if(ps instanceof PlanStrategyReRoutePtFixedSubMode){
//				strategy = ps;
//				break;
//			}
//		}
//		if(!(strategy == null)){
//			Person p;
//			for(Id id: this.agentToReRoute){
//				p = event.getControler().getPopulation().getPersons().get(id);
//				strategy.run(p);
//			}
//		}
//		this.agentToReRoute.clear();
//	}
//
//	@Override
//	public void handleEvent(AgentStuckEvent event) {
//		this.agentToReRoute.add(event.getPersonId());
//	}
//
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		event.getControler().getEvents().addHandler(this);
//	}
//	
//}
