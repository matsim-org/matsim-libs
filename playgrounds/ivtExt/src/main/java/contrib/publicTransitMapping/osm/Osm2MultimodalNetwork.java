/* *********************************************************************** *
 * project: org.matsim.*
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

package contrib.publicTransitMapping.osm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import contrib.publicTransitMapping.config.CreateDefaultOsmConfig;
import contrib.publicTransitMapping.config.OsmConverterConfigGroup;
import contrib.publicTransitMapping.tools.NetworkTools;

/**
 * Abstract class to create a multimodal MATSim network from OSM.
 *
 * @author polettif
 */
public abstract class Osm2MultimodalNetwork {

	protected final OsmConverterConfigGroup config;
	protected Network network;
	protected final CoordinateTransformation transformation;

	/**
	 * Converts an osm file to a MATSim network. The input and output file as well
	 * as conversion parameters are defined in this file. Run {@link CreateDefaultOsmConfig}
	 * to create a default config.
	 *
	 * @param args [0] the config.xml file<br>
	 */
	public static void main(String[] args) {
		if(args.length == 1) {
			run(args[0]);
		} else {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
	}

	/**
	 * Converts an osm file to a MATSim network. The input and output file as well
	 * as conversion parameters are defined in this file. Run {@link CreateDefaultOsmConfig}
	 * to create a default config.
	 *
	 * @param configFile the config.xml file
	 */
	public static void run(String configFile) {
		new OsmMultimodalNetworkConverter(configFile).run();
	}

	/**
	 * Converts an osm file with default conversion parameters.
	 * @param osmFile the osm file
	 * @param outputNetworkFile the path to the output network file
	 * @param outputCoordinateSystem output coordinate system (no transformation is applied if <tt>null</tt>)
	 */
	public static void run(String osmFile, String outputNetworkFile, String outputCoordinateSystem) {
		OsmConverterConfigGroup configGroup = OsmConverterConfigGroup.createDefaultConfig();
		configGroup.setOsmFile(osmFile);
		configGroup.setOutputNetworkFile(outputNetworkFile);
		configGroup.setOutputCoordinateSystem(outputCoordinateSystem);
		new OsmMultimodalNetworkConverter(configGroup).run();
	}

	/**
	 * Constructor reading config from file.
	 */
	public Osm2MultimodalNetwork(final String osmConverterConfigFile) {
		Config configAll = ConfigUtils.loadConfig(osmConverterConfigFile, new OsmConverterConfigGroup() ) ;
		this.config = ConfigUtils.addOrGetModule(configAll, OsmConverterConfigGroup.GROUP_NAME, OsmConverterConfigGroup.class);
		this.network = NetworkTools.createNetwork();
		this.transformation = config.getCoordinateTransformation();
	}

	/**
	 * Constructor using the a OsmCOnverterConfigGroup config.
	 */
	public Osm2MultimodalNetwork(final OsmConverterConfigGroup config) {
		this.config = config;
		this.network = NetworkTools.createNetwork();
		this.transformation = config.getCoordinateTransformation();
	}

	/**
	 * Converts the osm file specified in the config and writes
	 * the network to a file (also defined in config).
	 */
	public abstract void run();

	/**
	 * Parses the osm file and converts it to a MATSim network.
	 */
	public abstract void convert();

	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return this.network;
	}
}
