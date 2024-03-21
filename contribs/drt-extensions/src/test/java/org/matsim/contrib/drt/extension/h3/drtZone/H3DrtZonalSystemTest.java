/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.h3.drtZone;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.h3.H3GridUtils;
import org.matsim.contrib.common.zones.h3.H3ZoneSystemUtils;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.matsim.contrib.common.zones.ZoneSystemUtils.createZoneId;

/**
 * @author nkuehnel / MOIA
 */
public class H3DrtZonalSystemTest {

	@Test
	void test_Holzkirchen_Resolution3() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 3;
		ZoneSystem drtZonalSystem = H3ZoneSystemUtils.createFromPreparedGeometries(network,
			H3GridUtils.createH3GridFromNetwork(network, resolution, crs), crs, resolution);

		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("831f8dfffffffff"))).isTrue();

		// center of Holzkirchen
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(358598)).getId()).isEqualTo(createZoneId("831f8dfffffffff"));
		// Thanning (Western border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(78976)).getId()).isEqualTo(createZoneId("831f8dfffffffff"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(59914)).getId()).isEqualTo(createZoneId("831f89fffffffff"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(drtZonalSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution5() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 5;
		ZoneSystem drtZonalSystem = H3ZoneSystemUtils.createFromPreparedGeometries(network,
			H3GridUtils.createH3GridFromNetwork(network, resolution, crs), crs, resolution);

		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("851f88b7fffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("851f8d6bfffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("851f88a7fffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("851f89d3fffffff"))).isTrue();

		// center of Holzkirchen
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(358598)).getId()).isEqualTo(createZoneId("851f8d6bfffffff"));
		// Thanning (Western border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(78976)).getId()).isEqualTo(createZoneId("851f88b7fffffff"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(59914)).getId()).isEqualTo(createZoneId("851f89d3fffffff"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(drtZonalSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution6() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 6;
		ZoneSystem drtZonalSystem = H3ZoneSystemUtils.createFromPreparedGeometries(network,
			H3GridUtils.createH3GridFromNetwork(network, resolution, crs), crs, resolution);

		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f8d697ffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f8d687ffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f8d69fffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f88a6fffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f88a6fffffff"))).isTrue();
		assertThat(drtZonalSystem.getZones().containsKey(createZoneId("861f89d37ffffff"))).isTrue();

		// center of Holzkirchen
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(358598)).getId()).isEqualTo(createZoneId("861f8d697ffffff"));
		// Thanning (Western border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(78976)).getId()).isEqualTo(createZoneId("861f88b47ffffff"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(59914)).getId()).isEqualTo(createZoneId("861f89d07ffffff"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(drtZonalSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution10() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 10;
		ZoneSystem drtZonalSystem = H3ZoneSystemUtils.createFromPreparedGeometries(network,
			H3GridUtils.createH3GridFromNetwork(network, resolution, crs), crs, resolution);

		// center of Holzkirchen
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(358598)).getId()).isEqualTo(createZoneId("8a1f8d6930b7fff"));
		// Thanning (Western border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(78976)).getId()).isEqualTo(createZoneId("8a1f88b4025ffff"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(59914)).getId()).isEqualTo(createZoneId("8a1f89d06d5ffff"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(drtZonalSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution12() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 12;
		ZoneSystem drtZonalSystem = H3ZoneSystemUtils.createFromPreparedGeometries(network,
			H3GridUtils.createH3GridFromNetwork(network, resolution, crs), crs, resolution);

		// center of Holzkirchen
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(358598)).getId()).isEqualTo(createZoneId("8c1f8d6930b63ff"));
		// Thanning (Western border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(78976)).getId()).isEqualTo(createZoneId("8c1f88b4025d1ff"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId(59914)).getId()).isEqualTo(createZoneId("8c1f89d06d581ff"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(drtZonalSystem.getZoneForLinkId(link.getId()));
		}
	}


	static Network getNetwork() {
		Network network = NetworkUtils.createNetwork();
		URL holzkirchen = ConfigGroup.getInputFileURL(ExamplesUtils.getTestScenarioURL("holzkirchen"), "holzkirchenNetwork.xml.gz");
		new MatsimNetworkReader(network).parse(holzkirchen);
		return network;
	}
}
