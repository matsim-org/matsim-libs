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

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;

import com.google.common.base.VerifyException;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceConfigurationsParamsTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void testServiceConfigurationsAreOptional() {
		DrtConfigGroup drtConfig = drtConfig();
		assertThat(drtConfig.getServiceConfigurationsParams()).isEmpty();
		assertThatCode(() -> drtConfig.checkConsistency(ConfigUtils.createConfig())).doesNotThrowAnyException();
	}

	@Test
	void testWriteAndReadTwoServiceConfigurations() {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		params.addParameterSet(serviceConfiguration("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		params.addParameterSet(serviceConfiguration("offpeak", at(10 * 3600), at(20 * 3600), null));
		drtConfig.addParameterSet(params);

		DrtConfigGroup reloaded = writeAndRead(drtConfig);

		List<DrtServiceConfigurationParams> serviceConfigurations = reloaded.getServiceConfigurationsParams()
				.orElseThrow()
				.getServiceConfigurations();
		assertThat(serviceConfigurations).hasSize(2);
		assertThat(serviceConfigurations.get(0).getServiceConfigurationName()).isEqualTo("peak");
		assertThat(serviceConfigurations.get(0).getStartTime()).isEqualTo(OptionalTime.defined(6 * 3600));
		assertThat(serviceConfigurations.get(0).getEndTime()).isEqualTo(OptionalTime.defined(10 * 3600));
		assertThat(serviceConfigurations.get(0).getStopNetwork()).isEqualTo("peakStops");
		assertThat(serviceConfigurations.get(1).getServiceConfigurationName()).isEqualTo("offpeak");
		assertThat(serviceConfigurations.get(1).getStartTime()).isEqualTo(OptionalTime.defined(10 * 3600));
		assertThat(serviceConfigurations.get(1).getEndTime()).isEqualTo(OptionalTime.defined(20 * 3600));
		assertThat(serviceConfigurations.get(1).getStopNetwork()).isNull();
	}

	@Test
	void testWriteAndReadUndefinedTimes() {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		params.addParameterSet(serviceConfiguration("allDay", undefined(), undefined(), null));
		drtConfig.addParameterSet(params);

		DrtServiceConfigurationParams reloaded = writeAndRead(drtConfig).getServiceConfigurationsParams()
				.orElseThrow()
				.getServiceConfigurations()
				.get(0);

		assertThat(reloaded.getStartTime().isUndefined()).isTrue();
		assertThat(reloaded.getEndTime().isUndefined()).isTrue();
	}

	@Test
	void testDuplicateNamesAreRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceConfiguration("service", at(0), at(10 * 3600), null),
								serviceConfiguration("service", at(10 * 3600), at(20 * 3600), null)))
				.withMessageContaining("identical names");
	}

	@Test
	void testOverlappingWindowsAreRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceConfiguration("morning", at(6 * 3600), at(12 * 3600), null),
								serviceConfiguration("evening", at(11 * 3600), at(20 * 3600), null)))
				.withMessageContaining("overlap");
	}

	@Test
	void testEndTimeBeforeStartTimeIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceConfiguration("backwards", at(12 * 3600), at(6 * 3600), null)))
				.withMessageContaining("endTime must be later than startTime");
	}

	@Test
	void testEmptyContainerIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(this::checkConsistency)
				.withMessageContaining("At least one");
	}

	@Test
	void testAdjacentWindowsAreAccepted() {
		assertThatCode(() -> checkConsistency(serviceConfiguration("morning", at(6 * 3600), at(12 * 3600), null),
				serviceConfiguration("evening", at(12 * 3600), at(20 * 3600), null))).doesNotThrowAnyException();
	}

	@Test
	void testAtMostOneOpenStartAndOneOpenEnd() {
		assertThatCode(() -> checkConsistency(serviceConfiguration("early", undefined(), at(12 * 3600), null),
				serviceConfiguration("late", at(12 * 3600), undefined(), null))).doesNotThrowAnyException();

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceConfiguration("late", at(12 * 3600), undefined(), null),
								serviceConfiguration("alsoLate", at(20 * 3600), undefined(), null)))
				.withMessageContaining("undefined endTime");

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> checkConsistency(serviceConfiguration("early", undefined(), at(6 * 3600), null),
								serviceConfiguration("alsoEarly", undefined(), at(12 * 3600), null)))
				.withMessageContaining("undefined startTime");
	}

	@Test
	void testStopNetworkIsRejectedForDoor2Door() {
		DrtConfigGroup drtConfig = drtConfig();
		drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		params.addParameterSet(serviceConfiguration("peak", at(6 * 3600), at(10 * 3600), "peakStops"));
		drtConfig.addParameterSet(params);

		assertThatExceptionOfType(VerifyException.class).isThrownBy(
						() -> drtConfig.checkConsistency(ConfigUtils.createConfig()))
				.withMessageContaining("stopNetwork");
	}

	@Test
	void testExistingConfigsRemainReadable() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup());

		DrtConfigGroup drtConfig = MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next();
		assertThat(drtConfig.getServiceConfigurationsParams()).isEmpty();
	}

	private void checkConsistency(DrtServiceConfigurationParams... serviceConfigurations) {
		DrtConfigGroup drtConfig = drtConfig();
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		for (DrtServiceConfigurationParams serviceConfiguration : serviceConfigurations) {
			params.addParameterSet(serviceConfiguration);
		}
		drtConfig.addParameterSet(params);
		drtConfig.checkConsistency(ConfigUtils.createConfig());
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
	 * failures caused by the service configurations
	 */
	private static DrtConfigGroup drtConfig() {
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setStopDuration(60);
		drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.setMaxWaitTime(600);
		return drtConfig;
	}

	private static DrtServiceConfigurationParams serviceConfiguration(String name, OptionalTime startTime,
			OptionalTime endTime, String stopNetwork) {
		DrtServiceConfigurationParams serviceConfiguration = new DrtServiceConfigurationParams(name);
		serviceConfiguration.setStartTime(startTime);
		serviceConfiguration.setEndTime(endTime);
		serviceConfiguration.setStopNetwork(stopNetwork);
		return serviceConfiguration;
	}

	private static OptionalTime at(double time) {
		return OptionalTime.defined(time);
	}

	private static OptionalTime undefined() {
		return OptionalTime.undefined();
	}
}
