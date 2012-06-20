/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractPassengerPuDoCoordinates.java
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.Stack;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import playground.thibautd.jointtrips.population.JointActingTypes;

/**
 * @author thibautd
 */
public class ExtractPassengerPuDoCoordinates {
	public static void main(final String[] args) {
		String plansFile = args[ 0 ];
		String outFile = args[ 1 ];

		Parser parser = new Parser( outFile );
		parser.parse( plansFile );
		parser.close();
	}

	private static class Parser extends MatsimXmlParser {
		private boolean isSelectedPlan = false;
		private BufferedWriter writer;
		private String currentId = null;
		private Counter counter = new Counter( "parsing person # " );

		public Parser(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );
			try {
				writer.write( "id\tpuX\tpuY\tstatus\tdoX\tdoY" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void close() {
			try {
				counter.printCounter();
				writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ("person".equals( name )) {
				counter.incCounter();
				currentId = atts.getValue( "id" );
			}
			else if ("plan".equals( name )) {
				if ("yes".equals( atts.getValue( "selected" ) )) {
					isSelectedPlan = true;
				}
				else {
					isSelectedPlan = false;
				}
			}
			else if (isSelectedPlan) {
				try {
					if ("act".equals( name )) {
						if (JointActingTypes.PICK_UP.equals( atts.getValue( "type" ) )) {
							writer.newLine();
							writer.write( currentId+"\t"+atts.getValue( "x" )+"\t"+atts.getValue( "y" )+"\t" );
						}
						else if (JointActingTypes.DROP_OFF.equals( atts.getValue( "type" ) )) {
							writer.write( atts.getValue( "x" )+"\t"+atts.getValue( "y" ) );
						}
					}
					else if ("leg".equals( name )) {
						if (JointActingTypes.PASSENGER.equals( atts.getValue( "mode" ) )) {
							writer.write( "passenger\t" );
						}
						else if (JointActingTypes.DRIVER.equals( atts.getValue( "mode" ) )) {
							writer.write( "driver\t" );
						}
					}
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
		}
	}
}

