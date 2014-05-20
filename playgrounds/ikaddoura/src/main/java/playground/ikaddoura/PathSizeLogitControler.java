/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura;


import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;

import playground.vsp.planselectors.DiversityGeneratingPlansRemover;
import playground.vsp.planselectors.DiversityGeneratingPlansRemover.Builder;

/**
 * @author ikaddoura
 *
 */
public class PathSizeLogitControler {
	
	private static final Logger log = Logger.getLogger(PathSizeLogitControler.class);
	
	static String configFile;
	static double endTimeWeight;
	static boolean pathSizeLogit = false;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			pathSizeLogit = Boolean.parseBoolean(args[1]);		
			log.info("pathSizeLogit: "+ pathSizeLogit);
			
			endTimeWeight = Double.valueOf(args[2]);		
			log.info("endTimeWeight: "+ endTimeWeight);
			
		} else {
			configFile = "../../runs-svn/pathSizeLogit/config_n_60_1000_2.xml";
			endTimeWeight = 1.;
			pathSizeLogit = true;
		}
		
		PathSizeLogitControler main = new PathSizeLogitControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		final Network network = controler.getNetwork();
		
		if (pathSizeLogit){
			
			Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
			builder.setActTypeWeight(0.);
			builder.setLocationWeight(0.);
			builder.setSameModePenalty(0.);
			builder.setSameRoutePenalty(0.);
			builder.setActTimeParameter(10.);
			
			final AbstractPlanSelector remover = builder.build(network) ;
			
			controler.addControlerListener(new StartupListener(){
				@Override
				public void notifyStartup(StartupEvent event) {
					event.getControler().getStrategyManager().setPlanSelectorForRemoval(remover);
				}
			});
		}
		
		controler.run();
	}
}
	
