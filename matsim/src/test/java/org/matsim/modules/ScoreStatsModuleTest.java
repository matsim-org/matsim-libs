/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ScoreStatsModuleTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.modules;

import java.util.Map;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.testcases.MatsimTestUtils;

public class ScoreStatsModuleTest {

	public static final double DELTA = 0.0000000001;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	public static Stream<Arguments> arguments () {
		return Stream.of(Arguments.of(false, false), Arguments.of(true, false));
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testScoreStats(boolean isUsingFastCapacityUpdate, boolean isInsertingAccessEgressWalk) {
		Config config = utils.loadConfig("test/scenarios/equil/config.xml");

		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
		config.routing().setAccessEgressType(isInsertingAccessEgressWalk? AccessEgressType.accessEgressModeToLink : AccessEgressType.none);

		config.controller().setLastIteration(1);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().to(MyControlerListener.class);
			}
		});
		controler.run();
	}

	private static class MyControlerListener implements ShutdownListener {

		@Inject
		ScoreStats scoreStats;

		@Inject Config config ;

		@Override
		public void notifyShutdown(ShutdownEvent event) {
			Map<ScoreItem, Map<Integer, Double>> result = scoreStats.getScoreHistory();

			//            for (double[] vector : result) {
			//                for(double value : vector) {
			//                    System.out.print(value + " ");
			//                }
			//                System.out.println() ;
			//            }

			// yy the following is retrofitted from an older double[][] data structure and thus messy.  Please improve it.  kai, nov'16
			if ( config.routing().getAccessEgressType().equals(AccessEgressType.none) ) {
				{
					Double[] array = result.get(ScoreItem.worst).values().toArray(new Double[0]) ;
					Assertions.assertEquals(64.75686659291274, array[0], DELTA);
					Assertions.assertEquals(64.78366379257605, array[1], DELTA);
				} {
					Double[] array = result.get(ScoreItem.best).values().toArray(new Double[0]) ;
					Assertions.assertEquals(64.75686659291274, array[0], DELTA);
					Assertions.assertEquals(64.84180132563583, array[1], DELTA);
				}{
					Double[] array = result.get(ScoreItem.average).values().toArray(new Double[0]) ;
					Assertions.assertEquals(64.75686659291274, array[0], DELTA);
					Assertions.assertEquals(64.81273255910591, array[1], DELTA);
				}{
					Double[] array = result.get(ScoreItem.executed).values().toArray(new Double[0]) ;
					Assertions.assertEquals(64.75686659291274, array[0], DELTA);
					Assertions.assertEquals(64.84180132563583, array[1], DELTA);
				}
				} else {
					// yyyy these change with the access/egress car router, but I cannot say if the magnitude of change is plausible. kai, feb'16
//					if(config.qsim().isUsingFastCapacityUpdate()) {
						{
						Double[] array = result.get(ScoreItem.worst).values().toArray(new Double[0]) ;
						Assertions.assertEquals(new double[]{53.18953957492432, 38.73201822323088}[0], array[0], DELTA);
						Assertions.assertEquals(new double[]{53.18953957492432, 38.73201822323088}[1], array[1], DELTA);
						}{
						Double[] array = result.get(ScoreItem.best).values().toArray(new Double[0]) ;
						Assertions.assertEquals(new double[]{53.18953957492432, 53.2163372155953}[0], array[0], DELTA);
						Assertions.assertEquals(new double[]{53.18953957492432, 53.2163372155953}[1], array[1], DELTA);
						}{
						Double[] array = result.get(ScoreItem.average).values().toArray(new Double[0]) ;
						Assertions.assertEquals(new double[]{53.18953957492432, 45.9741777194131}[0], array[0], DELTA);
						Assertions.assertEquals(new double[]{53.18953957492432, 45.9741777194131}[1], array[1], DELTA);
						}{
						Double[] array = result.get(ScoreItem.executed).values().toArray(new Double[0]) ;
						Assertions.assertEquals(new double[]{53.18953957492432, 38.73201822323088}[0], array[0], DELTA);
						Assertions.assertEquals(new double[]{53.18953957492432, 38.73201822323088}[1], array[1], DELTA);
						}
//					} else {
//						{
//						Double[] array = result.get(ScoreItem.worst).values().toArray(new Double[0]) ;
//						Assert.assertEquals(new double[]{53.18953957492432, 38.73119275042525}[0], array[0], DELTA);
//						Assert.assertEquals(new double[]{53.18953957492432, 38.73119275042525}[1], array[1], DELTA);
//						}{
//						Double[] array = result.get(ScoreItem.best).values().toArray(new Double[0]) ;
//						Assert.assertEquals(new double[]{53.18953957492432, 53.2163372155953}[0], array[0], DELTA);
//						Assert.assertEquals(new double[]{53.18953957492432, 53.2163372155953}[1], array[1], DELTA);
//						}{
//						Double[] array = result.get(ScoreItem.average).values().toArray(new Double[0]) ;
//						Assert.assertEquals(new double[]{53.18953957492432, 45.97376498301028}[0], array[0], DELTA);
//						Assert.assertEquals(new double[]{53.18953957492432, 45.97376498301028}[1], array[1], DELTA);
//						}{
//						Double[] array = result.get(ScoreItem.executed).values().toArray(new Double[0]) ;
//						Assert.assertEquals(new double[]{53.18953957492432, 38.73119275042525}[0], array[0], DELTA);
//						Assert.assertEquals(new double[]{53.18953957492432, 38.73119275042525}[1], array[1], DELTA);
//						}
//					}
				}
			}
		}

	}
