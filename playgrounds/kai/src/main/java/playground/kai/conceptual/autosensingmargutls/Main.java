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
package playground.kai.conceptual.autosensingmargutls;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class Main {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		ActivityParams params = new ActivityParams("h")  ;
		config.planCalcScore().addActivityParams(params);
		double typicalDuration = 4.*3600;
		params.setTypicalDuration(typicalDuration);

		Scenario scenario = ScenarioUtils.createScenario(config) ;

	}

}
