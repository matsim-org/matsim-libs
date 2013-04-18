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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	private static final String LINK_STYLE = "link";

	private static final String LEGEND_ANCHOR = "legend-anchor";

	private final Set<Property> properties = new HashSet<Property>();

	private final Map<String, AgentPlanInfo> agentInfos = new LinkedHashMap<String,AgentPlanInfo>();
	private final List<PlanLinkInfo> linkInfos = new ArrayList<PlanLinkInfo>();

	public void addAgentInfo(
			final String id,
			final int nPlans) {
		this.agentInfos.put( id , new AgentPlanInfo( id , nPlans ) );
	}

	public Property overridePlanProperty(
			final String id,
			final int planIndex,
			final String propertyName) {
		final AgentPlanInfo a = this.agentInfos.get( id );
		if ( a == null ) throw new IllegalStateException( "unkown agent "+id );

		final Property property = new Property( propertyName );
		final Property old = a.planProperties.put( planIndex , property );
		properties.add( property );
		return old;
	}

	public void setPlanProperty(
			final String id,
			final int planIndex,
			final String propertyName) {
		final Property old = overridePlanProperty( id , planIndex , propertyName );
		if ( old != null )  throw new IllegalStateException( "cannot replace property "+old+" with "+propertyName+" for "+id );
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
		final List<AgentPlanInfo> agentList = new ArrayList<AgentPlanInfo>( agentInfos.values() );
		sortPersons( agentList , linkInfos );
		final List<PlanLinkInfo> cleanLinks = new ArrayList<PlanLinkInfo>( linkInfos );
		cleanLinks( agentList , cleanLinks );

		try {
			log.info( "write tex file" );
			final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
			writeBeginning( writer , properties );
			writePlans( writer , agentList );
			writeJointPlans( writer , agentList , cleanLinks );
			writeLegend( writer , properties );
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
			final BufferedWriter writer,
			final Collection<Property> properties) {
		MoreIOUtils.writeLines(
				writer,
				"\\documentclass{article}",
				"\\usepackage{tikz}",
				"\\usepackage[landscape]{geometry}",
				"\\pgfdeclarelayer{bg}",
				"\\pgfsetlayers{bg,main} % put a bg layer under the main layer",
				"");
		MoreIOUtils.writeLines(
				writer,
				createColorDefs( properties ) );

		MoreIOUtils.writeLines(
				writer,
				"\\begin{document}",
				"\\begin{figure}",
				"\\begin{center}",
				"% cut here -----------------------------------------------",
				"",
				"\\begin{tikzpicture}",
				"\\tikzstyle{"+AGENT_STYLE+"}=[rotate=45,anchor=south west]",
				"\\tikzstyle{"+PLAN_STYLE+"}=[rectangle,fill=white,draw]",
				"\\tikzstyle{"+LINK_STYLE+"}=[]");

		MoreIOUtils.writeLines(
				writer,
				createStyleLinesForProperties( properties ) );
	}

	private static void writePlans(
			final BufferedWriter writer,
			final List<AgentPlanInfo> agentPlanInfo) throws IOException {
		writer.newLine();
		String last = null;
		String leftmost = null;
		String downmost = null;
		int downsize = -1;
		for ( AgentPlanInfo info : agentPlanInfo ) {
			writer.newLine();
			final String pos = last == null ? "" : "[right of="+last+"]";
			last = ""+info.id;
			if ( leftmost == null ) leftmost = last;

			MoreIOUtils.writeLines(
				writer,
				"\\node ("+last+") "+pos+" {};",
				"\\node "+pos+" ["+AGENT_STYLE+"] {"+info.id+"};" );
			writer.newLine();
			String lastPlan = last;
			for ( int i=0; i < info.nPlans; i++ ) {
				final Property planProperty = info.planProperties.get( i );
				final String style =
					planProperty != null ?
						styleName( planProperty ) :
						PLAN_STYLE;
				final String planPos = "[below of="+lastPlan+"]";
				lastPlan = planString( last , i );
				MoreIOUtils.writeLines(
						writer,
						"\\node ("+lastPlan+") "+planPos+" ["+style+"] {};" );
			}

			if ( info.nPlans > downsize ) {
				downmost = lastPlan;
				downsize = info.nPlans;
			}
		}

		assert leftmost != null;
		assert downmost != null;
		MoreIOUtils.writeLines(
				writer,
				"\\node ("+LEGEND_ANCHOR+") at ("+leftmost+" |- "+downmost+") {};" );
	}

	private static void writeLegend(
			final BufferedWriter writer,
			final Collection<Property> properties) {
		String anchor = LEGEND_ANCHOR;

		int c = 0;
		for ( Property p : properties ) {
			final String nodeName = "leg-"+(c++);
			MoreIOUtils.writeLines(
					writer,
					"\\node ("+nodeName+") [below of="+anchor+",rectangle,draw,fill="+colorName( p )+"] {};",
					"\\node [right of="+nodeName+",anchor=west] {"+p+"};");
			anchor = nodeName;
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

	private static String styleName( final Property p ) {
		return p.toString();
	}

	private static String[] createStyleLinesForProperties(
			final Collection<Property> ps) {
		final List<String> lines = new ArrayList<String>();

		for ( Property p : ps ) {
			lines.add( "\\tikzstyle{"+styleName( p )+"}=[rectangle,fill="+colorName( p )+",draw]" );
		}

		return lines.toArray( new String[0] );
	}

	private static String colorName(final Property p) {
		return "mycolor-"+p;
	}

	private static String[] createColorDefs(final Collection<Property> properties) {
		final List<String> list = new ArrayList<String>();

		final double step = 6. / (properties.size() + 1);
		double col = 0;
		for ( Property prop : properties ) {
			final double g = col < 1 ? 1 : col < 2 ? 2 - col : col < 4 ? 0 : col < 5 ? col - 4 : 1;
			final double r = col < 1 ? col : col < 3 ? 1 : col < 4 ? 5 - col : 0;
			final double b = col < 2 ? 0 : col < 3 ? col - 2 : col < 5 ? 1 : 5 - col;

			list.add( "\\definecolor{"+colorName( prop )+"}{rgb}{"+r+","+g+","+b+"}" );
			col += step;
		}

		assert Math.abs( col - (6. - step) ) < 1E-7;
		assert list.size() == properties.size();

		return list.toArray(new String[0] );
	}
}

class Property {
	private final String s;

	public Property(final String s) {
		this.s = s;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof Property &&
			((Property) o).s.equals( s );
	}

	@Override
	public int hashCode() {
		return s.hashCode();
	}

	@Override
	public String toString() {
		return s;
	}
}

class AgentPlanInfo {
	public final String id;
	public final int nPlans;
	public final Map<Integer, Property> planProperties = new HashMap<Integer, Property>();

	public AgentPlanInfo(
			final String id,
			final int nPlans) {
		this.id = id;
		this.nPlans = nPlans;
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

