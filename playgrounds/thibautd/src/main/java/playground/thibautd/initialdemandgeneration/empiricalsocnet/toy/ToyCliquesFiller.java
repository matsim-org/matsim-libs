/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliqueStub;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.utils.RandomUtils;
import playground.thibautd.utils.spatialcollections.SpatialTree;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class ToyCliquesFiller implements CliquesFiller {
	private final Random random = new Random( 234 );
	private final ToySocialNetworkConfigGroup configGroup;

	@Inject
	public ToyCliquesFiller( final ToySocialNetworkConfigGroup configGroup ) {
		this.configGroup = configGroup;
	}

	@Override
	public Set<Ego> sampleClique(
			final CliqueStub stub,
			final SpatialTree<double[], CliqueStub> freeStubs ) {
		final Coord center = (Coord) stub.getEgo().getPerson().getCustomAttributes().get( "coord" );

		final Set<Ego> clique = new HashSet<>();
		clique.add( stub.getEgo() );
		freeStubs.remove( stub );
		for ( int i=0; i < stub.getCliqueSize(); i++ ) {
			final Coord newCoord = sampleCoord( center );
			final CliqueStub alterStub =
					freeStubs.getClosest(
							new double[]{ newCoord.getX() , newCoord.getY() },
							s -> !clique.contains( s.getEgo() ) );

			// nothing left
			if ( alterStub == null ) return clique;

			clique.add( alterStub.getEgo() );
			freeStubs.remove( alterStub );
		}

		return clique;
	}

	private Coord sampleCoord( final Coord center ) {
		final double distance = RandomUtils.nextLognormal( random , configGroup.getLognormalLocation_m() , configGroup.getLognormalScale_m() );
		final double bearing = random.nextDouble() * 2 * Math.PI;

		final double dX = Math.cos( bearing ) * distance;
		final double dY = Math.sin( bearing ) * distance;

		return new Coord( center.getX() + dX , center.getY() + dY );
	}
}
