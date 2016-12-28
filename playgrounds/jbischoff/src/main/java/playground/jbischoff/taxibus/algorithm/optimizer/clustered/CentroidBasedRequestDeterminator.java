/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.taxibus.algorithm.optimizer.clustered;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CentroidBasedRequestDeterminator implements RequestDeterminator {

	final double radius1;
	final double radius2;
	final Coord coord1;
	final Coord coord2;
	/**
	 * 
	 */
	public CentroidBasedRequestDeterminator(Coord coord1, Coord coord2, double radius1, double radius2) {
			this.coord1=coord1;	
			this.coord2=coord2;
			this.radius1=radius1;
			this.radius2=radius2;
	}
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.taxibus.algorithm.optimizer.clustered.RequestDeterminator#isRequestServable(org.matsim.contrib.dvrp.data.Request)
	 */
	@Override
	public boolean isRequestServable(Request request) {
		TaxibusRequest r = (TaxibusRequest) request;
		Coord fromCoord = r.getFromLink().getCoord();
		Coord toCoord = r.getToLink().getCoord();
		if (((CoordUtils.calcEuclideanDistance(fromCoord, coord1)<=radius1)&&(CoordUtils.calcEuclideanDistance(toCoord, coord2)<=radius2))
			||((CoordUtils.calcEuclideanDistance(fromCoord, coord2)<=radius2)&&(CoordUtils.calcEuclideanDistance(toCoord, coord1)<=radius1)))
		return true;
		else return false;
	}

}
