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

package playground.polettif.boescpa.converters.osm.networkCreator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.boescpa.lib.tools.fileMerging.NetworkMerger;

import java.util.ArrayList;
import java.util.List;

/**
 * A version of MultimodalNetworkCreator that creates a simple street network from the OSM file.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorStreets extends MultimodalNetworkCreator {

	private final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	private final Network streetNetwork;
	private final List<OsmFilter> osmFilter;

	public MultimodalNetworkCreatorStreets(Network network) {
		super(network);
		this.streetNetwork = NetworkUtils.createNetwork();
		this.osmFilter = new ArrayList<>();
	}

	public void addOsmFilter(final OsmFilter osmFilter) {
		this.osmFilter.add(osmFilter);
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReader reader =
				new OsmNetworkReader(this.network, transformation);
		for (OsmFilter filter : this.osmFilter) {
			reader.setHierarchyLayer(filter);
		}
		reader.parse(osmFile);
	}

	public void addNetwork(String osmFile, final OsmFilter osmFilter, String prefix) {
		// Create new Network
		OsmNetworkReader reader =
				new OsmNetworkReader(streetNetwork, transformation);
		// Take all street links...
		reader.setHierarchyLayer(osmFilter);
		reader.parse(osmFile);
		// Merge networks:
		NetworkMerger.integrateNetwork(this.network, streetNetwork, "", prefix);
	}

}
