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
package playground.thibautd.socnetsim.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author thibautd
 */
public class SocialNetworkWriter extends MatsimXmlWriter {
	public static final String ROOT_TAG = "socialnet";
	public static final String TIE_TAG = "tie";

	public static final String REFLECTIVE_ATT = "isReflective";
	public static final String EGO_ATT = "egoId";
	public static final String ALTER_ATT = "alterId";

	private final SocialNetwork network;

	public SocialNetworkWriter( final SocialNetwork network ) {
		this.network = network;
	}

	public void write( final String file ) {
		this.openFile( file );
		this.writeStartTag(
				ROOT_TAG,
				Collections.singletonList(
					createTuple(
						REFLECTIVE_ATT,
						network.isReflective() ) ) );
		writeNetwork( );
		this.writeEndTag( ROOT_TAG );
		this.close();
	}

	private void writeNetwork() {
		final Set<Id> dumpedEgos = new HashSet<Id>();
		final boolean reflective = network.isReflective();
		for ( Id ego : network.getEgos() ) {
			for ( Id alter : network.getAlters( ego ) ) {
				if ( reflective && dumpedEgos.contains( alter ) ) continue;

				final List<Tuple<String,String>> atts = new ArrayList<Tuple<String, String>>();
				atts.add( createTuple( EGO_ATT , ego.toString() ) );
				atts.add( createTuple( ALTER_ATT , alter.toString() ) );
				writeStartTag( TIE_TAG , atts , true );
			}
			dumpedEgos.add( ego );
		}
	}
}

