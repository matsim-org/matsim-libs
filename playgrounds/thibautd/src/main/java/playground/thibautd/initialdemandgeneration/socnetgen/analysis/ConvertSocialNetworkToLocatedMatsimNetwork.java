/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertSocialNetworkToLocatedMatsimNetwork.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.contrib.socnetsim.utils.ObjectPool;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author thibautd
 */
public class ConvertSocialNetworkToLocatedMatsimNetwork {
	public static void main(final String[] args) {
		final String socNetFile = args[ 0 ];
		final String popFile = args[ 1 ];
		final String outNetwork = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Map<Id<Person>, Coord> coords = parsePopulation( popFile );
		new SocialNetworkReader( sc ).parse( socNetFile );

		final Network network =
			SocialNetworkAsMatsimNetworkUtils.convertToNetwork(
					(SocialNetwork)
					sc.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
					coords );

		new NetworkWriter( network ).write( outNetwork );
	}

	public static Map<Id<Person>, Coord> parsePopulation(final String populationFile) {
		final Counter counter = new Counter( "read person # " );
		final ObjectPool<Coord> coordPool = new ObjectPool<Coord>();

		final Map<Id<Person>, Coord> map = new HashMap<>();

		new MatsimXmlParser() {
			Id<Person> id = null;

			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "person" ) ) {
					try {
						if ( Integer.parseInt( atts.getValue( "id" ) ) > 1000000000 ) return;
					}
					catch ( NumberFormatException e ) {}
					counter.incCounter();

					try {
						this.id = Id.create( atts.getValue( "id" ) , Person.class );
					}
					catch (Exception e) {
						throw new RuntimeException( "exception when processing person "+atts , e );
					}
				}

				if ( name.equals( "act" ) && id != null ) {
					final double x = Double.parseDouble( atts.getValue( "x" ) );
					final double y = Double.parseDouble( atts.getValue( "y" ) );
					map.put( id ,  coordPool.getPooledInstance(new Coord(x, y)) );
					id = null;
				}

			}

			@Override
			public void endTag(String name, String content,
					Stack<String> context) {}
		}.parse( populationFile );

		counter.printCounter();
		coordPool.printStats( "Coord pool" );

		return map;
	}


}

