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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MobsimListenerTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunMobsim_listenerTransient() {
        Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
        cfg.controller().setLastIteration(1);
        cfg.controller().setWritePlansInterval(0);
        final Controler c = new Controler(cfg);
        c.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addMobsimListenerBinding().to(CountingMobsimListener.class);
            }
        });
        c.getConfig().controller().setCreateGraphs(false);
        c.getConfig().controller().setDumpDataAtEnd(false);
        c.getConfig().controller().setWriteEventsInterval(0);
        c.run();
    }

	@Test
	void testRunMobsim_listenerSingleton() {
		assertThrows(RuntimeException.class, () -> {
			Config cfg = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
			cfg.controller().setLastIteration(1);
			cfg.controller().setWritePlansInterval(0);
			final Controler c = new Controler(cfg);
			c.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addMobsimListenerBinding().to(SingletonCountingMobsimListener.class);
				}
			});
			c.getConfig().controller().setCreateGraphs(false);
			c.getConfig().controller().setDumpDataAtEnd(false);
			c.getConfig().controller().setWriteEventsInterval(0);
			c.run();
		});
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
