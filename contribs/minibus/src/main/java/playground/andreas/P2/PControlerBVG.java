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

package playground.andreas.P2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.hook.PHook;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;


/**
 * Entry point, registers all necessary hooks.  This version uses {@link playground.andreas.bvgScoringFunction} instead of the 
 * standard MATSim scoring function.  This means "scoring for passengers", not "scoring of the operator".
 * <p/>
 * Comments:<ul>
 * <li> I am not sure why there needs to be a separate scoring function; in principle, the standard MATSim scoring function should have
 * the same functionality.  If not, then it should be added there.  kai, sep'14
 * <li> In consequence, this version here should not be used by outside people.  kai, sep'14
 * </ul>
 * 
 * @author aneumann
 */
public final class PControlerBVG{

	private final static Logger log = Logger.getLogger(PControlerBVG.class);

	public static void main(final String[] args) {
		
		if(args.length == 0){
			log.info("Arg 1: config.xml");
			System.exit(1);
		}
		
		Config config = new Config();
		config.addModule(new PConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);

        Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
		PHook pHook = new PHook(controler);
		controler.addControlerListener(pHook);		
		controler.setScoringFunctionFactory(new BvgScoringFunctionFactory(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroup(controler.getConfig()), controler.getNetwork()));

		controler.run();
	}		
}