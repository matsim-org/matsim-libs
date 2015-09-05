/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateDesiresFromConfigFile.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;
import playground.thibautd.utils.DesiresConverter;

/**
 * @author thibautd
 */
public class GenerateDesiresFromConfigFile {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		final String outObjectAttributes = args[ 1 ];

		final Config config = JointScenarioUtils.loadConfig( configFile );
		final Scenario sc = ScenarioUtils.createScenario( config );
		new MatsimPopulationReader( sc ).readFile( config.plans().getInputFile() );

		final ObjectAttributes desires = new ObjectAttributes();
		for ( Person p : sc.getPopulation().getPersons().values() ) {
			final Desires d = new Desires( null );
			for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
				d.putActivityDuration(
						params.getActivityType(),
						params.getTypicalDuration() );
			}
			desires.putAttribute(
					p.getId().toString(),
					"desires",
					d );
		}

		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter( desires );
		writer.putAttributeConverter( Desires.class , new DesiresConverter() );
		writer.writeFile( outObjectAttributes );
	}
}

