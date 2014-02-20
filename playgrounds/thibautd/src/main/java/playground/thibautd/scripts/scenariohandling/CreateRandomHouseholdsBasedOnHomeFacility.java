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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import playground.thibautd.householdsfromcensus.CliquesWriter;

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
		Map<Id, List<Id>> cliques = new HashMap<Id, List<Id>>();
		for (List<Id> agentsAtFacility : parser.agentsAtFacility.values()) {
			facilitycounter.incCounter();
			while (agentsAtFacility.size() > 0) {
				counter.incCounter();
				int maxSize = agentsAtFacility.size();
				maxSize = maxSize > HH_MAX_SIZE ? HH_MAX_SIZE : maxSize;
				int minSize = maxSize < HH_MIN_SIZE ? maxSize : HH_MIN_SIZE;
				int size = minSize + (maxSize > minSize ? random.nextInt( maxSize - minSize) : 0);

				List<Id> clique = new ArrayList<Id>();
				for (int i=0; i < size; i++) {
					clique.add( agentsAtFacility.remove( random.nextInt( agentsAtFacility.size() ) ) );
				}
				cliques.put( new IdImpl( currentCliqueId++ ) , clique );
			}
		}
		counter.printCounter();
		facilitycounter.printCounter();

		CliquesWriter writer = new CliquesWriter( cliques );
		writer.writeFile( outFile );
	}

	private static class PlansParser extends MatsimXmlParser {
		private Map<Id, List<Id>> agentsAtFacility = new HashMap<Id, List<Id>>();
		private boolean isHomeKnown = false;
		private Id currentAgent = null;
		private final Counter counter = new Counter( "reading person # " );
		private final Counter homecounter = new Counter( "reading home location # " );

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if (name.equals( "person" )) {
				counter.incCounter();
				isHomeKnown = false;
				currentAgent = new IdImpl( atts.getValue( "id" ) );
			}
			else if (name.equals( "act" ) &&
					atts.getValue( "type" ).equals( "home" ) &&
					context.contains( "plan" ) &&
					!isHomeKnown) {
				homecounter.incCounter();
				Id facilityId = new IdImpl( atts.getValue( "facility" ) );
				List<Id> agents = agentsAtFacility.get( facilityId );

				if (agents == null) {
					agents = new ArrayList<Id>();
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

