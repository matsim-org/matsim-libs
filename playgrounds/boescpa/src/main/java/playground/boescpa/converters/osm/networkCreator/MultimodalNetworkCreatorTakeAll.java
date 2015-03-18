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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.boescpa.lib.tools.merger.NetworkMerger;

/**
 * A version of MultimodalNetworkCreator that creates a simple street network from the OSM file.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorTakeAll extends MultimodalNetworkCreator {

	private final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	private final Network streetNetwork;
	private final Network highwayNetwork;

	public MultimodalNetworkCreatorTakeAll(Network network) {
		super(network);
		streetNetwork = NetworkUtils.createNetwork();
		highwayNetwork = NetworkUtils.createNetwork();
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReader reader =
				new OsmNetworkReader(this.network, transformation);
		// Ellipse around Switzerland
		reader.setHierarchyLayer(new OsmFilter.OsmFilterTakeAll(6));
		// Take all highway links...
		reader.setHierarchyLayer(new OsmFilter.OsmFilterTakeAll(2));
		reader.parse(osmFile);
	}

	public void addStreetNetwork(String osmFile) {
		// Create Street Network
		OsmNetworkReader reader =
				new OsmNetworkReader(streetNetwork, transformation);
		// Ellipse around Switzerland
		reader.setHierarchyLayer(new OsmFilter.OsmFilterTakeAll(6));
		reader.parse(osmFile);
		// Merge networks:
		NetworkMerger.integrateNetwork(this.network, streetNetwork, "", "st");
	}

	public void addHighwayNetwork(final String osmFile) {
		// Create Highway Network
		OsmNetworkReader reader =
				new OsmNetworkReader(highwayNetwork, transformation);
		// Take all highway links...
		reader.setHierarchyLayer(new OsmFilter.OsmFilterTakeAll(2));
		reader.parse(osmFile);
		// Merge networks:
		NetworkMerger.integrateNetwork(this.network, highwayNetwork, "", "hw");
	}

}
