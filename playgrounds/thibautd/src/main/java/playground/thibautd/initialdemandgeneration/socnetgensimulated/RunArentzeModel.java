/* *********************************************************************** *
 * project: org.matsim.*
 * RunArentzeModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated;

import gnu.trove.list.TCharList;
import gnu.trove.list.array.TCharArrayList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.FileWriterEvolutionListener;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.IndexedPopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ModelIterator;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ModelRunner;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.PreprocessedModelRunner;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocialNetworkGenerationConfigGroup;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.DeterministicPart;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.GumbelErrorTerm;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkWriter;
import playground.thibautd.utils.MoreIOUtils;
import playground.thibautd.utils.ObjectPool;
import playground.thibautd.utils.SoftCache;

/**
 * @author thibautd
 */
public class RunArentzeModel {
	private static final Logger log =
		Logger.getLogger(RunArentzeModel.class);


	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final SocialNetworkGenerationConfigGroup config = new SocialNetworkGenerationConfigGroup();
		final TRBModelConfigGroup pars = new TRBModelConfigGroup();
		loadAndLogGroups( configFile , config , pars );

		Logger.getLogger( SoftCache.class ).setLevel( Level.TRACE );

		MoreIOUtils.initOut( config.getOutputDirectory() );

		log.info( "################################################################################" );
		log.info( "###### start socnet gen" );
		log.info( "###### popfile: " + config.getInputPopulationFile() );
		log.info( "###### outputdir: " + config.getOutputDirectory() );
		log.info( "###### primary sampling rate: " + config.getPrimarySamplingRate() );
		log.info( "###### secondary sampling rate: " + config.getSecondarySamplingRate() );
		log.info( "################################################################################" );

		final ArentzePopulation population = parsePopulation( config.getInputPopulationFile() );

		final TieUtility utility = 
			new TieUtility(
				new DeterministicPart() {
						@Override
						public double calcDeterministicPart(
								final int ego,
								final int alter ) {
							final int ageClassDifference =
							Math.abs(
								population.getAgeCategory( ego ) -
								population.getAgeCategory( alter ) );

							// increase distance by 1 (normally meter) to avoid linking with all agents
							// living in the same place.
							// TODO: test sensitivity of the results to this
							return pars.getB_logDist() * Math.log( CoordUtils.calcDistance( population.getCoord( ego ), population.getCoord( alter ) ) + 1 )
									+ pars.getB_sameGender() * dummy( population.isMale( ego ) == population.isMale( alter ) )
									+ pars.getB_ageDiff0() * dummy( ageClassDifference == 0 )
									+ pars.getB_ageDiff2() * dummy( ageClassDifference == 2 )
									+ pars.getB_ageDiff3() * dummy( ageClassDifference == 3 )
									+ pars.getB_ageDiff4() * dummy( ageClassDifference == 4 );
						}
					},
					new GumbelErrorTerm(),
					false ); // cache
		final ModelRunner runner =
			new PreprocessedModelRunner(
					config.getLowestCachedUtilityPrimary(),
					config.getLowestCachedUtilitySecondary(),
					population,
					utility,
					config.getPrimarySamplingRate(),
					config.getSecondarySamplingRate(),
					config.getNThreads() );

		final ModelIterator modelIterator =
			new ModelIterator(
					config.getTargetClustering(),
					config.getTargetDegree() );

		final FileWriterEvolutionListener fileListener = new FileWriterEvolutionListener( config.getOutputDirectory()+"/threshold-evolution.dat" );
		modelIterator.addListener( fileListener );

		final SocialNetwork network =
			modelIterator.iterateModelToTarget(
					runner,
					config.getInitialPoints() );

		fileListener.close();
		new SocialNetworkWriter( network ).write( config.getOutputDirectory() + "/social-network.xml.gz" );

		MoreIOUtils.closeOutputDirLogging();
	}

	private static void loadAndLogGroups( final String file , final ConfigGroup... groups ) {
		final Config config = new Config();
		for ( ConfigGroup group : groups ) config.addModule( group );

		new MatsimConfigReader( config ).readFile( file );

		final String newline = System.getProperty( "line.separator" );// use native line endings for logfile
		final StringWriter writer = new StringWriter();
		new ConfigWriter( config ).writeStream( new PrintWriter( writer ), newline );

		log.info( "Config params:" );
		log.info( writer.getBuffer().toString() );
	}

	private static double dummy(final boolean b) {
		return b ? 1 : 0;
	}

	public static ArentzePopulation parsePopulation(final String populationFile) {
		final Counter counter = new Counter( "convert person to agent # " );
		final ObjectPool<Coord> coordPool = new ObjectPool<Coord>();

		final List<Id> ids = new ArrayList<Id>();
		final TCharList ageCategories = new TCharArrayList();
		final List<Boolean> isMales = new ArrayList<Boolean>();
		final List<Coord> coords = new ArrayList<Coord>();

		new MatsimXmlParser() {
			private boolean missCoord = false;

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
						final char ageCategory = age <= 23 ? (char) 1 : age <= 37 ? (char) 2 : age <= 50 ? (char) 3 : age <= 65 ? (char) 4 : (char) 5;
						final boolean male = atts.getValue( "sex" ).equals( "m" );

						ids.add( Id.create( atts.getValue( "id" ) , Person.class ) );
						ageCategories.add( ageCategory );
						isMales.add( male );

						missCoord = true;
					}
					catch (Exception e) {
						throw new RuntimeException( "exception when processing person "+atts , e );
					}
				}

				if ( name.equals( "act" ) && missCoord ) {
					final double x = Double.parseDouble( atts.getValue( "x" ) );
					final double y = Double.parseDouble( atts.getValue( "y" ) );
					coords.add( coordPool.getPooledInstance( new CoordImpl( x , y ) ) );
					missCoord = false;
				}

			}

			@Override
			public void endTag(String name, String content,
					Stack<String> context) {}
		}.parse( populationFile );

		counter.printCounter();
		coordPool.printStats( "Coord pool" );

		final boolean[] malesArray = new boolean[ isMales.size() ];
		for ( int i = 0; i < malesArray.length; i++ )  {
			malesArray[ i ] = isMales.get( i ).booleanValue();
		}

		return new ArentzePopulation(
				ids.toArray( new Id[ ids.size() ] ),
				ageCategories.toArray(),
				malesArray,
				coords.toArray( new Coord[ ids.size() ] ) );
	}

	private static class ArentzePopulation extends IndexedPopulation {
		private final char[] ageCategory;
		private final boolean[] isMale;
		private final Coord[] coord;

		protected ArentzePopulation(
				final Id[] ids,
				final char[] ageCategory,
				final boolean[] isMale,
				final Coord[] coord ) {
			super( ids );
			this.ageCategory = ageCategory;
			this.isMale = isMale;
			this.coord = coord;
		}

		public char getAgeCategory( final int agent ) {
			return ageCategory[ agent ];
		}

		public boolean isMale( final int agent ) {
			return isMale[ agent ];
		}

		public Coord getCoord( final int agent ) {
			return coord[ agent ];
		}
	}

	private static class TRBModelConfigGroup extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "utility";

		private double b_logDist = -1.222;
		private double b_sameGender = 0.725;
		private double b_ageDiff0 = 0.918;
		private double b_ageDiff2 = -0.227;
		private double b_ageDiff3 = -1.314;
		private double b_ageDiff4 = -1.934;

		public TRBModelConfigGroup( ) {
			super( GROUP_NAME );
		}

		@StringGetter( "b_logDist" )
		public double getB_logDist() {
			return b_logDist;
		}

		@StringSetter( "b_logDist" )
		public void setB_logDist( double b_logDist ) {
			this.b_logDist = b_logDist;
		}

		@StringGetter( "b_sameGender" )
		public double getB_sameGender() {
			return b_sameGender;
		}

		@StringSetter( "b_sameGender" )
		public void setB_sameGender( double b_sameGender ) {
			this.b_sameGender = b_sameGender;
		}

		@StringGetter( "b_ageDiff0" )
		public double getB_ageDiff0() {
			return b_ageDiff0;
		}

		@StringSetter( "b_ageDiff0" )
		public void setB_ageDiff0( double b_ageDiff0 ) {
			this.b_ageDiff0 = b_ageDiff0;
		}

		@StringGetter( "b_ageDiff2" )
		public double getB_ageDiff2() {
			return b_ageDiff2;
		}

		@StringSetter( "b_ageDiff2" )
		public void setB_ageDiff2( double b_ageDiff2 ) {
			this.b_ageDiff2 = b_ageDiff2;
		}

		@StringGetter( "b_ageDiff3" )
		public double getB_ageDiff3() {
			return b_ageDiff3;
		}

		@StringSetter( "b_ageDiff3" )
		public void setB_ageDiff3( double b_ageDiff3 ) {
			this.b_ageDiff3 = b_ageDiff3;
		}

		@StringGetter( "b_ageDiff4" )
		public double getB_ageDiff4() {
			return b_ageDiff4;
		}

		@StringSetter( "b_ageDiff4" )
		public void setB_ageDiff4( double b_ageDiff4 ) {
			this.b_ageDiff4 = b_ageDiff4;
		}
	}
}

