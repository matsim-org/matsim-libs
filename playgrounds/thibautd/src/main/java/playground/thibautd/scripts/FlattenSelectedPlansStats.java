/* *********************************************************************** *
 * project: org.matsim.*
 * FlattenSelectedPlansStats.java
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

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * creates a flat data file from an xml plans file, containing info about
 * the selected plan.
 * it uses the start and end time for activities, start and leg duration for legs.
 * @author thibautd
 */
public class FlattenSelectedPlansStats {
	private static final String AGENT_ID = "agentId";
	private static final String PLANS_SCORE = "planScore";
	private static final String PE_TYPE = "peType";
	private static final String PE_INFO = "peInfo";
	private static final String PE_START = "peStart";
	private static final String PE_END = "peEnd";
	private static final String ROUTE_DIST = "routeDist";
	private static final String ACT_X = "actXCoord";
	private static final String ACT_Y = "actYCoord";
	private static final String NA = "NA";
	private static final String SEP = "\t";

	public static void main(final String[] args) {
		String inFile = args[ 0 ];
		String outFile = args[ 1 ];

		(new Parser( outFile )).parse( inFile );
	}

	private static class Parser extends MatsimXmlParser {
		private final BufferedWriter writer;
		private final Counter counter;
		private PlanInfo currentInfo = null;
		private PlanElementInfo peInfo = null;
		private boolean isSelectedPlan = false;
		private int line = 0;

		public Parser(final String outFile) {
			this.writer = IOUtils.getBufferedWriter( outFile );
			this.counter = new Counter( "writing "+outFile+": line # " );

			try {
				writer.write(
						AGENT_ID + SEP +
						PLANS_SCORE + SEP +
						PE_TYPE + SEP +
						PE_INFO + SEP +
						PE_START + SEP +
						PE_END + SEP +
						ROUTE_DIST + SEP +
						ACT_X + SEP +
						ACT_Y);
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
				currentInfo = new PlanInfo();
				currentInfo.id = atts.getValue( "id" );
			}
			else if (name.equals( "plan" ) && atts.getValue( "selected" ).equals( "yes" )) {
				currentInfo.selectedPlanScore = atts.getValue( "score" );
				isSelectedPlan = true;
			}
			else if ( isSelectedPlan && name.equals( "act" ) ) {
				double start = Time.parseTime( atts.getValue( "start_time" ) );
				double end = Time.parseTime( atts.getValue( "end_time" ) );
				peInfo = new PlanElementInfo(
							"act",
							atts.getValue( "type" ),
							toString( start ),
							toString( end ),
							NA,
							atts.getValue( "x" ),
							atts.getValue( "y" ));
				currentInfo.pes.add( peInfo );
			}
			else if ( isSelectedPlan && name.equals( "leg" ) ) {
				double start = Time.parseTime( atts.getValue( "dep_time" ) );
				double tt = Time.parseTime( atts.getValue( "trav_time" ) );
				peInfo = new PlanElementInfo(
							"leg",
							atts.getValue( "mode" ),
							toString( start ),
							toString(start + tt),
							NA,
							NA,
							NA);
				currentInfo.pes.add( peInfo );
			}
			else if ( isSelectedPlan && name.equals( "route" ) ) {
				String dist = atts.getValue( "dist" );
				peInfo.routeDist = dist == null ? NA : dist;
			}
		}

		private static String toString(final double d) {
			return d == Time.UNDEFINED_TIME ? NA : ""+d;
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if (name.equals( "plan" )) {
				isSelectedPlan = false;
			}
			else if ( name.equals( "person" ) ) {
				dumpInfo();
				currentInfo = null;
			}
			else if ( name.equals( "plans" ) ) {
				try {
					counter.printCounter();
					writer.close();
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		private void dumpInfo() {
			for (PlanElementInfo pe : currentInfo.pes) {
				line++;
				counter.incCounter();
				try {
					writer.newLine();

					writer.write(
							line + SEP +
							currentInfo.id + SEP +
							currentInfo.selectedPlanScore + SEP +
							pe.type + SEP +
							pe.info + SEP +
							pe.start + SEP +
							pe.end + SEP +
							pe.routeDist + SEP +
							pe.actx + SEP +
							pe.acty);
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}
	}

	private static class PlanInfo {
		public String id = null;
		public String selectedPlanScore = null;
		public final List<PlanElementInfo> pes = new ArrayList<PlanElementInfo>();
	}

	private static class PlanElementInfo {
		public final String type;
		public final String info;
		public final String start;
		public final String end;
		public String routeDist;
		public final String actx;
		public final String acty;

		public PlanElementInfo(
				final String type,
				final String info,
				final String start,
				final String end,
				final String routeDist,
				final String actx,
				final String acty) {
			this.type = type;
			this.info = info;
			this.start = start;
			this.end = end;
			this.routeDist = routeDist;
			this.actx = actx;
			this.acty = acty;
		}
	}
}


