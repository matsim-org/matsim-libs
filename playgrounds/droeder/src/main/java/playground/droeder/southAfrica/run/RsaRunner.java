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

package playground.droeder.southAfrica.run;

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
import playground.droeder.southAfrica.routing.PtSubModeTripRouterFactory;


/**
 * @author droeder
 *
 */
public class RsaRunner {
	
	
	private final static Logger log = Logger.getLogger(RsaRunner.class);
	private static String CONFIGFILE = 
			"E:/VSP/svn/droeder/southAfrica/test/configReRouteFixedSubMode.xml";
//			"E:/rsa/test/configRSAtest.xml";
	
	private static boolean FIXEDSUBMODE = true;
	
	public static void main(final String[] args) {
		String configFile = null;
		Boolean fixedSubMode = null;
		
		if(args.length == 0){
			if(new File(CONFIGFILE).exists()){
				configFile = CONFIGFILE;
				fixedSubMode = FIXEDSUBMODE;
			}else{
				log.error("no config Found...");
				System.exit(1);
			}
		}else if(args.length == 1){
			configFile = args[0];
			fixedSubMode = Boolean.parseBoolean(args[1]);
		}else{
			log.error("no config Found...");
			System.exit(1);
		}
		
				
		sim(configFile, fixedSubMode);
		
	}

	
	private static void sim(String conf, boolean fixedSubMode) {
		
		
		Config config = new Config();
		config.addModule(PConfigGroup.GROUP_NAME, new PConfigGroup());
		ConfigUtils.loadConfig(config, conf);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(conf));
	
		Controler controler = new PtSubModeControler(scenario, fixedSubMode);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		PHook pFact = new PHook(controler, new Mode2LineSetterRSA(), (PTransitRouterFactory) controler.getTransitRouterFactory(), null, PtSubModeTripRouterFactory.class);
		controler.addControlerListener(pFact);		
		
		//necessary because PHook overwrites setting, made in PtSubModeControler-c'tor
		controler.setMobsimFactory(new TransitSubModeQSimFactory(fixedSubMode));
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.setUseTripRouting(true);

		controler.run();
		

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
