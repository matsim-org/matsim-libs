/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkWriter.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author thibautd
 */
public class SocialNetworkWriter extends MatsimXmlWriter {
	public static final String ROOT_TAG = "socialnet";
	public static final String TIE_TAG = "tie";
	public static final String EGO_ATT = "egoId";
	public static final String ALTER_ATT = "alterId";

	public void write( final String file , final SocialNetwork network ) {
		this.openFile( file );
		this.writeStartTag( ROOT_TAG , Collections.<Tuple<String,String>>emptyList() );
		writeNetwork( network );
		this.writeEndTag( ROOT_TAG );
		this.close();
	}

	private void writeNetwork(final SocialNetwork network) {
		for ( Tie tie : network.getTies() ) {
			final List<Tuple<String,String>> atts = new ArrayList<Tuple<String, String>>();
			atts.add( createTuple( EGO_ATT , tie.getFirstId().toString() ) );
			atts.add( createTuple( ALTER_ATT , tie.getSecondId().toString() ) );
			writeStartTag( TIE_TAG , atts , true );
		}
	}
}

