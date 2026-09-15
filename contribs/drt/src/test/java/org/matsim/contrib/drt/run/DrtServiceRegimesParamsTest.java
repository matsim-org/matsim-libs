/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.matsim.contrib.drt.run.DrtServiceRegimesFixtures.serviceRegime;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;

import com.google.common.base.VerifyException;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceRegimesParamsTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void testServiceRegimesAreOptional() {
		DrtConfigGroup drtConfig = drtConfig();
		assertThat(drtConfig.getServiceRegimesParams()).isEmpty();
		assertThatCode(() -> drtConfig.checkConsistency(ConfigUtils.createConfig())).doesNotThrowAnyException();
	}

	@Test
	void testWriteAndReadTwoServiceRegimes() {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		params.addParameterSet(serviceRegime("offpeak", at(10 * 3600), at(20 * 3600), null));
		drtConfig.addParameterSet(params);

		DrtConfigGroup reloaded = writeAndRead(drtConfig);

		List<DrtServiceRegimeParams> serviceRegimes = reloaded.getServiceRegimesParams()
				.orElseThrow()
				.getServiceRegimes();
		assertThat(serviceRegimes).hasSize(2);
		assertThat(serviceRegimes.get(0).getServiceRegimeName()).isEqualTo("peak");
		assertThat(serviceRegimes.get(0).getStartTime()).isEqualTo(OptionalTime.defined(6 * 3600));
		assertThat(serviceRegimes.get(0).getEndTime()).isEqualTo(OptionalTime.defined(10 * 3600));
		assertThat(serviceRegimes.get(0).getStopNetwork()).isEqualTo("peakStops");
		assertThat(serviceRegimes.get(1).getServiceRegimeName()).isEqualTo("offpeak");
		assertThat(serviceRegimes.get(1).getStartTime()).isEqualTo(OptionalTime.defined(10 * 3600));
		assertThat(serviceRegimes.get(1).getEndTime()).isEqualTo(OptionalTime.defined(20 * 3600));
		assertThat(serviceRegimes.get(1).getStopNetwork()).isNull();
	}

	@Test
	void testWriteAndReadUndefinedTimes() {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("allDay", undefined(), undefined(), null));
		drtConfig.addParameterSet(params);

		DrtServiceRegimeParams reloaded = writeAndRead(drtConfig).getServiceRegimesParams()
				.orElseThrow()
				.getServiceRegimes()
				.get(0);

		assertThat(reloaded.getStartTime().isUndefined()).isTrue();
		assertThat(reloaded.getEndTime().isUndefined()).isTrue();
	}

	@Test
	void testDuplicateNamesAreRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceRegime("service", at(0), at(10 * 3600), null),
								serviceRegime("service", at(10 * 3600), at(20 * 3600), null)))
				.withMessageContaining("identical names");
	}

	@Test
	void testOverlappingWindowsAreRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceRegime("morning", at(6 * 3600), at(12 * 3600), null),
								serviceRegime("evening", at(11 * 3600), at(20 * 3600), null)))
				.withMessageContaining("overlap");
	}

	@Test
	void testEndTimeBeforeStartTimeIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceRegime("backwards", at(12 * 3600), at(6 * 3600), null)))
				.withMessageContaining("endTime must be later than startTime");
	}

	@Test
	void testEmptyContainerIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(this::checkConsistency)
				.withMessageContaining("At least one");
	}

	@Test
	void testAdjacentWindowsAreAccepted() {
		assertThatCode(() -> checkConsistency(serviceRegime("morning", at(6 * 3600), at(12 * 3600), null),
				serviceRegime("evening", at(12 * 3600), at(20 * 3600), null))).doesNotThrowAnyException();
	}

	@Test
	void testAtMostOneOpenStartAndOneOpenEnd() {
		assertThatCode(() -> checkConsistency(serviceRegime("early", undefined(), at(12 * 3600), null),
				serviceRegime("late", at(12 * 3600), undefined(), null))).doesNotThrowAnyException();

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceRegime("late", at(12 * 3600), undefined(), null),
								serviceRegime("alsoLate", at(20 * 3600), undefined(), null)))
				.withMessageContaining("undefined endTime");

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceRegime("early", undefined(), at(6 * 3600), null),
								serviceRegime("alsoEarly", undefined(), at(12 * 3600), null)))
				.withMessageContaining("undefined startTime");
	}

	@Test
	void testStopNetworkIsRejectedForDoor2Door() {
		DrtConfigGroup drtConfig = drtConfig();
		drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		drtConfig.addParameterSet(params);

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> drtConfig.checkConsistency(ConfigUtils.createConfig()))
				.withMessageContaining("stopNetwork");
	}

	@Test
	void testWriteAndReadServiceAreaFile() {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		params.setServiceAreaFile("serviceAreas.shp");
		params.setServiceAreaAttribute("networks");
		drtConfig.addParameterSet(params);

		DrtServiceRegimesParams reloaded = writeAndRead(drtConfig).getServiceRegimesParams()
				.orElseThrow();

		assertThat(reloaded.getServiceAreaFile()).isEqualTo("serviceAreas.shp");
		assertThat(reloaded.getServiceAreaAttribute()).isEqualTo("networks");
	}

	@Test
	void testServiceAreaFileIsOptionalAndTheAttributeDefaultsToTheStopAttribute() {
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();

		assertThat(params.getServiceAreaFile()).isNull();
		assertThat(params.getServiceAreaAttribute()).isEqualTo(
				AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE);
	}

	@Test
	void testServiceAreaFileReplacesDrtServiceAreaShapeFile() {
		assertThatCode(() -> serviceAreaBasedConfig(null, "serviceAreas.shp").checkConsistency(
				ConfigUtils.createConfig())).doesNotThrowAnyException();
		assertThatCode(() -> serviceAreaBasedConfig("serviceArea.shp", null).checkConsistency(
				ConfigUtils.createConfig())).doesNotThrowAnyException();
	}

	@Test
	void testServiceAreaBasedWithoutAnyAreaIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> serviceAreaBasedConfig(null, null).checkConsistency(ConfigUtils.createConfig()))
				.withMessageContaining(DrtServiceRegimesParams.SERVICE_AREA_FILE);
	}

	@Test
	void testBothAreaSourcesAtOnceAreRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> serviceAreaBasedConfig("serviceArea.shp", "serviceAreas.shp").checkConsistency(
								ConfigUtils.createConfig()))
				.withMessageContaining("must not be set at the same time");
	}

	@Test
	void testServiceAreaFileIsRejectedForDoor2Door() {
		DrtConfigGroup drtConfig = drtConfig();
		drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("peak", at(6 * 3600), at(10 * 3600), null));
		params.setServiceAreaFile("serviceAreas.shp");
		drtConfig.addParameterSet(params);

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> drtConfig.checkConsistency(ConfigUtils.createConfig()))
				.withMessageContaining(DrtServiceRegimesParams.SERVICE_AREA_FILE);
	}

	@Test
	void testExistingConfigsRemainReadable() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup());

		DrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();
		assertThat(drtConfig.getServiceRegimesParams()).isEmpty();
	}

	private void checkConsistency(DrtServiceRegimeParams... serviceRegimes) {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		for (DrtServiceRegimeParams serviceRegime : serviceRegimes) {
			params.addParameterSet(serviceRegime);
		}
		drtConfig.addParameterSet(params);
		drtConfig.checkConsistency(ConfigUtils.createConfig());
	}

	/**
	 * @return a {@code serviceAreaBased} DRT config with one service regime and the given area sources, of
	 * which exactly one is expected to be set
	 */
	private static DrtConfigGroup serviceAreaBasedConfig(String drtServiceAreaShapeFile, String serviceAreaFile) {
		DrtConfigGroup drtConfig = drtConfig();
		drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.serviceAreaBased);
		drtConfig.setDrtServiceAreaShapeFile(drtServiceAreaShapeFile);
		DrtServiceRegimesParams params = new DrtServiceRegimesParams();
		params.addParameterSet(serviceRegime("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		params.setServiceAreaFile(serviceAreaFile);
		drtConfig.addParameterSet(params);
		return drtConfig;
	}

	private DrtConfigGroup writeAndRead(DrtConfigGroup drtConfig) {
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config,
				MultiModeDrtConfigGroup.class);
		multiModeDrtConfigGroup.addParameterSet(drtConfig);

		Path configFile = temporaryDirectory.resolve("config.xml");
		ConfigUtils.writeConfig(config, configFile.toString());

		Config reloadedConfig = ConfigUtils.loadConfig(configFile.toString(), new MultiModeDrtConfigGroup());
		return MultiModeDrtConfigGroup.get(reloadedConfig).getModalElements().iterator().next();
	}

	/**
	 * @return a DRT config which passes {@link DrtConfigGroup#checkConsistency} on its own, so that the tests only see
	 * failures caused by the service regimes
	 */
	private static DrtConfigGroup drtConfig() {
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setStopDuration(60);
		drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.setMaxWaitTime(600);
		return drtConfig;
	}

	private static OptionalTime at(double time) {
		return OptionalTime.defined(time);
	}

	private static OptionalTime undefined() {
		return OptionalTime.undefined();
	}
}
