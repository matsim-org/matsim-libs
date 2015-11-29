/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSlopeScorer.java
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
package playground.thibautd.router.multimodal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.Map;

/**
 * @author thibautd
 */
public class LinkSlopeScorer {
	final Network network;
	final Map<Id<Link>, Double> linkSlopes;
	final double betaGain;

	public LinkSlopeScorer(
			final Network network,
			final Map<Id<Link>, Double> linkSlopes,
			final double betaGain ) {
		if ( betaGain > 0 ) throw new IllegalArgumentException( "elevation gain is assumed to have negative utility. Got "+betaGain );
		this.betaGain = betaGain;
		this.network = network;
		this.linkSlopes = linkSlopes;
	}

	public double calcGainUtil( final Id<Link> link ) {
		return calcGainUtil( network.getLinks().get( link ) );
	}

	public double calcGainUtil( final Link link ) {
		final double gain = link.getLength() * linkSlopes.get( link.getId() );
		return betaGain * Math.max( 0 , gain );
	}

	public double calcGainUtil( final NetworkRoute route ) {
		double score = 0;

		for ( final Id<Link> linkId : route.getLinkIds() ) {
			score += calcGainUtil( linkId );
		}

		score += calcGainUtil( route.getEndLinkId() );

		return score;

	}
}

