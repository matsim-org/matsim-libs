/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTikzFigureOfJpStructure.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 */
public class CreateTikzFigureOfJpStructure {
	private static final Logger log =
		Logger.getLogger(CreateTikzFigureOfJpStructure.class);

	private static final String AGENT_STYLE = "agent";
	private static final String PLAN_STYLE = "plan";
	private static final String SELECTED_PLAN_STYLE = "selectedplan";
	private static final String LINK_STYLE = "link";

	public static void main(final String[] args) throws IOException {
		final String plansFile = args[ 0 ];
		final String jointPlansFile = args[ 1 ];
		final String outFile = args[ 2 ];
		final List<String> personIds = new ArrayList<String>();
		for ( int i=3; i < args.length; i++ ) {
			personIds.add( args[ i ] );
		}

		log.info( "plans file: "+plansFile );
		log.info( "joint plans file: "+jointPlansFile );
		log.info( "out file: "+outFile );
		log.info( "person ids: "+personIds );

		log.info( "load plan infos" );
		final List<AgentPlanInfo> planInfos =
			parsePlanInfos( plansFile , personIds );
		log.info( "load joint plan infos" );
		final List<PlanLinkInfo> planLinkInfo =
			parsePlanLinkInfo( jointPlansFile , personIds );
		
		log.info( "write tex file" );
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writeBeginning( writer );
		writePlans( writer , planInfos );
		writeJointPlans( writer , planLinkInfo );
		writeEnd( writer );
		writer.close();
	}

	private static void writeBeginning(
			final BufferedWriter writer) throws IOException {
		writer.write( "\\begin{tikzpicture}" );
		writer.newLine();
		writer.write( "\\tikzstyle{"+AGENT_STYLE+"}=[]" );
		writer.newLine();
		writer.write( "\\tikzstyle{"+PLAN_STYLE+"}=[rectangle,draw]" );
		writer.newLine();
		writer.write( "\\tikzstyle{"+SELECTED_PLAN_STYLE+"}=[rectangle,fill=green,draw]" );
		writer.newLine();
		writer.write( "\\tikzstyle{"+LINK_STYLE+"}=[]" );
		writer.newLine();
	}

	private static void writePlans(
			final BufferedWriter writer,
			final List<AgentPlanInfo> agentPlanInfo) throws IOException {
		writer.newLine();
		String last = null;
		for ( AgentPlanInfo info : agentPlanInfo ) {
			writer.newLine();
			final String pos = last == null ? "" : "[right of="+last+"]";
			last = info.id;
			writer.write( "\\node ("+last+") "+pos+" ["+AGENT_STYLE+"] {"+info.id+"};" );
			writer.newLine();
			String lastPlan = last;
			for ( int i=0; i < info.nPlans; i++ ) {
				final String style = i == info.selectedPlan ? SELECTED_PLAN_STYLE : PLAN_STYLE;
				final String planPos = "[below of="+lastPlan+"]";
				lastPlan = planId( last , i );
				writer.write( "\\node ("+lastPlan+") "+planPos+" ["+style+"] {};" );
				writer.newLine();
			}
		}
	}

	private static String planId(final String personId, final int plan) {
		return personId+"-plan-"+plan;
	}

	private static void writeJointPlans(
			final BufferedWriter writer,
			final List<PlanLinkInfo> linkInfo) throws IOException {
		writer.newLine();
		for ( PlanLinkInfo info : linkInfo ) {
			final String start = planId( info.id1 , info.plan1 );
			final String end = planId( info.id2 , info.plan2 );
			//writer.write( "\\draw ["+LINK_STYLE+"] ("+start+") to [out=90,in=90] ("+end+");" );
			writer.write( "\\draw ["+LINK_STYLE+"] ("+start+") .. controls +(up:4mm) and +(up:4mm) .. ("+end+");" );
			writer.newLine();
		}
	}

	private static void writeEnd(
			final BufferedWriter writer) throws IOException {
		writer.write( "\\end{tikzpicture}" );
	}

	private static List<AgentPlanInfo> parsePlanInfos(
			final String plansFile,
			final List<String> personIds) {
		final List<AgentPlanInfo> infos = new ArrayList<AgentPlanInfo>();

		new MatsimXmlParser() {
			private String id = null;
			private int count = 0;
			private int selected = -1;
			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "person" ) ) {
					id = atts.getValue( "id" );
					count = 0;
					selected = -1;
				}
				if ( name.equals( "plan" ) ) {
					if ( atts.getValue( "selected" ).equals( "yes" ) ) {
						assert selected < 0;
						selected = count;
					}
					count++;
				}
			}

			@Override
			public void endTag(
					final String name,
					final String content,
					final Stack<String> context) {
				if ( name.equals( "person" ) && personIds.contains( id ) ) {
					infos.add( new AgentPlanInfo( id , count , selected ) );
				}
			}
		}.parse( plansFile );
		return infos;
	}

	private static List<PlanLinkInfo> parsePlanLinkInfo(
			final String jointPlansFile,
			final List<String> personIdsToConsider) {
		final List<PlanLinkInfo> infos = new ArrayList<PlanLinkInfo>();

		new MatsimXmlParser( false ) {
			final List<String> personIds = new ArrayList<String>();
			final List<Integer> planNrs = new ArrayList<Integer>();

			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "jointPlan" ) ) {
					personIds.clear();
					planNrs.clear();
				}
				if ( name.equals( "individualPlan" ) ) {
					personIds.add( atts.getValue( "personId" ) );
					planNrs.add( Integer.valueOf( atts.getValue( "planNr" ) ) );
				}
			}

			@Override
			public void endTag(
					final String name,
					final String content,
					final Stack<String> context) {
				if ( name.equals( "jointPlan" ) ) {
					assert personIds.size() == planNrs.size();
					if ( consider( personIds , personIdsToConsider ) ) {
						for ( int i = 0; i < personIds.size(); i++ ) {
							final String id1 = personIds.get( i );
							final int plan1 = planNrs.get( i );
							for ( int j = 0; j < personIds.size(); j++ ) {
								if ( i == j ) continue;
								final String id2 = personIds.get( j );
								final int plan2 = planNrs.get( j );
								infos.add( new PlanLinkInfo( id1 , plan1 , id2 , plan2 ) );
							}
						}
					}
				}			
			}
		}.parse( jointPlansFile );

		return infos;
	}

	private static boolean consider(
			final List<String> personIds,
			final List<String> personIdsToConsider) {
		boolean foundNonConsidered = false;
		boolean foundConsidered = false;
		for ( String id : personIds ) {
			if ( personIdsToConsider.contains( id ) ) {
				if ( foundNonConsidered ) throw new RuntimeException( "found a joint plan only partly in specified group: "+personIds );
				foundConsidered = true;
			}
			else {
				if ( foundConsidered ) throw new RuntimeException( "found a joint plan only partly in specified group: "+personIds );
				foundNonConsidered = true;
			}
		}
		return foundConsidered;
	}
}

class AgentPlanInfo {
	public final String id;
	public final int nPlans, selectedPlan;

	public AgentPlanInfo(
			final String id,
			final int nPlans,
			final int selectedPlan) {
		this.id = id;
		this.nPlans = nPlans;
		this.selectedPlan = selectedPlan;
	}
}

class PlanLinkInfo {
	public final String id1, id2;
	public final int plan1, plan2;

	public PlanLinkInfo(
			final String id1,
			final int plan1,
			final String id2,
			final int plan2) {
		this.id1 = id1;
		this.plan1 = plan1;
		this.id2 = id2;
		this.plan2 = plan2;
	}
}
