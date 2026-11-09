/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.contrib.drt.run.DrtPickupDropoffRestrictionParams.RequiredInArea;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class DrtPickupDropoffRestrictionParamsTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void testRestrictionIsOptional() {
		assertThat(new DrtConfigGroup().getPickupDropoffRestrictionParams()).isEmpty();
	}

	@Test
	void testDefaultIsPickupOrDropoff() {
		assertThat(new DrtPickupDropoffRestrictionParams().getRequiredInArea()).isEqualTo(RequiredInArea.pickupOrDropoff);
	}

	@Test
	void testWriteAndRead() {
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		DrtPickupDropoffRestrictionParams params = new DrtPickupDropoffRestrictionParams();
		params.setAreaFile("area.shp");
		params.setRequiredInArea(RequiredInArea.dropoff);
		drtConfig.addParameterSet(params);

		Config config = ConfigUtils.createConfig();
		ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class).addParameterSet(drtConfig);
		Path configFile = temporaryDirectory.resolve("config.xml");
		ConfigUtils.writeConfig(config, configFile.toString());

		Config reloadedConfig = ConfigUtils.loadConfig(configFile.toString(), new MultiModeDrtConfigGroup());
		DrtPickupDropoffRestrictionParams reloaded = MultiModeDrtConfigGroup.get(reloadedConfig)
				.getModalElements()
				.iterator()
				.next()
				.getPickupDropoffRestrictionParams()
				.orElseThrow();
		assertThat(reloaded.getAreaFile()).isEqualTo("area.shp");
		assertThat(reloaded.getRequiredInArea()).isEqualTo(RequiredInArea.dropoff);
	}
}
