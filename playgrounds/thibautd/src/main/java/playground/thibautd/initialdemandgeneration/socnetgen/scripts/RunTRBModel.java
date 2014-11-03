/* *********************************************************************** *
 * project: org.matsim.*
 * RunTRBModel.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.scripts;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import playground.ivt.utils.ArgParser;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.LockedSocialNetwork;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ModelIterator;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ModelIteratorFileListener;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ModelRunner;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialPopulation;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ThresholdFunction;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.UtilityFunction;
import playground.thibautd.socnetsim.population.SocialNetworkWriter;
import playground.thibautd.utils.MoreIOUtils;
import playground.thibautd.utils.ObjectPool;

/**
 * @author thibautd
 */
public class RunTRBModel {
	private static final Logger log =
		Logger.getLogger(RunTRBModel.class);

	private static final double TARGET_NET_SIZE = 22.0;
	private static final double TARGET_CLUSTERING = 0.206;

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-p" , "--population-file" , null );
		parser.setDefaultValue( "-o" , "--output-dir" , null );
		parser.setDefaultValue( "-sp" , "--step-primary" , "1" );
		parser.setDefaultValue( "-ss" , "--step-secondary" , "1" );

		parser.addSwitch( "--null" , "--run-null-model" );

		main( parser.parseArgs( args ) );
	}

	private static void main(final ArgParser.Args args) {
		final String populationFile = args.getValue( "-p" );
		final String outputDirectory = args.getValue( "-o" );
		final int stepSizePrimary = args.getIntegerValue( "-sp" );
		final int stepSizeSecondary = args.getIntegerValue( "-ss" );
		final boolean isNullModel = args.isSwitched( "--null" );

		MoreIOUtils.initOut( outputDirectory );

		log.info( "################################################################################" );
		log.info( "###### start socnet gen" );
		log.info( "###### popfile: "+populationFile );
		log.info( "###### outputdir: "+outputDirectory );
		log.info( "###### step size primary: "+stepSizePrimary );
		log.info( "###### step size secondary: "+stepSizeSecondary );
		log.info( "###### use null model: "+isNullModel );
		log.info( "################################################################################" );


		final SocialPopulation<ArentzeAgent> population = parsePopulation( populationFile );
		
		final ModelRunner<ArentzeAgent> runner = new ModelRunner<ArentzeAgent>();

		runner.setStepSizePrimary( stepSizePrimary );
		runner.setStepSizeSecondary( stepSizeSecondary );

		runner.setUtilityFunction(
				new UtilityFunction<ArentzeAgent>() {
					@Override
					public double calcTieUtility(
							final ArentzeAgent ego,
							final ArentzeAgent alter) {
						if ( isNullModel ) return 0;

						final int ageClassDifference = Math.abs( ego.getAgeCategory() - alter.getAgeCategory() );

						// increase distance by 1 (normally meter) to avoid linking with all agents
						// living in the same place.
						// TODO: test sensitivity of the results to this
						return -1.222 * Math.log( CoordUtils.calcDistance( ego.getCoord() , alter.getCoord() ) + 1 )
							+0.725 * dummy( ego.isMale() == alter.isMale() )
							+0.918 * dummy( ageClassDifference == 0 )
							-0.227 * dummy( ageClassDifference == 2 )
							-1.314 * dummy( ageClassDifference == 3 )
							-1.934 * dummy( ageClassDifference == 4 );
					}
				});
		//runner.setThresholds( new ThresholdFunction( 1.735 , 1.835 ) );
		runner.setThresholds( new ThresholdFunction( -8.8 , 230 ) );

		final ModelIterator modelIterator = new ModelIterator();
		final ModelIteratorFileListener listener = new ModelIteratorFileListener( outputDirectory+"/thresholdEvolution.dat" );
		//modelIterator.addListener( listener );
		final LockedSocialNetwork network =
			modelIterator.iterateModelToTarget(
				runner,
				population,
				TARGET_NET_SIZE,
				TARGET_CLUSTERING,
				2);
		listener.close();

		new SocialNetworkWriter( network ).write( outputDirectory+"/social-network.xml.gz" );

		MoreIOUtils.closeOutputDirLogging();
	}

	private static double dummy(final boolean b) {
		return b ? 1 : 0;
	}

	private static SocialPopulation<ArentzeAgent> parsePopulation(final String populationFile) {
		final SocialPopulation<ArentzeAgent> population = new SocialPopulation<ArentzeAgent>();

		final Counter counter = new Counter( "convert person to agent # " );
		final ObjectPool<Coord> coordPool = new ObjectPool<Coord>();

		new MatsimXmlParser() {
			private ArentzeAgent agentWithoutCoord = null;

			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "person" ) ) {
					try {
						if ( Integer.parseInt( atts.getValue( "id" ) ) > 1000000000 ) return;
					}
					catch ( NumberFormatException e ) {
						// not the herbie population: do no bother.
					}
					counter.incCounter();

					try {
						final int age = Integer.parseInt( atts.getValue( "age" ) );
						if ( age < 0 ) throw new IllegalArgumentException( ""+age );
						final int ageCategory = age <= 23 ? 1 : age <= 37 ? 2 : age <= 50 ? 3 : age <= 65 ? 4 : 5;
						final boolean male = atts.getValue( "sex" ).equals( "m" );

						agentWithoutCoord =
								new ArentzeAgent(
									new IdImpl( atts.getValue( "id" ) ),
									ageCategory,
									male );

						population.addAgent(
								agentWithoutCoord );
					}
					catch (Exception e) {
						throw new RuntimeException( "exception when processing person "+atts , e );
					}
				}

				if ( name.equals( "act" ) && agentWithoutCoord != null ) {
					final double x = Double.parseDouble( atts.getValue( "x" ) );
					final double y = Double.parseDouble( atts.getValue( "y" ) );
					agentWithoutCoord.setCoord( coordPool.getPooledInstance( new CoordImpl( x , y ) ) );
					agentWithoutCoord = null;
				}

			}

			@Override
			public void endTag(String name, String content,
					Stack<String> context) {}
		}.parse( populationFile );

		counter.printCounter();
		coordPool.printStats( "Coord pool" );

		return population;
	}

	private static class ArentzeAgent implements Agent {
		private final Id id;
		private final int ageCategory;
		private final boolean isMale;
		private Coord coord = null;

		public ArentzeAgent(
				final Id id,
				final int ageCategory,
				final boolean male) {
			this.id = id;
			this.ageCategory = ageCategory;
			this.isMale = male;
		}

		@Override
		public Id getId() {
			return id;
		}

		public int getAgeCategory() {
			return this.ageCategory;
		}

		public boolean isMale() {
			return this.isMale;
		}

		public Coord getCoord() {
			return this.coord;
		}

		public void setCoord(final Coord coord) {
			if ( this.coord != null ) throw new IllegalStateException();
			this.coord = coord;
		}
	}
}

