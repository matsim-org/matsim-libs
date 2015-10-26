/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MobsimListenerTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.core.controler;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.misc.MatsimTestUtils;

import javax.inject.Singleton;

public class MobsimListenerTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testRunMobsim_listenerTransient() {
        Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
        cfg.controler().setLastIteration(1);
        cfg.controler().setWritePlansInterval(0);
        final Controler c = new Controler(cfg);
        c.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addMobsimListenerBinding().to(CountingMobsimListener.class);
            }
        });
        c.getConfig().controler().setCreateGraphs(false);
        c.setDumpDataAtEnd(false);
        c.getConfig().controler().setWriteEventsInterval(0);
        c.run();
    }

    @Test(expected = RuntimeException.class)
    public void testRunMobsim_listenerSingleton() {
        Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
        cfg.controler().setLastIteration(1);
        cfg.controler().setWritePlansInterval(0);
        final Controler c = new Controler(cfg);
        c.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addMobsimListenerBinding().to(SingletonCountingMobsimListener.class);
            }
        });
        c.getConfig().controler().setCreateGraphs(false);
        c.setDumpDataAtEnd(false);
        c.getConfig().controler().setWriteEventsInterval(0);
        c.run();
    }

    private static class CountingMobsimListener implements MobsimInitializedListener {

        int count = 0;

        @Override
        public void notifyMobsimInitialized(MobsimInitializedEvent e) {
            count++;
            if (count > 1) {
                throw new RuntimeException("This mobsim listener ran more than once.");
            }
        }

    }

    @Singleton
    private static class SingletonCountingMobsimListener implements MobsimInitializedListener {

        int count = 0;

        @Override
        public void notifyMobsimInitialized(MobsimInitializedEvent e) {
            count++;
            if (count > 1) {
                throw new RuntimeException("This mobsim listener ran more than once.");
            }
        }

    }

}
