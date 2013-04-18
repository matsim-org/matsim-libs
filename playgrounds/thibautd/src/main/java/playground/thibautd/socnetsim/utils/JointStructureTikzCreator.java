/* *********************************************************************** *
 * project: org.matsim.*
 * JointStructureTikzCreator.java
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
package playground.thibautd.socnetsim.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class JointStructureTikzCreator {
	private static final Logger log =
		Logger.getLogger(JointStructureTikzCreator.class);

	private static final String AGENT_STYLE = "agent";
	private static final String PLAN_STYLE = "plan";
	private static final String SELECTED_PLAN_STYLE = "selectedplan";
	private static final String LINK_STYLE = "link";

	private final List<AgentPlanInfo> agentInfos = new ArrayList<AgentPlanInfo>();
	private final List<PlanLinkInfo> linkInfos = new ArrayList<PlanLinkInfo>();

	public void addAgentInfo(
			final String id,
			final int nPlans,
			final int selectedPlan) {
		this.agentInfos.add( new AgentPlanInfo( id , nPlans , selectedPlan ) );
	}

	public void addPlanLinkInfo(
			final String agent1,
			final int plan1,
			final String agent2,
			final int plan2) {
		this.linkInfos.add( new PlanLinkInfo( agent1 , plan1 , agent2 , plan2 ) );
	}
		
	public void writeTexFile( final String outFile ) {
		log.info( "clean info for visual appeal" );
		sortPersons( agentInfos , linkInfos );
		final List<PlanLinkInfo> cleanLinks = new ArrayList<PlanLinkInfo>( linkInfos );
		cleanLinks( agentInfos , cleanLinks );

		try {
			log.info( "write tex file" );
			final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
			writeBeginning( writer );
			writePlans( writer , agentInfos );
			writeJointPlans( writer , agentInfos , cleanLinks );
			writeEnd( writer );
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
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
			if ( !knownPlans.add( planString( curr.id1 , curr.plan1 ) ) ) iterator.remove();
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
			final BufferedWriter writer) {
		MoreIOUtils.writeLines(
				writer,
				"\\documentclass{article}",
				"\\usepackage{tikz}",
				"\\usepackage[landscape]{geometry}",
				"\\pgfdeclarelayer{bg}",
				"\\pgfsetlayers{bg,main} % put a bg layer under the main layer",
				"",
				"\\begin{document}",
				"\\begin{figure}",
				"\\begin{center}",
				"% cut here -----------------------------------------------",
				"",
				"\\begin{tikzpicture}",
				"\\tikzstyle{"+AGENT_STYLE+"}=[rotate=45,anchor=south west]",
				"\\tikzstyle{"+PLAN_STYLE+"}=[rectangle,fill=white,draw]",
				"\\tikzstyle{"+SELECTED_PLAN_STYLE+"}=[rectangle,fill=green,draw]",
				"\\tikzstyle{"+LINK_STYLE+"}=[]");
	}

	private static void writePlans(
			final BufferedWriter writer,
			final List<AgentPlanInfo> agentPlanInfo) throws IOException {
		writer.newLine();
		String last = null;
		for ( AgentPlanInfo info : agentPlanInfo ) {
			writer.newLine();
			final String pos = last == null ? "" : "[right of="+last+"]";
			last = ""+info.id;
			MoreIOUtils.writeLines(
				writer,
				"\\node ("+last+") "+pos+" {};",
				"\\node "+pos+" ["+AGENT_STYLE+"] {"+info.id+"};" );
			writer.newLine();
			String lastPlan = last;
			for ( int i=0; i < info.nPlans; i++ ) {
				final String style = i == info.selectedPlan ? SELECTED_PLAN_STYLE : PLAN_STYLE;
				final String planPos = "[below of="+lastPlan+"]";
				lastPlan = planString( last , i );
				MoreIOUtils.writeLines(
						writer,
						"\\node ("+lastPlan+") "+planPos+" ["+style+"] {};" );
			}
		}
	}

	private static String planString(final String personString, final int plan) {
		return personString+"-plan-"+plan;
	}

	private static void writeJointPlans(
			final BufferedWriter writer,
			final List<AgentPlanInfo> agentPlanInfo,
			final List<PlanLinkInfo> linkInfo) throws IOException {
		writer.newLine();
		MoreIOUtils.writeLines(
				writer,
				"% write links \"under\" the nodes, otherwise parabolas do not look nice",
				"\\begin{pgfonlayer}{bg}" );
		for ( PlanLinkInfo info : linkInfo ) {
			final String start = planString( info.id1 , info.plan1 );
			final String end = planString( info.id2 , info.plan2 );
			final String curve = areContinguous( info.id1 , info.id2 , agentPlanInfo ) ?
				"to[out=0,in=180]" : "parabola[parabola height=6mm]";
			MoreIOUtils.writeLines(
					writer,
					"\\draw ["+LINK_STYLE+"] ("+start+") "+curve+" ("+end+");" );
		}
		MoreIOUtils.writeLines(
				writer,
				"\\end{pgfonlayer}{bg}" );
	}

	private static boolean areContinguous(
			final String id1,
			final String id2,
			final List<AgentPlanInfo> agentPlanInfo) {
		boolean foundOne = false;
		for (AgentPlanInfo info : agentPlanInfo) {
			if ( foundOne ) {
				return info.id.equals( id1 ) || info.id.equals( id2 );
			}
			if ( info.id.equals( id1 ) || info.id.equals( id2 ) ) foundOne = true;
		}
		return false;
	}

	private static void writeEnd(
			final BufferedWriter writer) {
		MoreIOUtils.writeLines(
				writer,
				"\\end{tikzpicture}",
				"",
				"% cut here -----------------------------------------------",
				"\\end{center}",
				"\\end{figure}",
				"\\end{document}");
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

