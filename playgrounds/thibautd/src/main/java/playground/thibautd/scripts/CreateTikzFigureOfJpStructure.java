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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

		log.info( "clean info for visual appeal" );
		sortPersons( planInfos , planLinkInfo );
		cleanLinks( planInfos , planLinkInfo );
		
		log.info( "write tex file" );
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writeBeginning( writer );
		writePlans( writer , planInfos );
		writeJointPlans( writer , planLinkInfo );
		writeEnd( writer );
		writer.close();
	}

	private static void sortPersons(
			final List<AgentPlanInfo> planInfos,
			final List<PlanLinkInfo> planLinkInfo) {
		final List<AgentPlanInfo> sortedPersons = new ArrayList<AgentPlanInfo>();
		sortedPersons.add( planInfos.remove( 0 ) );

		while ( !planInfos.isEmpty() ) {
			final AgentPlanInfo last = sortedPersons.get( sortedPersons.size() - 1 );
			final AgentPlanInfo closest =
				Collections.max(
						planInfos,
						new Comparator<AgentPlanInfo>() {
							@Override
							public int compare(
									final AgentPlanInfo o1,
									final AgentPlanInfo o2) {
								final int n1 = countCommonJointPlans( last , o1 , planLinkInfo );
								final int n2 = countCommonJointPlans( last , o2 , planLinkInfo );
								return n1 - n2;
							}
						});
			planInfos.remove( closest );
			sortedPersons.add( closest );
		}

		assert planInfos.isEmpty();
		planInfos.addAll( sortedPersons );
	}

	private static int countCommonJointPlans(
			final AgentPlanInfo a1,
			final AgentPlanInfo a2,
			final List<PlanLinkInfo> planLinkInfo) {
		int c = 0;
		for ( PlanLinkInfo i : planLinkInfo ) {
			if ( (i.id1.equals( a1.id ) && i.id2.equals( a2.id )) ||
					(i.id2.equals( a1.id ) && i.id1.equals( a2.id )) ) {
				c++;
			}
		}
		assert c % 2 == 0;
		return c / 2;
	}

	private static void cleanLinks(
			final List<AgentPlanInfo> planInfos,
			final List<PlanLinkInfo> planLinkInfo) {
		{
			final Iterator<PlanLinkInfo> iterator = planLinkInfo.iterator();
			// remove all "back pointing" links
			while ( iterator.hasNext() ) {
				final PlanLinkInfo info = iterator.next();
				if ( pointsBackwards( info , planInfos ) ) iterator.remove();
			}
		}

		// sort by order of "to" link
		Collections.sort(
				planLinkInfo,
				new Comparator<PlanLinkInfo>() {
					@Override
					public int compare(
							final PlanLinkInfo l1,
							final PlanLinkInfo l2) {
						for ( AgentPlanInfo agent : planInfos ) {
							if ( l1.id2.equals( agent.id ) ) return -1;
							if ( l2.id2.equals( agent.id ) ) return 1;
						}
						return 0;
					}
				});

		// only keep the first link leaving each plan
		final Set<String> knownPlans = new HashSet<String>();
		final Iterator<PlanLinkInfo> iterator = planLinkInfo.iterator();
		while ( iterator.hasNext() ) {
			final PlanLinkInfo curr = iterator.next();
			if ( !knownPlans.add( planId( curr.id1 , curr.plan1 ) ) ) iterator.remove();
		}
	}

	private static boolean pointsBackwards(
			final PlanLinkInfo info,
			final List<AgentPlanInfo> planInfos) {
		for ( AgentPlanInfo agent : planInfos ) {
			if ( agent.id.equals( info.id1 ) ) return false;
			if ( agent.id.equals( info.id2 ) ) return true;
		}
		throw new RuntimeException();
	}

	private static void writeBeginning(
			final BufferedWriter writer) throws IOException {
		writer.write( "\\documentclass{article}" );
		writer.newLine();
		writer.write( "\\usepackage{tikz}" );
		writer.write( "\\usepackage[landscape]{geometry}" );
		writer.newLine();
		writer.newLine();
		writer.write( "\\begin{document}" );
		writer.write( "\\begin{figure}" );
		writer.write( "\\begin{center}" );
		writer.newLine();
		writer.write( "% cut here -----------------------------------------------" );
		writer.newLine();
		writer.newLine();

		writer.write( "\\begin{tikzpicture}" );
		writer.newLine();
		writer.write( "\\tikzstyle{"+AGENT_STYLE+"}=[rotate=45,anchor=south west]" );
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
			writer.write( "\\node ("+last+") "+pos+" {};" );
			writer.write( "\\node "+pos+" ["+AGENT_STYLE+"] {"+info.id+"};" );
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
		writer.newLine();
		writer.newLine();
		writer.write( "% cut here -----------------------------------------------" );
		writer.newLine();
		writer.write( "\\end{center}" );
		writer.write( "\\end{figure}" );
		writer.write( "\\end{document}" );
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
