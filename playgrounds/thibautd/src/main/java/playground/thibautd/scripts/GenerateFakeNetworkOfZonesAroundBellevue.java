/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateFakeNetworkOfZonesAroundBellevue.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Collections;

/**
 * @author thibautd
 */
public class GenerateFakeNetworkOfZonesAroundBellevue {
	private static final double BELLEVUE_X = 683518;
	private static final double BELLEVUE_Y = 246836;
	private static final double[] radii = new double[]{ 10000 , 15000 , 20000 , 30000 };
	private static final double stepDeg = 2;

	public static void main(final String[] args) {
		final String outputNetworkFile = args[ 0 ];

		final Network net = ScenarioUtils.createScenario(
				ConfigUtils.createConfig() ).getNetwork();

		for ( double radius : radii ) {
			addCircle( radius , net );
		}

		new NetworkWriter( net ).write( outputNetworkFile );
	}

	private static void addCircle(
			final double radius,
			final Network net) {
		Node lastNode = null;
		for ( double angle = 0 ; angle <= 360.1 ; angle += stepDeg ) {
			final Node newNode = net.getFactory().createNode(
					Id.create( (int)radius +"~"+ (int)angle , Node.class ),
					new Coord(BELLEVUE_X + radius * Math.cos(Math.PI * angle / 180), BELLEVUE_Y + radius * Math.sin(Math.PI * angle / 180)));
			net.addNode( newNode );

			if ( lastNode != null ) {
				final Link l = net.getFactory().createLink(
						Id.create( lastNode.getId() +"^"+ newNode.getId() , Link.class ),
						lastNode,
						newNode );
				net.addLink( l );
				l.setAllowedModes( Collections.singleton( ""+radius ) );
			}

			lastNode = newNode;
		}
	}
}

