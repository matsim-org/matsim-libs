/* *********************************************************************** *
 * project: org.matsim.*
 * SampleCliques.java
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
package playground.thibautd.householdsfromcensus;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Takes a Clique xml file and samples a given portion of them.
 * The ratio (e.g. 10%) is defined on the number of agents, not cliques.
 * @author thibautd
 */
public class SampleCliques {
	private static final Logger log =
		Logger.getLogger(SampleCliques.class);
	private static final String CLIQUE_START_LINE = "\t<"+CliquesSchemaNames.CLIQUE+" "+CliquesSchemaNames.CLIQUE_ID+"=\"";
	private static final String MEMBER_START_LINE ="\t\t<"+CliquesSchemaNames.MEMBER+" "+CliquesSchemaNames.MEMBER_ID+"=\"";
	private static final String END_CLIQUE = "\t</"+CliquesSchemaNames.CLIQUE+">";
	private final static double maxCliqueSize = 10;

	public static void main(final String[] args) {
		final double rate = Double.parseDouble( args[ 0 ] );
		final String inFile = args[ 1 ];
		final String outFile = args[ 2 ];

		Parser parser = new Parser();
		parser.parse( inFile );
		
		int popSize = 0;
		for (Record r : parser.getRecords()) {
			popSize += r.members.size();
		}

		final int toSample = (int) (rate * popSize);
		log.info( "sampling "+toSample+" individuals out of "+popSize );
		int sampled = 0;

		try {
			Random random = new Random( 1988 );
			Counter counter = new Counter( "sampling clique # " );
			BufferedWriter writer = initOut( outFile );

			while (sampled < toSample) {
				counter.incCounter();
				Record r = parser.getRecords().remove( random.nextInt( parser.getRecords().size() ) );
				sampled += r.members.size();
				writeRecord( r , writer );
			}
			closeOut( writer );
			counter.printCounter();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		log.info( sampled+" individuals were sampled" );
	}

	private static BufferedWriter initOut(final String outFile) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		writer.newLine();
		writer.newLine();
		writer.write( "<!-- generated with "+SampleCliques.class+" -->" );
		writer.newLine();
		writer.write( "<cliques>" );
		writer.newLine();
		return writer;
	}

	private static void writeRecord(final Record record , final BufferedWriter writer) throws IOException {
		writer.write( CLIQUE_START_LINE+record.id+"\" >" );
		writer.newLine();
		for (String m : record.members) {
			writer.write( MEMBER_START_LINE+m+"\" />" );
			writer.newLine();
		}
		writer.write( END_CLIQUE );
		writer.newLine();
	}

	private static void closeOut(final BufferedWriter writer) throws IOException {
		writer.write( "\t</cliques>" );
		writer.close();
	}

	private static class Parser extends MatsimXmlParser {
		private final List<Record> records = new ArrayList<Record>();
		private Record currentRecord = null;
		private final Counter counter = new Counter( "importing clique info # " );

		public Parser() {
			super( false );
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if (name.equals( CliquesSchemaNames.CLIQUE )) {
				counter.incCounter();
				currentRecord = new Record( atts.getValue( CliquesSchemaNames.CLIQUE_ID ) );
			}
			else if (name.equals( CliquesSchemaNames.MEMBER )) {
				currentRecord.members.add( atts.getValue( CliquesSchemaNames.MEMBER_ID ) );
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if (name.equals( CliquesSchemaNames.CLIQUE )) {
				if (currentRecord.members.size() <= maxCliqueSize) {
					records.add( currentRecord );
				}
			}
		}

		public List<Record> getRecords() {
			return records;
		}
	}

	private static class Record {
		private final String id;
		public final List<String> members = new ArrayList<String>();

		public Record(final String id) {
			this.id = id;
		}
	}
}

