/* *********************************************************************** *
 * project: org.matsim.*
 * ExecutePlans.java
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
package playground.thibautd.socnetsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

import playground.ivt.utils.ArgParser;
import playground.thibautd.socnetsim.population.JointPlansConfigGroup;
import playground.thibautd.utils.MoreIOUtils;

/**
 * To just execute a plans file, in case you are so stupid you delete
 * the events file of the last iteration (but nobody would ever do that, right?)
 * @author thibautd
 */
public class ExecutePlans {
	public static void main(final String[] argParser) {
		main( new ArgParser( argParser ) );
	}

	private static enum SimulationType {
		households, socialnet;
	}

	private static void main(final ArgParser argParser) {
		argParser.setDefaultValue( "--folder-for-input" , "-f" , null );
		argParser.setDefaultValue( "--output-folder" , "-o" , null );
		argParser.setDefaultValue( "--simulation-type" , "-t" , ""+SimulationType.households );

		final String folder = argParser.args().getValue( "-f" );
		final String output = argParser.args().getValue( "-o" );
		final SimulationType simType = argParser.args().getEnumValue( "-t" , SimulationType.class );

		MoreIOUtils.checkDirectory( output );

		final Config config = RunUtils.loadConfig( folder+"/output_config.xml.gz" );

		config.controler().setOutputDirectory( output );
		// TODO: use actual iteratio number
		config.controler().setFirstIteration( 1 );
		config.controler().setLastIteration( 1 );

		config.plans().setInputFile( folder+"/output_plans.xml.gz" );
		((JointPlansConfigGroup) config.getModule( JointPlansConfigGroup.GROUP_NAME )).setFileName(
				folder+"/output_jointPlans.xml.gz" );

		final Scenario scenario = RunUtils.loadScenario( config );

		switch ( simType ) {
		case households:
			RunCliquesWithModularStrategies.runScenario( scenario , true );
			break;
		case socialnet:
			RunGenericSocialNetwork.runScenario( scenario , true );
			break;
		default:
			throw new IllegalArgumentException( simType.toString() );
		}
	}
}

