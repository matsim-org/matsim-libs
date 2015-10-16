/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pbouman.crowdedness;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.pbouman.crowdedness.rules.SimpleRule;
import playground.pbouman.scenarios.SingleLineScenario;

/**
 * This class should probably not be here, but at least it gives an example
 * how to use the crowdedness stuff.
 * @author pbouman
 *
 */

public class RunSingleLineScenario
{

	public static void main(String [] args)
	{
		Scenario scenario = SingleLineScenario.buildScenario(20);
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.setScoringFunctionFactory(new CrowdedScoringFunctionFactory(
			new CharyparNagelScoringFunctionFactory( scenario ),
				controler.getEvents()
		));
		
		//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
		CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new SimpleRule());
		controler.getEvents().addHandler(observer);
		
		controler.run();
	}


}
