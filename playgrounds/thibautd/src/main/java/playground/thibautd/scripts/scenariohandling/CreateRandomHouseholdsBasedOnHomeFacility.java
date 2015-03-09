/* *********************************************************************** *
 * project: org.matsim.*
 * CreateRandomHouseholdsBasedOnHomeFacility.java
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
package playground.thibautd.scripts.scenariohandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.xml.sax.Attributes;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.socnetsim.cliques.Clique;

/**
 * @author thibautd
 */
public class CreateRandomHouseholdsBasedOnHomeFacility {
	private final static int HH_MIN_SIZE = 2;
	private final static int HH_MAX_SIZE = 10;

	public static void main(final String[] args) {
		final String inFile = args[ 0 ];
		final String outFile = args[ 1 ];
		final Random random = new Random( 1234 );

		PlansParser parser = new PlansParser();
		parser.parse( inFile );

		Counter counter = new Counter( "creating clique # " );
		Counter facilitycounter = new Counter( "examining facility # " );
		long currentCliqueId = 0;
		Map<Id<Clique>, List<Id<Person>>> cliques = new HashMap<>();
		for (List<Id<Person>> agentsAtFacility : parser.agentsAtFacility.values()) {
			facilitycounter.incCounter();
			while (agentsAtFacility.size() > 0) {
				counter.incCounter();
				int maxSize = agentsAtFacility.size();
				maxSize = maxSize > HH_MAX_SIZE ? HH_MAX_SIZE : maxSize;
				int minSize = maxSize < HH_MIN_SIZE ? maxSize : HH_MIN_SIZE;
				int size = minSize + (maxSize > minSize ? random.nextInt( maxSize - minSize) : 0);

				List<Id<Person>> clique = new ArrayList<>();
				for (int i=0; i < size; i++) {
					clique.add( agentsAtFacility.remove( random.nextInt( agentsAtFacility.size() ) ) );
				}
				cliques.put( Id.create( currentCliqueId++ , Clique.class ) , clique );
			}
		}
		counter.printCounter();
		facilitycounter.printCounter();

		CliquesWriter writer = new CliquesWriter( cliques );
		writer.writeFile( outFile );
	}

	private static class PlansParser extends MatsimXmlParser {
		private Map<Id<ActivityFacility>, List<Id<Person>>> agentsAtFacility = new HashMap<>();
		private boolean isHomeKnown = false;
		private Id<Person> currentAgent = null;
		private final Counter counter = new Counter( "reading person # " );
		private final Counter homecounter = new Counter( "reading home location # " );

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (name.equals( "person" )) {
				counter.incCounter();
				isHomeKnown = false;
				currentAgent = Id.create( atts.getValue( "id" ) , Person.class );
			}
			else if (name.equals( "act" ) &&
					atts.getValue( "type" ).equals( "home" ) &&
					context.contains( "plan" ) &&
					!isHomeKnown) {
				homecounter.incCounter();
				Id<ActivityFacility> facilityId = Id.create( atts.getValue( "facility" ) , ActivityFacility.class );
				List<Id<Person>> agents = agentsAtFacility.get( facilityId );

				if (agents == null) {
					agents = new ArrayList<>();
					agentsAtFacility.put( facilityId , agents );
				}

				agents.add( currentAgent );
				isHomeKnown = true;
			}
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if (name.equals( "persons" )) {
				counter.printCounter();
				homecounter.printCounter();
			}
		}
	}
}

