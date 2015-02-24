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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import playground.boescpa.lib.tools.merger.NetworkMerger;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorSimple extends MultimodalNetworkCreator {

	private final Network streetNetwork;
	private final Network highwayNetwork;

	public MultimodalNetworkCreatorSimple(Network network) {
		super(network);
		streetNetwork = getEmptyNetwork();
		highwayNetwork = getEmptyNetwork();
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReader reader =
				new OsmNetworkReader(this.network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"));
		reader.setHierarchyLayer(47.811547,5.936507,45.834786,10.517806,6); // Rectangle around Switzerland
		reader.setHierarchyLayer(48.811547,4.936507,44.834786,11.517806,2); // Rectangle around Switzerland + 1 degree distance in each direction
		reader.parse(osmFile);
	}

	public void addStreetNetwork(String osmFile) {
		// Create Street Network
		OsmNetworkReader reader =
				new OsmNetworkReader(streetNetwork, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"));
		reader.setHierarchyLayer(47.811547,5.936507,45.834786,10.517806,6); // Rectangle around Switzerland
		reader.parse(osmFile);

		// Merge networks:
		NetworkMerger.integrateNetwork(this.network, streetNetwork, "", "st");
	}

	public void addHighwayNetwork(final String osmFile) {
		// Create Highway Network
		OsmNetworkReader reader = new OsmNetworkReader(highwayNetwork, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"));
		reader.setHierarchyLayer(48.811547,4.936507,44.834786,11.517806,2); // Rectangle around Switzerland + 1 degree distance in each direction
		reader.parse(osmFile);

		// Merge networks:
		NetworkMerger.integrateNetwork(this.network, highwayNetwork, "", "hw");
	}

	protected Network getEmptyNetwork() {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		return scenario.getNetwork();
	}

}
