/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.framework.population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class SocialNetworkWriter extends MatsimXmlWriter {
	private static final Logger log =
		Logger.getLogger(SocialNetworkWriter.class);

	public static final String ROOT_TAG = "socialnet";
	public static final String EGO_TAG = "ego";
	public static final String TIE_TAG = "tie";

	public static final String REFLECTIVE_ATT = "isReflective";
	public static final String EGO_ATT = "egoId";
	public static final String ALTER_ATT = "alterId";

	public static final String METADATA_TAG = "metadata";
	public static final String ATTRIBUTE_TAG = "attribute";
	public static final String NAME_ATT = "name";
	public static final String VALUE_ATT = "value";

	private final SocialNetwork network;

	public SocialNetworkWriter( final SocialNetwork network ) {
		this.network = network;
	}

	public void write( final String file ) {
		this.openFile( file );
		log.info( "Start writing social network in file "+file );
		this.writeXmlHead();
		this.writeDoctype( ROOT_TAG , "socialnetwork_v1.dtd" );
		this.writeStartTag(
				ROOT_TAG,
				Collections.singletonList(
					createTuple(
						REFLECTIVE_ATT,
						network.isReflective() ) ) );
		writeMetadata();
		writeEgos();
		writeNetwork( );
		this.writeEndTag( ROOT_TAG );
		this.close();
		log.info( "Finished writing social network in file "+file );
	}

	private void writeMetadata() {
		if ( network.getMetadata().isEmpty() ) return;
		writeStartTag( METADATA_TAG , Collections.<Tuple<String, String>>emptyList() );

		for ( Map.Entry<String, String> e : network.getMetadata().entrySet() ) {
			writeStartTag(
					ATTRIBUTE_TAG,
					Arrays.asList(
						createTuple(
							NAME_ATT,
							e.getKey() ),
						createTuple(
							VALUE_ATT,
							e.getValue() ) ),
					true);
		}

		writeEndTag( METADATA_TAG );
		writeContent( "" , true ); // to skip a line
	}

	private void writeEgos() {
		log.info( "start writing egos..." );
		final Counter counter = new Counter( "[SocialNetworkWriter] writing ego # " );
		for ( Id ego : network.getEgos() ) {
			counter.incCounter();
			final List<Tuple<String,String>> atts = new ArrayList<Tuple<String, String>>();
			atts.add( createTuple( EGO_ATT , ego.toString() ) );
			writeStartTag( EGO_TAG , atts , true );
		}
		counter.printCounter();
		log.info( "finished writing egos." );
	}

	private void writeNetwork() {
		log.info( "start writing ties..." );
		final Counter counter = new Counter( "[SocialNetworkWriter] writing tie # " );
		final Set<Id> dumpedEgos = new HashSet<Id>();
		final boolean reflective = network.isReflective();
		for ( Id ego : network.getEgos() ) {
			for ( Id alter : network.getAlters( ego ) ) {
				if ( reflective && dumpedEgos.contains( alter ) ) continue;

				counter.incCounter();
				final List<Tuple<String,String>> atts = new ArrayList<Tuple<String, String>>();
				atts.add( createTuple( EGO_ATT , ego.toString() ) );
				atts.add( createTuple( ALTER_ATT , alter.toString() ) );
				writeStartTag( TIE_TAG , atts , true );
			}
			dumpedEgos.add( ego );
		}
		counter.printCounter();
		log.info( "finished writing ties." );
	}
}

