/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractDriverBeeflyDetours.java
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
package org.matsim.contrib.socnetsim.usage.analysis.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 */
public class ExtractDriverBeeflyDetours {
	public static void main(final String[] args) {
		final String plansFile = args[ 0 ];
		final String outFile = args[ 1 ];
		(new PlansParser( outFile )).parse( plansFile );
	}

	private static class PlansParser extends MatsimXmlParser {
		private String currentId = null;
		private boolean isSelectedPlan = false;
		private Coord lastActCoord = null;
		private Coord pickUpCoord = null;
		private Coord dropOffCoord = null;
		private final BufferedWriter writer;
		private final Counter counter = new Counter( "reading person # " );

		public PlansParser(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );
			try {
				writer.write( "driverId\tdirectDistance\taccessDistance\tjointDistance\tegressDistance" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if (name.equals( "person" )) {
				counter.incCounter();
				currentId = atts.getValue( "id" );
			}
			else if (name.equals( "plan" )) {
				isSelectedPlan = atts.getValue( "selected" ).equals( "yes" );
			}
			else if (isSelectedPlan && name.equals( "act" )) {
				Coord coord = new Coord(Double.parseDouble(atts.getValue("x")), Double.parseDouble(atts.getValue("y")));

				String type = atts.getValue( "type" );
				if (type.equals( "pick_up" )) {
					pickUpCoord = coord;
				}
				else if (type.equals( "drop_off" )) {
					dropOffCoord = coord;
				}
				else if (pickUpCoord != null) {
					double directDist = CoordUtils.calcEuclideanDistance( lastActCoord , coord );
					double accessDist = CoordUtils.calcEuclideanDistance( lastActCoord , pickUpCoord );
					double jointDist = CoordUtils.calcEuclideanDistance( pickUpCoord , dropOffCoord );
					double egressDist = CoordUtils.calcEuclideanDistance( dropOffCoord , coord );

					try{
						writer.newLine();
						writer.write(
								currentId +"\t"+
								directDist +"\t"+
								accessDist +"\t"+
								jointDist +"\t"+
								egressDist );
					}
					catch (IOException e) {
						throw new UncheckedIOException( e );
					}
					lastActCoord = coord;
					pickUpCoord = null;
					dropOffCoord = null;
				}
				else {
					lastActCoord = coord;
				}
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if (name.equals( "plans" )) {
				counter.printCounter();
				try {
					writer.close();
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}
	}
}

