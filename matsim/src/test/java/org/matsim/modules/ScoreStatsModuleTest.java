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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Inject;

public class ScoreStatsModuleTest {

    public static final double DELTA = 0.0000000001;
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
    
    @Test
    public void testScoreStats() {
        Config config = utils.loadConfig("test/scenarios/equil/config.xml");
        config.controler().setLastIteration(1);
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
        
        @Inject PlansCalcRouteConfigGroup plansCalcRouteConfigGroup ;

        @Override
        public void notifyShutdown(ShutdownEvent event) {
            double[][] result = scoreStats.getHistory();

            for (double[] vector : result) {
                for(double value : vector) {
                    System.out.print(value + " ");
                }
                System.out.println() ;
            }

            if ( !plansCalcRouteConfigGroup.isInsertingAccessEgressWalk() ) {
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 53.2163372155953}, result[ScoreStatsControlerListener.INDEX_WORST], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 53.27444944602408}, result[ScoreStatsControlerListener.INDEX_BEST], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 53.245393330809684}, result[ScoreStatsControlerListener.INDEX_AVERAGE], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 53.27444944602408}, result[ScoreStatsControlerListener.INDEX_EXECUTED], DELTA);
            } else {
            	// yyyy these change with the access/egress car router, but I cannot say if the magnitude of change is plausible. kai, feb'16
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 38.73119275042525}, result[ScoreStatsControlerListener.INDEX_WORST], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 53.2163372155953}, result[ScoreStatsControlerListener.INDEX_BEST], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 45.97376498301028}, result[ScoreStatsControlerListener.INDEX_AVERAGE], DELTA);
            	Assert.assertArrayEquals(new double[]{53.18953957492432, 38.73119275042525}, result[ScoreStatsControlerListener.INDEX_EXECUTED], DELTA);
            }
        }
    }

}
