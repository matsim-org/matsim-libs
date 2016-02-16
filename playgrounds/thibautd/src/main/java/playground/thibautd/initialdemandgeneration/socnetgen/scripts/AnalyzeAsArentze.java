/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeAsArentze.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.scripts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.initialdemandgeneration.socnetgen.analysis.ComputeSocialDistanceBetweenRandomIndividuals;
import playground.thibautd.initialdemandgeneration.socnetgen.analysis.IdentifyAndWriteComponents;
import playground.thibautd.initialdemandgeneration.socnetgen.analysis.WriteDegreeTable;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialPopulation;
import playground.thibautd.initialdemandgeneration.socnetgen.scripts.RunTRBModel.ArentzeAgent;
import playground.ivt.utils.MoreIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Produce the same kind of stats as T Arentze in his Social Networks paper.
 * @author thibautd
 */
public class AnalyzeAsArentze {
	private static final Logger log =
		Logger.getLogger(AnalyzeAsArentze.class);

	public static void main(final String[] args) {
		final String populationFile = args[ 0 ];
		final String inputSocialNetwork = args[ 1 ];
		final String outputDirectory = args[ 2 ];

		MoreIOUtils.initOut( outputDirectory );

		log.info( "reading social network" );
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( inputSocialNetwork );
	
		log.info( "computing clustering" );
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		final double clustering = SnaUtils.calcClusteringCoefficient( socialNetwork );
		log.info( "clustering is "+clustering );

		log.info( "creating degree table" );
		WriteDegreeTable.writeDegreeTable(
				outputDirectory+"/degree.dat",
				socialNetwork );

		log.info( "creating components table" );
		IdentifyAndWriteComponents.writeComponents(
				outputDirectory+"/components.dat",
				SnaUtils.identifyConnectedComponents( socialNetwork ) );


		log.info( "creating homophily table" );
		writeHomophilyDataset(
				outputDirectory+"/homophily.dat",
				RunTRBModel.parsePopulation( populationFile ),
				socialNetwork );

		log.info( "creating distance table" );
		ComputeSocialDistanceBetweenRandomIndividuals.writeRandomDistances(
				outputDirectory+"/distances.dat",
				socialNetwork,
				10000 );

	}

	public static void writeHomophilyDataset(
			final String file,
			final SocialPopulation<ArentzeAgent> population,
			final SocialNetwork socialNetwork ) {
		try (final BufferedWriter writer = IOUtils.getBufferedWriter( file )) {
			writer.write( "egoId\talterId\tegoAgeClass\talterAgeClass\tegoGender\talterGender\tdistance" );

			final Counter counter = new Counter( "Tie # " );
			for ( Id egoId : socialNetwork.getEgos() ) {
				final ArentzeAgent ego = population.getAgentsMap().get( egoId );

				final Iterable<Id<Person>> alters = socialNetwork.getAlters(egoId);
				for ( Id<Person> alterId : alters ) {
					counter.incCounter();
					final ArentzeAgent alter = population.getAgentsMap().get( alterId );

					writer.newLine();
					writer.write( egoId.toString() );
					writer.write( "\t"+alterId );
					writer.write( "\t"+ego.getAgeCategory() );
					writer.write( "\t"+alter.getAgeCategory() );
					writer.write( "\t"+(ego.isMale() ? "m" : "f" ) );
					writer.write( "\t"+(alter.isMale() ? "m" : "f" ) );
					writer.write( "\t"+CoordUtils.calcEuclideanDistance( ego.getCoord() , alter.getCoord() ) );
				}
			}
			counter.printCounter();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}

