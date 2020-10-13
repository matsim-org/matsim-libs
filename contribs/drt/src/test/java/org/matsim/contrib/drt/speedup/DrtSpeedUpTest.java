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

package org.matsim.contrib.drt.speedup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.speedup.DrtSpeedUp.computeMovingAverage;
import static org.matsim.contrib.drt.speedup.DrtSpeedUp.isTeleportDrtUsers;

import java.util.List;

import org.junit.Test;
import org.matsim.core.config.groups.ControlerConfigGroup;

/**
 * @author ikaddoura
 * @author michalm (Michal Maciejewski)
 */
public class DrtSpeedUpTest {
	@Test
	public final void test_computeMovingAverage() {
		List<Double> list = List.of(2., 5., 22.);
		assertThat(computeMovingAverage(2, list)).isEqualTo(27. / 2);
		assertThat(computeMovingAverage(3, list)).isEqualTo(29. / 3);
		assertThat(computeMovingAverage(4, list)).isEqualTo(29. / 3);
	}

	@Test
	public void test_isTeleportDrtUsers() {
		DrtSpeedUpParams drtSpeedUpParams = new DrtSpeedUpParams();
		drtSpeedUpParams.setFractionOfIterationsSwitchOn(0.1);
		drtSpeedUpParams.setFractionOfIterationsSwitchOff(0.9);
		drtSpeedUpParams.setIntervalDetailedIteration(10);

		ControlerConfigGroup controlerConfig = new ControlerConfigGroup();
		controlerConfig.setLastIteration(100);

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 0)).isFalse();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 9)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 10)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 11)).isTrue();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 49)).isTrue();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 50)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 51)).isTrue();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 89)).isTrue();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 90)).isFalse();
		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 91)).isFalse();

		assertThat(isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, 100)).isFalse();
	}
}
