package playground.ikaddoura.busCorridor.version3_controlerListenerTestLegScore;

/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

/**
 * @author Ihab
 *
 */

public class MyControler {
	
	public static void main(final String[] args) {
					
			String configFile = "../../shared-svn/studies/ihab/busCorridor/input_version3/config_busline.xml";
			Controler controler = new Controler(configFile);
			controler.setOverwriteFiles(true);
			controler.addControlerListener(new MyControlerListener());
			
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();	
			planCalcScoreConfigGroup.setTravelingPt_utils_hr(-5);
			planCalcScoreConfigGroup.setMarginalUtilityOfMoney(-5);
			
			TestScoringFunctionFactory scoringfactory = new TestScoringFunctionFactory(planCalcScoreConfigGroup);
			controler.setScoringFunctionFactory(scoringfactory);
			controler.run();
		}
}
