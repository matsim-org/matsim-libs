/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateMerged2030Scenario.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.ivt.matsim2030.Matsim2030Utils;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.File;

/**
 * @author thibautd
 */
public class GenerateMerged2030Scenario {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "--config" , "-c" , null );
		parser.setDefaultValue( "--output-directory" , "-o" , "output/merged-2030/" );

		main( parser.parseArgs( args ) );
	}

	private static void main( final Args args ) {
		final String configFile = args.getValue( "--config" );
		final String outputDirectory = args.getValue( "--output-directory" );

		createOrFail( outputDirectory );

		final Config config = Matsim2030Utils.loadConfig( configFile );
		final Scenario scenario = Matsim2030Utils.loadScenario( config );

		final String plansFile = outputDirectory+"/population.xml.gz";
		final String personAttsFile = outputDirectory+"/personAttributes.xml.gz";
		final String networkFile = outputDirectory+"/network.xml.gz";

		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( plansFile );
		new ObjectAttributesXmlWriter( scenario.getPopulation().getPersonAttributes() ).writeFile( personAttsFile );

		new NetworkWriter( scenario.getNetwork() ).write( networkFile );

		config.plans().setInputFile( plansFile );
		config.plans().setInputPersonAttributeFile( personAttsFile );
		config.network().setInputFile( networkFile );

		new ConfigWriter( config ).write( outputDirectory+"/config.xml.gz" );
	}

	private static void createOrFail(final String outputDirectory ) {
		final File file = new File( outputDirectory );

		if ( file.exists() ) {
			if ( !file.isDirectory() ) throw new RuntimeException( outputDirectory+" exists and is not a directory." );
			if ( file.list().length > 0 ) throw new RuntimeException( outputDirectory+" exists and is not empty." );
		}

		file.mkdirs();
	}
}

