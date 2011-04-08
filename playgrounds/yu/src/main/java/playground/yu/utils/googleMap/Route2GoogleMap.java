/* *********************************************************************** *
 * project: org.matsim.*
 * Route2GoogleMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.utils.googleMap;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * shows routes in google maps with google map static API supporting latitude &
 * longitude (xx.xxxxxx) or WGS84 formats
 * 
 * @author yu
 * 
 */
public class Route2GoogleMap {
	/** converts coordinate system to WGS84 */
	private CoordinateTransformation coordTransform;
	private NetworkRoute route;
	private Network network;

	public Route2GoogleMap(String fromSystem, Network network,
			NetworkRoute route) {
		coordTransform = TransformationFactory.getCoordinateTransformation(
				fromSystem, TransformationFactory.WGS84);
		this.network = network;
		this.route = route;
	}

	public String getPath4googleMap() {
		Id startLinkId = route.getStartLinkId();
		String startMarkers = null;

		List<Id> linkIds = route.getLinkIds();
		Id endLinkId = route.getEndLinkId();
		// TODO
		return null;
	}

	protected String createMarker(Id linkId) {
		// TODO
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
}
