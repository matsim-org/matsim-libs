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
package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.awt.geom.Rectangle2D;

/**
 * @author thibautd
 */
public class PieSlicesLandmarker implements Landmarker {
	private final Rectangle2D.Double travelZone;

	public PieSlicesLandmarker( Rectangle2D.Double travelZone ) {
		this.travelZone = travelZone;
	}

	@Override
	public Node[] identifyLandmarks( int nLandmarks, Network network ) {
		final LandmarkerPieSlices delegate = new LandmarkerPieSlices( nLandmarks , travelZone );
		delegate.run( network );
		return delegate.getLandmarks();
	}
}
