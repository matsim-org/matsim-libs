/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.ikaddoura.berlin;


import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;

import java.io.IOException;

/**
 * @author ikaddoura
 *
 */
public class IKControler_Berlin {
	
	private static final Logger log = Logger.getLogger(IKControler_Berlin.class);
	
	static String configFile;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
		} else {
			configFile = "/Users/Ihab/Desktop/Scenarios/Berlin/config.xml";
		}
		
		IKControler_Berlin main = new IKControler_Berlin();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		controler.getConfig().planCalcScore().addActivityParams(transitActivityParams);

        controler.setScoringFunctionFactory(new BvgScoringFunctionFactory(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroup(controler.getConfig()), controler.getScenario().getNetwork()));
		controler.run();
	}
}
	
