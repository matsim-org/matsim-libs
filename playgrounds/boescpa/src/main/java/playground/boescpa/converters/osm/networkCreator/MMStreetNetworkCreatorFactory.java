/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.networkCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class MMStreetNetworkCreatorFactory {

	private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");

	public static MultimodalNetworkCreator getCreatorTakeAll(Network network) {
		MultimodalNetworkCreatorStreets networkCreator = new MultimodalNetworkCreatorStreets(network);
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterTakeAll(6));
		return networkCreator;
	}

	public static MultimodalNetworkCreator getCreatorEllipseAroundSwitzerland(Network network) {
		MultimodalNetworkCreatorStreets networkCreator = new MultimodalNetworkCreatorStreets(network);
		// detailed street network only around Switzerland:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterEllipse(
				transformation.transform(new Coord(8.2363579, 46.7976954)), 150000, 200000, 0.71577, 6));
		// but take all highway network:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterTakeAll(2));
		return networkCreator;
	}

	public static MultimodalNetworkCreator getCreatorRectangleAroundSwitzerland(Network network) {
		MultimodalNetworkCreatorStreets networkCreator = new MultimodalNetworkCreatorStreets(network);
		// detailed street network only around Switzerland:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterRectangle(
				transformation.transform(new Coord(5.936507, 47.811547)), transformation.transform(new Coord(10.517806, 45.834786)), 6));
		// but take all highway network:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterTakeAll(2));
		return networkCreator;
	}

	public static MultimodalNetworkCreator getCreatorSHPCut(Network network, String pathToSHP) {
		MultimodalNetworkCreatorStreets networkCreator = new MultimodalNetworkCreatorStreets(network);
		// detailed street network according to SHP-File:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterShp(pathToSHP, 6));
		// but take all highway network:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterTakeAll(2));
		return networkCreator;
	}

	public static MultimodalNetworkCreator getCircleWithinRectangle(Network network, Coord center, double radius, Coord coordNW, Coord coordSE) {
		MultimodalNetworkCreatorStreets networkCreator = new MultimodalNetworkCreatorStreets(network);
		// detailed street network only in the circle:
		networkCreator.addOsmFilter(new OsmFilter.OSMFilterCircle(
				transformation.transform(center), radius, 6));
		// but take all highway network:
		networkCreator.addOsmFilter(new OsmFilter.OsmFilterRectangle(
				transformation.transform(coordNW), transformation.transform(coordSE), 3));
		return networkCreator;
	}
}
