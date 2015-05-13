/* *********************************************************************** *
 * project: org.matsim.*
 * FilterCliquesBasedOnPopulation.java
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
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.socnetsim.framework.cliques.Clique;

/**
 * @author thibautd
 */
public class FilterCliquesBasedOnPopulation {
	public static void main(final String[] args) {
		final String plansFile = args[0];
		final String cliquesFile = args[1];
		final String outCliques = args[2];

		PlansParser plansParser = new PlansParser();
		plansParser.parse( plansFile );

		CliquesParser cliquesParser = new CliquesParser( plansParser );
		cliquesParser.parse( cliquesFile );

		(new CliquesWriter( cliquesParser.cliques )).writeFile( outCliques );
	}

	private static class PlansParser extends MatsimXmlParser {
		private final List<Id<Person>> ids = new ArrayList<>();
		private final Counter counter = new Counter( "reading person # " );

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (name.equals( "person" )) {
				counter.incCounter();
				ids.add( Id.create( atts.getValue( "id" ) , Person.class ) );
			}
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if ( name.equals( "plans" ) ) {
				counter.printCounter();
			}
		}
	}

	private static class CliquesParser extends MatsimXmlParser {
		private final Map<Id<Clique>, List<Id<Person>>> cliques = new HashMap<Id<Clique>, List<Id<Person>>>();
		private Id<Clique> currentCliqueId;
		private List<Id<Person>> currentClique;
		private final Counter counter = new Counter( "reading clique # " );
		private final Set<Id<Person>> agentsToKeep;
		private boolean isToKeep = false;

		public CliquesParser(final PlansParser plans) {
			super( false );
			agentsToKeep = new TreeSet<>(plans.ids);
		}

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (name.equals( "clique" )) {
				counter.incCounter();
				isToKeep = false;
				currentClique = new ArrayList<>();
				currentCliqueId = Id.create( atts.getValue( "id" ) , Clique.class );
			}
			else if (name.equals( "person" )) {
				Id<Person> id = Id.create( atts.getValue( "id" ) , Person.class );
				currentClique.add( id );
				if ( agentsToKeep.remove( id ) ) {
					isToKeep = true;
				}
			}
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if ( name.equals( "cliques" ) ) {
				counter.printCounter();
			}
			if ( name.equals( "clique" ) ) {
				if (isToKeep) {
					cliques.put( currentCliqueId , currentClique );
				}
			}
		}
	}
}

