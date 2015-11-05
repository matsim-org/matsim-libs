/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.boescpa.lib.tools.coordUtils.CoordAnalyzer;
import playground.boescpa.lib.tools.shpUtils.SHPFileUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class MsNetworkMapper extends AbstractNetworkMapper {

	/**
	 * Read matsim network and cut it to zones.
	 *
	 * @param path2MATSimNetwork
	 * @param path2VissimZoneShp
	 * @return The prepared matsim network.
	 */
	@Override
	protected Network providePreparedNetwork(String path2MATSimNetwork, String path2VissimZoneShp) {
		// Read network
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
		NetworkReader.readFile(path2MATSimNetwork);
		Network network = scenario.getNetwork();
		// Prepare zones and identifier.
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(path2VissimZoneShp));
		SHPFileUtil util = new SHPFileUtil();
		Geometry cuttingArea = util.mergeGeometries(features);
		CoordAnalyzer coordAnalyzer = new CoordAnalyzer(cuttingArea);
		// Identify links not in zones.
		Set<Link> linkSet2Remove = new HashSet<Link>();
		for (Link link : network.getLinks().values()) {
			if (!coordAnalyzer.isLinkAffected(link)) {
				linkSet2Remove.add(link);
			}
		}
		// Remove links not in zones.
		for (Link link : linkSet2Remove) {
			network.removeLink(link.getId());
		}
		return network;
	}

}
