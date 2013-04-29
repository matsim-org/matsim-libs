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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.thibautd.socnetsim.utils.JointStructureTikzCreator;

/**
 * @author thibautd
 */
public class CreateTikzFigureOfJpStructure {
	private static final Logger log =
		Logger.getLogger(CreateTikzFigureOfJpStructure.class);

	private static final boolean VEHICLES = true;

	public static void main(final String[] args) {
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

		final JointStructureTikzCreator tikzCreator =
			new JointStructureTikzCreator();
		log.info( "load plan infos" );
		parsePlanInfos( VEHICLES , plansFile , tikzCreator , personIds );
		log.info( "load joint plan infos" );
		parsePlanLinkInfo( jointPlansFile , tikzCreator , personIds );

		tikzCreator.writeTexFile( outFile );
	}

	private static void parsePlanInfos(
			final boolean lookAtVehicles,
			final String plansFile,
			final JointStructureTikzCreator tikzCreator,
			final List<String> personIds) {
		new MatsimXmlParser() {
			private String id = null;
			private int count = 0;
			private int selected = -1;
			private final Map<Integer, String> vehicles = new LinkedHashMap<Integer, String>();
			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "person" ) ) {
					id = atts.getValue( "id" );
					count = 0;
					selected = -1;
					vehicles.clear();
				}
				if ( name.equals( "plan" ) ) {
					if ( atts.getValue( "selected" ).equals( "yes" ) ) {
						assert selected < 0;
						selected = count;
					}
					count++;
				}
				if ( name.equals( "route" ) ) {
					final String v = atts.getValue( "vehicleRefId" );
					if ( v != null ) {
						final String old = vehicles.put( count -1 , v );
						if ( old != null && !old.equals( v ) ) {
							log.warn( "cannot handle plans with several vehicles. only the last vehicle will be kept for plan "+(count-1)+" of person "+id );
						}
					}
				}
			}

			@Override
			public void endTag(
					final String name,
					final String content,
					final Stack<String> context) {
				if ( name.equals( "person" ) && personIds.contains( id ) ) {
					tikzCreator.addAgentInfo( id , count );
					if ( !lookAtVehicles ) {
						tikzCreator.setPlanProperty( id , selected , "selected" );
						return;
					}

					for ( Map.Entry<Integer, String> e : vehicles.entrySet() ) {
						final int plan = e.getKey();
						final String vehicle = e.getValue();
						tikzCreator.setPlanProperty( id , plan , vehicle );
					}
				}
			}
		}.parse( plansFile );
	}

	private static void parsePlanLinkInfo(
			final String jointPlansFile,
			final JointStructureTikzCreator tikzCreator,
			final List<String> personIdsToConsider) {
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
								tikzCreator.addPlanLinkInfo( id1 , plan1 , id2 , plan2 );
							}
						}
					}
				}			
			}
		}.parse( jointPlansFile );
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
