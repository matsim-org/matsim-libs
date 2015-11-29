/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedSocialNetworkReader.java
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

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * @author thibautd
 */
public class WeightedSocialNetworkReader extends MatsimXmlParser {
	
	private WeightedSocialNetwork network = null;

	int currentEgo = -1;

	public WeightedSocialNetworkReader() {
		// do not validate, as not yet a dtd
		super( false );
	}

	public WeightedSocialNetwork read( final String file ) {
		parse( file );
		return network;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context ) {
		switch ( name ) {
			case "weightedSocialNetwork":
				network = new WeightedSocialNetwork(
						Integer.parseInt( atts.getValue( "maxSize" ) ),
						Double.parseDouble( atts.getValue( "lowestAllowedWeight" ) ),
						Integer.parseInt( atts.getValue( "popSize" ) ) );
				break;
			case "ego":
				currentEgo = Integer.parseInt( atts.getValue( "index" ) );
				break;
			case "alter":
				network.addBidirectionalTie(
						currentEgo, 
						Integer.parseInt( atts.getValue( "index" ) ),
						Double.parseDouble( atts.getValue( "weight" ) ) );
				break;
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context ) {
		switch ( name ) {
			case "ego":
				currentEgo = -1;
				break;
			case "weightedSocialNetwork":
				network.trimAll();
				break;
		}

	}
}

