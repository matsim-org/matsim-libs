/* *********************************************************************** *
 * project: org.matsim.*
 * ReadNetwork.java
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
 * *********************************************************************** */

package playground.gregor.casim.helper;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

public class ReadNetwork {
	
	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		String osmFile = "/Users/laemmel/devel/venice/osm/map.osm";
		CoordinateTransformation ct = new GeotoolsTransformation("WGS84","EPSG:3395");
		CustomizedOsmNetworkReader reader = new CustomizedOsmNetworkReader(sc.getNetwork(), ct);
		reader.setKeepPaths(true);
		
		double laneCap = 2808 * 2; // 2 lanes

		reader.setHighwayDefaults(2, "trunk", 2, 1.34, 1., laneCap);
		reader.setHighwayDefaults(2, "trunk_link", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(3, "primary", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(3, "primary_link", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(4, "secondary", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(5, "tertiary", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "minor", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "unclassified", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "residential", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "living_street", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "path", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "cycleway", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "footway", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "steps", 2, 1.34, 1.0, laneCap);
		reader.setHighwayDefaults(6, "pedestrian", 2, 1.34, 1.0, laneCap);

		reader.parse(osmFile);
		// max density is set to 5.4 p/m^2
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.6);
		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.31);
		
		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/devel/venice/input/network.xml.gz");
	}

}
