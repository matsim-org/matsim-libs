/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSQSimModule extends AbstractQSimModule {

    static final public String COMPONENT_NAME = "FISS";

    @Override
    protected void configureQSim() {
	    addQSimComponentBinding( COMPONENT_NAME ).to( FISS.class );
    }

    @Provides
    @Singleton
    FISS provideMultiModalDepartureHandler(MatsimServices matsimServices, QNetsimEngineI qNetsimEngine,
										   QSimConfigGroup qsimConfig, Scenario scenario, EventsManager eventsManager,
										   @Named(TransportMode.car) TravelTime travelTime) {
        Config config = scenario.getConfig();
        FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
		return new FISS(matsimServices, qNetsimEngine, scenario, eventsManager, fissConfigGroup, travelTime);
    }
}
