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

package org.matsim.contrib.common.zones.systems.h3;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
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
		ZoneSystem zoneSystem = new H3ZoneSystem(crs, resolution, network, z -> true);

		assertThat(zoneSystem.getZones().containsKey(createZoneId("590526392240701439"))).isTrue();

		// center of Holzkirchen
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).orElseThrow().getId()).isEqualTo(createZoneId("590526667118608383"));
		// Thanning (Western border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).orElseThrow().getId()).isEqualTo(createZoneId("590526667118608383"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).orElseThrow().getId()).isEqualTo(createZoneId("590526392240701439"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution5() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 5;

		ZoneSystem zoneSystem = new H3ZoneSystem(crs, resolution, network, z -> true);

		assertThat(zoneSystem.getZones().containsKey(createZoneId("599533579684282367"))).isTrue();
		assertThat(zoneSystem.getZones().containsKey(createZoneId("599533826644901887"))).isTrue();
		assertThat(zoneSystem.getZones().containsKey(createZoneId("599533499153645567"))).isTrue();
		assertThat(zoneSystem.getZones().containsKey(createZoneId("599533503448612863"))).isTrue();

		// center of Holzkirchen
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).orElseThrow().getId()).isEqualTo(createZoneId("599533826644901887"));
		// Thanning (Western border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).orElseThrow().getId()).isEqualTo(createZoneId("599533503448612863"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).orElseThrow().getId()).isEqualTo(createZoneId("599533579684282367"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution6() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 6;

		ZoneSystem zoneSystem = new H3ZoneSystem(crs, resolution, network, z -> true);

		// center of Holzkirchen
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).orElseThrow().getId()).isEqualTo(createZoneId("604037425601183743"));
		// Thanning (Western border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).orElseThrow().getId()).isEqualTo(createZoneId("604037102136459263"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).orElseThrow().getId()).isEqualTo(createZoneId("604037178372128767"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution10() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 10;
		ZoneSystem zoneSystem = new H3ZoneSystem(crs, resolution, network, z -> true);

		// center of Holzkirchen
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).orElseThrow().getId()).isEqualTo(createZoneId("622051824027533311"));
		// Thanning (Western border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).orElseThrow().getId()).isEqualTo(createZoneId("622051500514213887"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).orElseThrow().getId()).isEqualTo(createZoneId("622051576862081023"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()));
		}
	}

	@Test
	void test_Holzkirchen_Resolution12() {
		Network network = getNetwork();
		String crs = TransformationFactory.DHDN_GK4;
		int resolution = 12;

		ZoneSystem zoneSystem = new H3ZoneSystem(crs, resolution, network, z -> true);


		// center of Holzkirchen
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).orElseThrow().getId()).isEqualTo(createZoneId("631059023282267135"));
		// Thanning (Western border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).orElseThrow().getId()).isEqualTo(createZoneId("631058699768943103"));
		// between Gross- and Kleinpienzenau (Southeastern border of network)
		assertThat(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).orElseThrow().getId()).isEqualTo(createZoneId("631058776116789759"));

		//check all links are mapped
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()));
		}
	}


	static Network getNetwork() {
		Network network = NetworkUtils.createNetwork();
		URL holzkirchen = ConfigGroup.getInputFileURL(ExamplesUtils.getTestScenarioURL("holzkirchen"), "holzkirchenNetwork.xml.gz");
		new MatsimNetworkReader(network).parse(holzkirchen);
		return network;
	}
}
