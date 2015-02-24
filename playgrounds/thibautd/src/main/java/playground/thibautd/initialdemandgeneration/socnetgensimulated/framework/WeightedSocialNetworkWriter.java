/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedSocialNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.Arrays;
import java.util.Collections;

import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.WeightedSocialNetwork.WeightedAlter;

/**
 * @author thibautd
 */
public class WeightedSocialNetworkWriter extends MatsimXmlWriter {

	public void write( final WeightedSocialNetwork sn , final String file ) {
		openFile( file );
		writeXmlHead();
		// TODO dtd
		//writeDoctype( "weightedSocialNetwork" , "weightedSocialNetwork.dtd" );
		writeStartTag(
				"weightedSocialNetwork",
				Arrays.asList(
					createTuple(
						"lowestAllowedWeight",
						sn.getLowestAllowedWeight() ),
					createTuple(
						"maxSize",
						sn.getMaximalSize() ),
					createTuple(
						"popSize",
						sn.getNEgos() ) ) );
		for ( int i=0; i < sn.getNEgos(); i ++ ) {
			writeStartTag( "ego" , Collections.singletonList( createTuple( "index" , i ) ) );
			for ( WeightedAlter a : sn.getAlters( i ) ) {
				// only write upper half of matrix (reflective)
				if ( a.alter < i ) continue;

				writeStartTag(
						"alter",
						Arrays.asList(
							createTuple( 
								"index",
								a.alter ),
							createTuple(
								"weight",
								a.weight ) ),
						true );
			}
			writeEndTag( "ego" );
		}
		writeEndTag( "weightedSocialNetwork" );
		close();
	}
}

