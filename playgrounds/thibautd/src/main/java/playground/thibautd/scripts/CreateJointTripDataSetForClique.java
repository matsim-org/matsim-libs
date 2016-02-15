/* *********************************************************************** *
 * project: org.matsim.*
 * CreateJointTripDataSetForClique.java
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Creates a data set to use in R or whatever data analysis tool
 * to visualize joint structure in a particular clique,
 * for instance in R:
 *
 * plot( as.numeric( as.factor( data$personId ) ) , as.numeric( as.factor( data$jointPlanId ) ) )
 *
 * note that it does not include individual plans
 * @author thibautd
 */
public class CreateJointTripDataSetForClique {
	private static final Logger log =
		Logger.getLogger(CreateJointTripDataSetForClique.class);

	public static void main(final String[] args) {
		final String cliqueId = args[ 0 ];
		final String cliquesFile = args[ 1 ];
		final String jointPlansFile = args[ 2 ];
		final String outputFile = args[ 3 ];

		CliqueParser clique = new CliqueParser( cliqueId );
		clique.parse( cliquesFile );

		JointPlanParser jointPlans = new JointPlanParser( clique.membersIds );
		jointPlans.parse( jointPlansFile );

		write( jointPlans , outputFile );
	}

	private static void write(
			final JointPlanParser jointPlans,
			final String outputFile) {
		final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile );
		
		final Iterator<String> planIter = jointPlans.plans.iterator();
		final Iterator<String> personIter = jointPlans.persons.iterator();

		try {
			writer.write( "personId\tjointPlanId" );
			Counter counter = new Counter( "write line # " );
			while ( planIter.hasNext() ) {
				counter.incCounter();
				writer.newLine();
				writer.write( personIter.next()+"\t"+planIter.next() );
			}
			counter.printCounter();
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static class CliqueParser extends MatsimXmlParser {
		private final String cliqueId;
		private final List<String> membersIds = new ArrayList<String>();
		private boolean inClique = false;
		
		public CliqueParser(final String cliqueId) {
			super( false );
			this.cliqueId = cliqueId;
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( name.equals( "clique" ) &&
					atts.getValue( "id" ).equals( cliqueId ) ) {
				log.info( "entering clique" );
				inClique = true;
			}
			else if ( inClique && name.equals( "person" ) ) {
				log.info( "found member "+atts.getValue( "id" ) );
				membersIds.add( atts.getValue( "id" ) );
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if ( name.equals( "clique" ) ) inClique = false;
		}
	}

	private static class JointPlanParser extends MatsimXmlParser {
		private final List<String> members;

		private final List<String> persons = new ArrayList<String>();
		private final List<String> plans = new ArrayList<String>();

		private int currentJointPlan = 0;
		private final Counter count = new Counter( "store plan # " );

		public JointPlanParser(final List<String> members) {
			super( false );
			this.members = members;
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( name.equals( "jointPlan" ) ) currentJointPlan++;
			if ( name.equals( "individualPlan" ) &&
					members.contains( atts.getValue( "personId" ) ) ) {
				persons.add( atts.getValue( "personId" ) );
				plans.add( ""+currentJointPlan );
				count.incCounter();
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if ( name.equals( "jointPlans" ) ) count.printCounter();
		}
	}
}

