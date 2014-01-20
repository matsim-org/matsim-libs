/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkReader.java
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

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.thibautd.utils.ObjectPool;

/**
 * @author thibautd
 */
public class SocialNetworkReader extends MatsimXmlParser {
	private final Scenario scenario;
	private SocialNetwork socialNetwork;

	private boolean isReflective = false;
	private final ObjectPool<Id> idPool = new ObjectPool<Id>();

	public SocialNetworkReader(final Scenario scenario) {
		super( false );
		this.scenario = scenario;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals( SocialNetworkWriter.ROOT_TAG ) ) {
			this.isReflective = Boolean.parseBoolean( atts.getValue( SocialNetworkWriter.REFLECTIVE_ATT ) );
			this.socialNetwork = new SocialNetwork( this.isReflective );
			this.scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , this.socialNetwork );
		}
		else if ( name.equals( SocialNetworkWriter.TIE_TAG ) ) {
			final Id ego = idPool.getPooledInstance(
					new IdImpl(
						atts.getValue(
							SocialNetworkWriter.EGO_ATT ) ) );;
			final Id alter = idPool.getPooledInstance(
					new IdImpl(
						atts.getValue(
							SocialNetworkWriter.ALTER_ATT ) ) );;
			if ( this.isReflective ) this.socialNetwork.addBidirectionalTie( ego , alter );
			else this.socialNetwork.addMonodirectionalTie( ego , alter );
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( SocialNetworkWriter.ROOT_TAG ) ) {
			idPool.printStats( "ID Pool" );
		}
	}

}

