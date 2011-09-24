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
package playground.ikaddoura.busCorridor;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author Ihab
 *
 */

public class MyControler_2 {
	public static void main(final String[] args) {
			
			String configFile = "daten/studienarbeit/input/config_busline_3.xml";
			Config config = ConfigUtils.loadConfig(configFile);
			Controler controler = new Controler(config);
			controler.setOverwriteFiles(true);
			
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();			
			
			BusCorridorCharyparNagelScoringFunctionFactory factory = new BusCorridorCharyparNagelScoringFunctionFactory(planCalcScoreConfigGroup);
			controler.setScoringFunctionFactory(factory);
			controler.run();
		}
}
