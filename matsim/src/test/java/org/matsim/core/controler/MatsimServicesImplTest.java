
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimServicesImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

 package org.matsim.core.controler;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.testcases.MatsimTestUtils;

public class MatsimServicesImplTest {
	
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
	
    @Ignore
    @Test
    public void testIterationInServicesEqualsIterationInEvent() {
   	
    	Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		Controler controler = new Controler( config );
		
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addControlerListenerBinding().toInstance(new IterationStartsListener() {

					@Override
					public void notifyIterationStarts(IterationStartsEvent event) {
						Assert.assertSame(event.getIteration(), event.getServices().getIterationNumber());
						
					}
                	
                });
            }
        });
		
		controler.run();
    	
    }

}
