/* *********************************************************************** *
 * project: org.matsim.*
 * DiluteSocialNetworkedPopulation.java
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
package playground.thibautd.initialdemandgeneration.transformation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.MoreIOUtils;

/**
 * Given a population for a wide area (say Switzerland), goes through a dilution
 * process to restrict the scenario to an area of interest (say Zurich).
 * It does NOT sample the population, it just removes agents considered out
 * of the area of interest. See {@link SocialNetworkedPopulationDilutionUtils}
 * for more details on which agents are kept or not.
 * @author thibautd
 */
public class DiluteSocialNetworkedPopulation {
	private static enum DilutionType {
		area_only,
		area_leisure_alters,
		area_all_alters;
	}

	private static void main(final ArgParser.Args args) {

		final Coord center =
				new Coord(args.getDoubleValue("--xcenter"), args.getDoubleValue("--ycenter"));

		final double radius = args.getDoubleValue( "--radius" );

		final DilutionType dilutionType = args.getEnumValue( "--dilution-type" , DilutionType.class );

		final String inpopfile = args.getValue( "--inpopfile" );
		final String insocnet = args.getValue( "--insocnet" );
		final String outdir = args.getValue( "--outdir" );

		MoreIOUtils.initOut( outdir );

		try {
			final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

			new MatsimPopulationReader( scenario ).readFile( inpopfile );
			new SocialNetworkReader( scenario ).parse( insocnet );

			switch ( dilutionType ) {
				case area_all_alters:
					SocialNetworkedPopulationDilutionUtils.dilute(
							scenario,
							center,
							radius );
					break;
				case area_leisure_alters:
					SocialNetworkedPopulationDilutionUtils.diluteLeisureOnly(
							scenario,
							center,
							radius );
					break;
				case area_only:
					SocialNetworkedPopulationDilutionUtils.diluteAreaOnly(
							scenario,
							center,
							radius );
					break;
				default:
					throw new RuntimeException( ""+dilutionType );
			}

			final String outpopfile = outdir+"/diluted-population.xml.gz";
			final String outsocnet = outdir+"/diluted-socialnetwork.xml.gz";

			new PopulationWriter(
					scenario.getPopulation(),
					scenario.getNetwork() ).write( outpopfile );

			new SocialNetworkWriter(
					(SocialNetwork)
						scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME ) ).write( outsocnet );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		
		parser.setDefaultValue( "--xcenter" , "683518.0" );
		parser.setDefaultValue( "--ycenter" , "246836.0" );

		parser.setDefaultValue( "--radius" , "30000" );

		parser.setDefaultValue( "--dilution-type" , "area_only" );

		parser.setDefaultValue( "--netfile" , null ); // unused.
		parser.setDefaultValue( "--inpopfile" , null );
		parser.setDefaultValue( "--insocnet" , null );
		parser.setDefaultValue( "--outdir" , null );
		
		main( parser.parseArgs( args ) );
	}
}

