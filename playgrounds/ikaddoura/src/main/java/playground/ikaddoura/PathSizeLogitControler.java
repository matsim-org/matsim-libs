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


import org.apache.log4j.Logger;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover.Builder;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.IOException;

/**
 * @author ikaddoura
 *
 */
public class PathSizeLogitControler {
	
	private static final Logger log = Logger.getLogger(PathSizeLogitControler.class);
	
	static String configFile;
	static double actTimeParam;
	static boolean pathSizeLogit = false;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			pathSizeLogit = Boolean.parseBoolean(args[1]);		
			log.info("pathSizeLogit: "+ pathSizeLogit);
			
			actTimeParam = Double.valueOf(args[2]);		
			log.info("actTimeParameter: "+ actTimeParam);
			
		} else {
			configFile = "../../runs-svn/pathSizeLogit/config_n_60_1000_2.xml";
			actTimeParam = 10.;
			pathSizeLogit = true;
		}
		
		PathSizeLogitControler main = new PathSizeLogitControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.getConfig().controler().setCreateGraphs(false);

        if (pathSizeLogit){
			
			final Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
			builder.setActTypeWeight(0.);
			builder.setLocationWeight(0.);
			builder.setSameModePenalty(0.);
			builder.setSameRoutePenalty(0.);
			builder.setActTimeParameter(actTimeParam);

			// this is the new syntax; not yet extensively tested: kai, aug'14
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					if (getConfig().strategy().getPlanSelectorForRemoval().equals("divGenPlansRemover")) {
						bindPlanSelectorForRemoval().toProvider(builder);
					}
				}
			});
			controler.getConfig().strategy().setPlanSelectorForRemoval("divGenPlansRemover");

		}
		
		controler.run();
	}
}
	
