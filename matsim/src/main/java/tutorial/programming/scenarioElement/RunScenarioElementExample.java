/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.scenarioElement;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunScenarioElementExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		
		Scenario scenario = ScenarioUtils.createScenario(config) ;
		
		// NOTE: The below mechanism is probably superseded by the injection syntax.

		{
			final MyScenarioElement myScenarioElement = new MyScenarioElement();
			myScenarioElement.addInformation("this is a text") ;
			scenario.addScenarioElement( MyScenarioElement.NAME, myScenarioElement );
		}
		
		// ...
		
		// This can later be retrieved at arbitrary places where the scenario is availabe by
		{
			MyScenarioElement mm = (MyScenarioElement) scenario.getScenarioElement( MyScenarioElement.NAME ) ;
			for ( String str : mm.retrieveInformation() ) {
				System.out.println( str );
			}
		}
		
		// NOTE: The above mechanism is probably superseded by the injection syntax.
		
	}

}
