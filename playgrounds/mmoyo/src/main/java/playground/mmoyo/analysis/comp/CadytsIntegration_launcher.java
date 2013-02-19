/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.pt.CadytsContext;
import org.matsim.contrib.cadyts.pt.CadytsPtPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author manuel
 * 
 * invokes a standard MATSim transit simulation
 */
public class CadytsIntegration_launcher {
	
	public static void main(String[] args) {
		String configFile; 
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../../ptManuel/calibration/my_config.xml";
		}

		Config config = null;
		config = ConfigUtils.loadConfig(configFile);
		
		int lastStrategyIdx = config.strategy().getStrategySettings().size() ;
		
		StrategySettings stratSets = new StrategySettings(new IdImpl(lastStrategyIdx+1));
		stratSets.setModuleName("myCadyts");
		stratSets.setProbability(0.1);
		config.strategy().addStrategySettings(stratSets);

		final Controler controler = new Controler(config);

		final CadytsContext context = new CadytsContext( config ) ;
		controler.setOverwriteFiles(true);
		controler.addControlerListener(context);
		controler.addPlanStrategyFactory("myCadyts", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager events) {
				return new PlanStrategyImpl(new CadytsPtPlanChanger( context));
			}
		});
		
		controler.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				ObjectAttributes attrs = new ObjectAttributes() ;
				
				Population pop = event.getControler().getPopulation() ;
				for ( Person person : pop.getPersons().values() ) {
					int cnt = 0 ;
					for ( Plan plan : person.getPlans() ) {
						Double cadytsCorrection = 0. ;
						if ( plan.getCustomAttributes() != null ) {
							cadytsCorrection = (Double) plan.getCustomAttributes().get(CadytsPtPlanChanger.CADYTS_CORRECTION) ;
							attrs.putAttribute( person.getId().toString()+Integer.toString(cnt) , CadytsPtPlanChanger.CADYTS_CORRECTION, cadytsCorrection) ;
						}
						System.err.println( " personId: " + person.getId() + " planScore: " + plan.getScore()  + " cadytsCorrection: " + cadytsCorrection ) ; 
						cnt++ ;
					}
				}
				OutputDirectoryHierarchy hhh = event.getControler().getControlerIO() ;
				new ObjectAttributesXmlWriter(attrs).writeFile(hhh.getIterationFilename(event.getIteration(), "cadytsCorrections.xml") ) ;
			}
		}) ;

		controler.run();
	} 
}
