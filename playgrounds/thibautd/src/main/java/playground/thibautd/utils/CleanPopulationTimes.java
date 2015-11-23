/* *********************************************************************** *
 * project: org.matsim.*
 * CleanPopulationTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import java.util.List;

/**
 * @author thibautd
 */
public class CleanPopulationTimes {
	public static void main( final String[] args ) {
		Config config = ConfigUtils.loadConfig( args[ 0 ] );
		Scenario scen = ScenarioUtils.loadScenario( config );

		Population pop = scen.getPopulation();

		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				double now = 0;
				for (PlanElement pe : pes) {
					if ( pe instanceof Activity ) {
						Activity act = (Activity) pe;

						double endTime = act.getEndTime();
						if (endTime == Time.UNDEFINED_TIME) {
							endTime = now + act.getMaximumDuration();
							act.setEndTime( endTime );
						}
						now = endTime;
						act.setMaximumDuration( Time.UNDEFINED_TIME );
					}
				}
				((Activity) pes.get( 0 )).setStartTime( Time.UNDEFINED_TIME );
				((Activity) pes.get( pes.size() - 1 )).setEndTime( Time.UNDEFINED_TIME );
			}
		}

		(new PopulationWriter( pop , scen.getNetwork() )).write( config.plans().getInputFile()+".clean.xml.gz" );
	}
}

