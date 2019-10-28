/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.mzilske.osm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.FastXmlReader;

import java.io.File;
import java.util.*;

public class BayAreaMain {

	public static void main(String[] args) {
		for (int hierarchyLevel=1; hierarchyLevel <= 6; hierarchyLevel++) {
			Scenario scenario = createScenario("/Users/zilske/Documents/osm-bayarea/bayarea-"+hierarchyLevel+".osm.xml");
			new NetworkCleaner().run(scenario.getNetwork());
			new NetworkWriter(scenario.getNetwork()).write("/Users/zilske/Documents/osm-bayarea/network-"+hierarchyLevel+".xml");
		}
	}

	private static Scenario createScenario(String networkFilename) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street")));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);

		FastXmlReader reader = new FastXmlReader(new File(networkFilename ), true, CompressionMethod.None);

		SimplifyTask simplify = new SimplifyTask(IdTrackerType.BitSet);

		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:32710");
		NetworkSink sink = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		sink.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		sink.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		sink.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, false);
		sink.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, false);
		sink.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, false);
		sink.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		sink.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);

		reader.setSink(tagFilter);
		tagFilter.setSink(simplify);
		simplify.setSink(sink);
		sink.setSink(new Sink() {

			@Override
			public void initialize(Map<String, Object> map) {

			}

			@Override
			public void process(EntityContainer entityContainer) {

			}

			@Override
			public void complete() {

			}

			@Override
			public void release() {

			}

		});

		reader.run();
		return scenario;
	}

}
