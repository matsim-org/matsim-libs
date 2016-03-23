/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.estimatedsampling;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import gnu.trove.list.TCharList;
import gnu.trove.list.array.TCharArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.ObjectPool;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.ArentzePopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.IndexedPopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocialNetworkGenerationConfigGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author thibautd
 */
public class ContinuousAgeArentzePopulationModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(ContinuousAgeArentzePopulationModule.class);

	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );
		bind(IndexedPopulation.class).to(ArentzePopulation.class);
		log.debug("Configuring " + getClass().getSimpleName() + ": DONE");
	}

	@Provides @Singleton
	private ArentzePopulation createArentzePopulation( SocialNetworkGenerationConfigGroup group , Config config ) {
		final String populationFile = group.getInputPopulationFile();
		final Counter counter = new Counter( "convert person to agent # " );
		final ObjectPool<Coord> coordPool = new ObjectPool<>();

		final List<Id> ids = new ArrayList<>();
		final TCharList ages = new TCharArrayList();
		final List<Boolean> isMales = new ArrayList<>();
		final List<Coord> coords = new ArrayList<>();

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
						final boolean male = atts.getValue( "sex" ).equals( "m" );

						ids.add( Id.create(atts.getValue("id"), Person.class) );
						ages.add( (char) age );
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
					coords.add( coordPool.getPooledInstance(new Coord(x, y)) );
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
				ages.toArray(),
				malesArray,
				coords.toArray( new Coord[ ids.size() ] ) );
	}

}
